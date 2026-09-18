package cn.ialley.unihalo.finders.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.theme.finders.Finder;

import cn.ialley.unihalo.captcha.CaptchaScope;
import cn.ialley.unihalo.captcha.CaptchaService;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.finders.UniHaloFinder;
import cn.ialley.unihalo.scheme.FeatureConfig;
import cn.ialley.unihalo.scheme.LoveAlbum;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.services.LoveAlbumService;
import cn.ialley.unihalo.services.LoveDailyItemService;
import cn.ialley.unihalo.services.LoveStoryService;
import cn.ialley.unihalo.utils.LoveDates;
import cn.ialley.unihalo.utils.LoveDiaryConfigResolver;
import cn.ialley.unihalo.utils.LoveRouteResolver;
import cn.ialley.unihalo.utils.LoveUnlockContext;
import cn.ialley.unihalo.vo.LoveAlbumVo;
import cn.ialley.unihalo.vo.LoveConfigVo;
import cn.ialley.unihalo.vo.LoveDailyItemVo;
import cn.ialley.unihalo.vo.LoveDiaryThemeConfig;
import cn.ialley.unihalo.vo.LoveModuleAccess;
import cn.ialley.unihalo.vo.LoveModuleVo;
import cn.ialley.unihalo.vo.LoveOverviewVo;
import cn.ialley.unihalo.vo.LovePageVo;
import cn.ialley.unihalo.vo.LoveRoutePlan;
import cn.ialley.unihalo.vo.LoveStoryVo;

/**
 * 恋爱日记 Finder 实现。
 *
 * 锁语义（与公开 API 一致且更严格）：模块锁与相册锁两层独立、token 不通用；
 * 模块锁定时对应列表方法不调用 service，直接返回空结果（连条数都不泄露）；
 * 锁状态读取失败一律按「已锁定」处理（fail-closed），不抛 5xx。
 *
 * 本层不消费解锁 token：SSR 锁定页只渲染表单，校验通过后由前端 JS 带 token
 * 调公开 API 取数据并原地替换 —— 即使删掉表单也看不到任何内容。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Finder("uniHaloFinder")
@RequiredArgsConstructor
public class UniHaloFinderImpl implements UniHaloFinder {

    /** 首页卡片展示顺序（与 app 端 love.vue 的 moduleKeys 一致） */
    private static final List<String> MODULE_ORDER = List.of(
            Constants.LOVE_MODULE_OUR_STORY,
            Constants.LOVE_MODULE_PHOTO,
            Constants.LOVE_MODULE_DAILY);

    /** 首页摘要条数 */
    private static final int OVERVIEW_SIZE = 3;

    /** 单页上限（防止模板传入超大 size 拖垮服务端） */
    private static final int MAX_PAGE_SIZE = 100;

    /** 内置彩色图标字体类名（站长未配置图标时按模块 key 回落） */
    private static final Map<String, String> DEFAULT_ICON_CLASS = Map.of(
            Constants.LOVE_MODULE_DIARY, "uhlove-icon-aixin",
            Constants.LOVE_MODULE_OUR_STORY, "uhlove-icon-gushi",
            Constants.LOVE_MODULE_PHOTO, "uhlove-icon-xiangce",
            Constants.LOVE_MODULE_DAILY, "uhlove-icon-liebiao");

    private final LoveStoryService loveStoryService;

    private final LoveAlbumService loveAlbumService;

    private final LoveDailyItemService loveDailyItemService;

    private final FeatureConfigService featureConfigService;

    private final CaptchaService captchaService;

    private final LoveDiaryConfigResolver configResolver;

    private final LoveRouteResolver routeResolver;

    // ------------------------------------------------------------------
    // 配置 / 访问状态
    // ------------------------------------------------------------------

    @Override
    public Mono<LoveConfigVo> loveConfig() {
        return context().map(ctx -> buildConfig(ctx));
    }

    @Override
    public Mono<LoveModuleAccess> loveModuleAccess(String module) {
        String key = normalizeModuleKey(module);
        return context().map(ctx -> {
            if (ctx.locked(key)) {
                return LoveModuleAccess.locked(key, moduleTitle(ctx, key), ctx.captchaRequired());
            }
            return LoveModuleAccess.unlocked(key);
        });
    }

    @Override
    public Mono<LovePageVo> loveHome() {
        return context().flatMap(ctx -> {
            LovePageVo page = new LovePageVo();
            page.setRoutes(ctx.plan().getPaths());
            if (ctx.locked(Constants.LOVE_MODULE_DIARY)) {
                // 恋爱日记入口本身加密：只给锁状态，config/overview 保持 null（零业务字段）
                page.setAccess(LoveModuleAccess.locked(Constants.LOVE_MODULE_DIARY,
                        moduleTitle(ctx, Constants.LOVE_MODULE_DIARY), ctx.captchaRequired()));
                return Mono.just(page);
            }
            page.setAccess(LoveModuleAccess.unlocked(Constants.LOVE_MODULE_DIARY));
            page.setConfig(buildConfig(ctx));
            return buildOverview(ctx, ctx.theme().isShowOverview())
                    .map(overview -> {
                        page.setOverview(overview);
                        return page;
                    });
        });
    }

    // ------------------------------------------------------------------
    // 列表
    // ------------------------------------------------------------------

    @Override
    public Mono<ListResult<LoveStoryVo>> listLoveStories(Integer page, Integer size) {
        int currentPage = page(page);
        int pageSize = size(size);
        return context().flatMap(ctx -> {
            if (ctx.locked(Constants.LOVE_MODULE_OUR_STORY)) {
                return Mono.just(empty(currentPage, pageSize));
            }
            return loveStoryService.listPublic(currentPage, pageSize)
                    .map(result -> new ListResult<>(result.getPage(), result.getSize(),
                            result.getTotal(),
                            result.getItems().stream().map(LoveStoryVo::from).toList()));
        });
    }

    @Override
    public Mono<ListResult<LoveAlbumVo>> listLoveAlbums(Integer page, Integer size) {
        int currentPage = page(page);
        int pageSize = size(size);
        return context().flatMap(ctx -> {
            if (ctx.locked(Constants.LOVE_MODULE_PHOTO)) {
                return Mono.just(empty(currentPage, pageSize));
            }
            return loveAlbumService.listPublic(currentPage, pageSize)
                    .map(result -> new ListResult<>(result.getPage(), result.getSize(),
                            result.getTotal(),
                            result.getItems().stream()
                                    .map(album -> LoveAlbumVo.from(album, albumLocked(album)))
                                    .toList()));
        });
    }

    @Override
    public Mono<ListResult<LoveDailyItemVo>> listLoveItems(String status, Integer page, Integer size) {
        int currentPage = page(page);
        int pageSize = size(size);
        String filter = status == null ? "" : status.trim();
        return context().flatMap(ctx -> {
            if (ctx.locked(Constants.LOVE_MODULE_DAILY)) {
                return Mono.just(empty(currentPage, pageSize));
            }
            return loveDailyItemService.listPublic(filter, currentPage, pageSize)
                    .map(result -> new ListResult<>(result.getPage(), result.getSize(),
                            result.getTotal(),
                            result.getItems().stream().map(LoveDailyItemVo::from).toList()));
        });
    }

    // ------------------------------------------------------------------
    // 内部：上下文与视图构建
    // ------------------------------------------------------------------

    /**
     * 一次页面渲染所需的全部只读上下文（路由计划 + 主题配置 + 功能设置 + 锁状态 + 验证码）。
     */
    private Mono<Ctx> context() {
        return Mono.zip(
                        routeResolver.resolvePlan(),
                        configResolver.resolve(),
                        featureConfigService.get()
                                .onErrorResume(e -> {
                                    log.warn("读取功能设置失败，恋爱页按空配置渲染：{}", e.getMessage());
                                    return Mono.just(new FeatureConfig());
                                }),
                        lockFlags(),
                        captchaService.requiredFor(CaptchaScope.LOVE_MODULE_UNLOCK)
                                .defaultIfEmpty(false)
                                .onErrorResume(e -> Mono.just(false)))
                .map(tuple -> new Ctx(tuple.getT1(), tuple.getT2(), tuple.getT3(),
                        tuple.getT4(), tuple.getT5()));
    }

    /**
     * 四个模块的锁状态。任一读取失败 → 该模块按「已锁定」处理（fail-closed）。
     *
     * 最后叠加 {@link LoveUnlockContext} 的覆盖表：带凭证的那次文档请求里，
     * 已解锁模块直接按「未锁定」参与后续构建 —— 于是同一个 Finder 既能渲染
     * 锁定页（无凭证）也能渲染解锁后的内容（带凭证），未改任何签名。
     */
    private Mono<Map<String, Boolean>> lockFlags() {
        List<String> modules = List.of(Constants.LOVE_MODULE_DIARY, Constants.LOVE_MODULE_OUR_STORY,
                Constants.LOVE_MODULE_PHOTO, Constants.LOVE_MODULE_DAILY);
        return Flux.fromIterable(modules)
                .concatMap(module -> lockFlag(module)
                        .map(locked -> Map.entry(module, locked)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new)
                .flatMap(flags -> LoveUnlockContext.overrides().map(overrides -> {
                    if (!overrides.isEmpty()) {
                        flags.putAll(overrides);
                    }
                    return flags;
                }));
    }

    private Mono<Boolean> lockFlag(String module) {
        return featureConfigService.isLoveModuleLocked(module)
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.warn("读取恋爱模块锁状态失败，按已锁定处理（fail-closed）：module={}, err={}",
                            module, e.getMessage());
                    return Mono.just(true);
                });
    }

    private LoveConfigVo buildConfig(Ctx ctx) {
        FeatureConfig.Love love = loveOf(ctx.general());
        LoveConfigVo vo = new LoveConfigVo();
        vo.setEnabled(love != null);
        if (love != null && love.getLoveInfo() != null) {
            FeatureConfig.LoveInfo info = love.getLoveInfo();
            vo.setLoveDateTitle(info.getLoveDateTitle());
            vo.setLoveDate(LoveDates.isoDate(info.getLoveDate()));
            vo.setLoveDays(LoveDates.daysSince(info.getLoveDate()));
            vo.setBoyNickname(info.getBoyNickname());
            vo.setBoyAvatar(info.getBoyAvatar());
            vo.setGirlNickname(info.getGirlNickname());
            vo.setGirlAvatar(info.getGirlAvatar());
        }
        vo.setBgImageUrl(loveBgImage(ctx.general()));
        vo.setModules(buildModules(ctx, love));
        return vo;
    }

    /**
     * 模块入口列表：只保留 enabled=true 且路由已注册的模块，按 priority 降序。
     */
    private List<LoveModuleVo> buildModules(Ctx ctx, FeatureConfig.Love love) {
        List<LoveModuleVo> modules = new ArrayList<>();
        for (String module : MODULE_ORDER) {
            String url = ctx.plan().getPaths().get(routeKeyOf(module));
            if (isBlank(url)) {
                // 路由未注册（留空 / 冲突 / 未启用）→ 不下发入口，模板无需兜底判空
                continue;
            }
            FeatureConfig.ModuleSwitch cfg = findModule(love, module);
            if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
                continue;
            }
            LoveModuleVo item = new LoveModuleVo();
            item.setKey(module);
            item.setTitle(blankTo(cfg.getTitle(), defaultModuleTitle(module)));
            item.setSubTitle(cfg.getSubTitle() == null ? "" : cfg.getSubTitle());
            item.setTitleColor(trimToNull(cfg.getTitleColor()));
            item.setSubTitleColor(trimToNull(cfg.getSubTitleColor()));
            item.setIconBgColor(trimToNull(cfg.getIconBgColor()));
            item.setIconClass(iconClass(module, cfg));
            item.setUrl(url);
            item.setPriority(cfg.getPriority() == null ? 0 : cfg.getPriority());
            item.setLocked(ctx.locked(module));
            modules.add(item);
        }
        modules.sort(Comparator.comparingInt((LoveModuleVo item) -> item.getPriority()).reversed());
        return modules;
    }

    /**
     * 首页摘要：各模块最新 {@value #OVERVIEW_SIZE} 条。
     *
     * 锁定模块不查库，直接给空列表；{@code showOverview=false} 时三个列表全空。
     */
    private Mono<LoveOverviewVo> buildOverview(Ctx ctx, boolean showOverview) {
        LoveOverviewVo overview = new LoveOverviewVo();
        if (!showOverview) {
            return Mono.just(overview);
        }
        Mono<List<LoveStoryVo>> stories = ctx.locked(Constants.LOVE_MODULE_OUR_STORY)
                ? Mono.just(List.of())
                : loveStoryService.listPublic(1, OVERVIEW_SIZE)
                        .map(result -> result.getItems().stream().map(LoveStoryVo::from).toList())
                        .onErrorResume(e -> emptyOnError("故事摘要", e));
        Mono<List<LoveAlbumVo>> albums = ctx.locked(Constants.LOVE_MODULE_PHOTO)
                ? Mono.just(List.of())
                : loveAlbumService.listPublic(1, OVERVIEW_SIZE)
                        .map(result -> result.getItems().stream()
                                .map(album -> LoveAlbumVo.from(album, albumLocked(album)))
                                .toList())
                        .onErrorResume(e -> emptyOnError("相册摘要", e));
        Mono<List<LoveDailyItemVo>> items = ctx.locked(Constants.LOVE_MODULE_DAILY)
                ? Mono.just(List.of())
                : loveDailyItemService.listPublic("", 1, OVERVIEW_SIZE)
                        .map(result -> result.getItems().stream().map(LoveDailyItemVo::from).toList())
                        .onErrorResume(e -> emptyOnError("清单摘要", e));
        return Mono.zip(stories, albums, items).map(tuple -> {
            overview.setStories(tuple.getT1());
            overview.setAlbums(tuple.getT2());
            overview.setItems(tuple.getT3());
            return overview;
        });
    }

    // ------------------------------------------------------------------
    // 小工具
    // ------------------------------------------------------------------

    private <T> Mono<List<T>> emptyOnError(String what, Throwable e) {
        log.warn("读取{}失败，按空列表渲染：{}", what, e.getMessage());
        return Mono.just(List.of());
    }

    private static boolean albumLocked(LoveAlbum album) {
        return album != null && album.getSpec() != null
                && Boolean.TRUE.equals(album.getSpec().getPasswordEnabled());
    }

    private static FeatureConfig.Love loveOf(FeatureConfig config) {
        return config == null || config.getSpec() == null ? null : config.getSpec().getLove();
    }

    /**
     * 恋爱页背景图：取自功能设置
     * {@code spec.love.diaryPage.bgImageUrl} —— 与小程序端同一处配置，
     * 站长只维护一份。
     *
     * v1.5 起刻意不再回落主题页自己的配置：恋爱日记主题页已删除
     * {@code bgImageUrl} 设置项，避免同一个背景图要在两个地方各填一次。
     */
    private static String loveBgImage(FeatureConfig config) {
        if (config == null || config.getSpec() == null || config.getSpec().getLove() == null) {
            return null;
        }
        FeatureConfig.LoveDiaryPage page = config.getSpec().getLove().getDiaryPage();
        return page == null ? null : trimToNull(page.getBgImageUrl());
    }

    private static FeatureConfig.ModuleSwitch findModule(FeatureConfig.Love love, String module) {
        if (love == null) {
            return null;
        }
        return switch (module) {
            case Constants.LOVE_MODULE_DIARY -> love.getLoveDiary();
            case Constants.LOVE_MODULE_OUR_STORY -> love.getOurStory();
            case Constants.LOVE_MODULE_PHOTO -> love.getLovePhoto();
            case Constants.LOVE_MODULE_DAILY -> love.getLoveDaily();
            default -> null;
        };
    }

    private String moduleTitle(Ctx ctx, String module) {
        FeatureConfig.ModuleSwitch cfg = findModule(loveOf(ctx.general()), module);
        return cfg == null ? defaultModuleTitle(module)
                : blankTo(cfg.getTitle(), defaultModuleTitle(module));
    }

    /** 站长配置的图标优先，缺失时按模块 key 回落内置彩色图标。 */
    private static String iconClass(String module, FeatureConfig.ModuleSwitch cfg) {
        String prefix = cfg == null ? null : trimToNull(cfg.getIconPrefix());
        String icon = cfg == null ? null : trimToNull(cfg.getIcon());
        if (prefix != null && icon != null) {
            return prefix + "-" + icon;
        }
        return DEFAULT_ICON_CLASS.getOrDefault(module, "uhlove-icon-aixin");
    }

    /** 模块 key 或路由 key → 模块 key。未知值回落到 loveDiary（只影响文案，不影响数据方法）。 */
    private static String normalizeModuleKey(String module) {
        if (module == null || module.isBlank()) {
            return Constants.LOVE_MODULE_DIARY;
        }
        return switch (module.trim()) {
            case Constants.LOVE_MODULE_OUR_STORY, "stories" -> Constants.LOVE_MODULE_OUR_STORY;
            case Constants.LOVE_MODULE_PHOTO, "albums" -> Constants.LOVE_MODULE_PHOTO;
            case Constants.LOVE_MODULE_DAILY, "daily" -> Constants.LOVE_MODULE_DAILY;
            default -> Constants.LOVE_MODULE_DIARY;
        };
    }

    /** 模块 key → 路由 key（{@code loveDiary} 即首页）。 */
    private static String routeKeyOf(String module) {
        return switch (module) {
            case Constants.LOVE_MODULE_OUR_STORY -> "stories";
            case Constants.LOVE_MODULE_PHOTO -> "albums";
            case Constants.LOVE_MODULE_DAILY -> "daily";
            default -> "home";
        };
    }

    private static String defaultModuleTitle(String module) {
        return switch (module) {
            case Constants.LOVE_MODULE_DIARY -> "恋爱日记";
            case Constants.LOVE_MODULE_OUR_STORY -> "恋爱故事";
            case Constants.LOVE_MODULE_PHOTO -> "恋爱相册";
            case Constants.LOVE_MODULE_DAILY -> "恋爱清单";
            default -> "加密模块";
        };
    }

    private static <T> ListResult<T> empty(int page, int size) {
        return new ListResult<>(page, size, 0L, List.of());
    }

    private static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int size(Integer size) {
        if (size == null || size < 1) {
            return Constants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String blankTo(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 单次渲染的只读上下文。
     */
    private record Ctx(LoveRoutePlan plan, LoveDiaryThemeConfig theme, FeatureConfig general,
                       Map<String, Boolean> locks, boolean captchaRequired) {

        boolean locked(String module) {
            return Boolean.TRUE.equals(locks.get(module));
        }
    }
}
