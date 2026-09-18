package cn.ialley.unihalo.router;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.ModelConst;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;

import cn.ialley.unihalo.captcha.CaptchaScope;
import cn.ialley.unihalo.captcha.CaptchaService;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.finders.UniHaloFinder;
import cn.ialley.unihalo.utils.LoveDiaryConfigResolver;
import cn.ialley.unihalo.utils.LoveModuleTokenManager;
import cn.ialley.unihalo.utils.LoveRouteResolver;
import cn.ialley.unihalo.utils.LoveUnlockContext;
import cn.ialley.unihalo.vo.LoveConfigVo;
import cn.ialley.unihalo.vo.LoveDiaryThemeConfig;
import cn.ialley.unihalo.vo.LoveModuleAccess;
import cn.ialley.unihalo.vo.LoveModuleVo;
import cn.ialley.unihalo.vo.LovePageVo;
import cn.ialley.unihalo.vo.LoveRoutePlan;

/**
 * 恋爱日记主题页路由：一个谓词 + 一个 handler。
 *
 * <p>路由路径由站长配置，而 {@code @Bean RouterFunction} 只在插件启动时构建一次，
 * 若读进 {@code path()} 则改路径必须重载插件。改为按 {@link LoveRouteResolver#snapshot()}
 * （同步 volatile 快照）判断 GET 路径是否已注册，是则交给同一 handler ——
 * 改配置 30s 内自动生效。谓词只匹配已通过冲突检测的精确路径，冲突路径不匹配 → 404。</p>
 *
 * <p>分页用 Halo 官方路径段式 {@code /page/N} 并兼容 {@code ?page=N}；Finder 返回裸
 * {@code ListResult}，由本层包成 {@link UrlContextListResult} 供模板使用。</p>
 *
 * @author 小莫唐尼
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class LoveDiaryRouter {

    /** key → 默认模板名（主题在 templates/ 下放同名文件即覆盖） */
    private static final Map<String, String> TEMPLATE_NAMES = Map.of(
            "home", "love",
            "stories", "love-stories",
            "albums", "love-albums",
            "daily", "love-daily");

    /**
     * {@code layoutMode=standalone} 时统一使用的模板名。
     *
     * <p>独立外壳不调用主题的 {@code layout :: html(...)} 契约，自带完整
     * {@code <head>}/{@code <body>}；四页正文共用同一批片段，故只需一个外壳文件，
     * 内部按 {@code pageKey} 分派。主题若要覆盖独立外壳，放
     * {@code templates/standalone/love.html}。</p>
     */
    private static final String STANDALONE_TEMPLATE = "standalone/love";

    /** key → {@code _templateId}（HeadProcessor 据此前缀注入资源） */
    private static final Map<String, String> TEMPLATE_IDS = Map.of(
            "home", Constants.LOVE_TEMPLATE_ID_HOME,
            "stories", Constants.LOVE_TEMPLATE_ID_STORIES,
            "albums", Constants.LOVE_TEMPLATE_ID_ALBUMS,
            "daily", Constants.LOVE_TEMPLATE_ID_DAILY);

    /** key → 模块 key（锁判定与摘要用） */
    private static final Map<String, String> MODULE_KEYS = Map.of(
            "home", Constants.LOVE_MODULE_DIARY,
            "stories", Constants.LOVE_MODULE_OUR_STORY,
            "albums", Constants.LOVE_MODULE_PHOTO,
            "daily", Constants.LOVE_MODULE_DAILY);

    private final TemplateNameResolver templateNameResolver;

    private final LoveRouteResolver routeResolver;

    private final LoveDiaryConfigResolver configResolver;

    private final CaptchaService captchaService;

    private final LoveModuleTokenManager moduleTokenManager;

    private final UniHaloFinder uniHaloFinder;

    @Bean
    RouterFunction<ServerResponse> loveDiaryRouterFunction() {
        return RouterFunctions.route(this::isLovePage, this::render);
    }

    // ------------------------------------------------------------------
    // 路由匹配
    // ------------------------------------------------------------------

    /**
     * 同步谓词：仅 GET，且路径命中已注册路由（含列表页 {@code /page/N}）。
     *
     * <p>注意：谓词是<b>同步</b>的，只能读快照，不能 block 数据库。</p>
     *
     * <p>未命中且快照已过期时，顺带触发一次异步重算（fire-and-forget）—— 这样
     * 「站长刚配好新路径」的场景最多只需刷新两次页面；由于每次刷新都会顺延 TTL，
     * 随机 404 不会把它放大成全站扫描风暴。</p>
     */
    private boolean isLovePage(ServerRequest request) {
        if (!HttpMethod.GET.equals(request.method())) {
            return false;
        }
        if (resolveKey(request.path()) != null) {
            return true;
        }
        if (routeResolver.isStale()) {
            routeResolver.refresh().subscribe(plan -> {
                // 只记日志：本请求已经没戏了，刷新是为了下一个请求
            }, error -> log.warn("恋爱日记主题页路由快照刷新失败：{}", error.getMessage()));
        }
        return false;
    }

    /**
     * 路径 → 页面 key（{@code home}/{@code stories}/{@code albums}/{@code daily}）；
     * 不是本插件页面时返回 {@code null}。
     */
    private String resolveKey(String path) {
        for (Map.Entry<String, String> entry : routeResolver.snapshot().getPaths().entrySet()) {
            String key = entry.getKey();
            String base = entry.getValue();
            if (base.equals(path)) {
                return key;
            }
            if (LoveRoutePlan.isListPage(key) && isPagedPath(path, base)) {
                return key;
            }
        }
        return null;
    }

    /** {@code <base>/page/<数字>} 且后面没有更多段。 */
    private static boolean isPagedPath(String path, String base) {
        String prefix = base + "/" + Constants.PAGE_SEGMENT + "/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String tail = path.substring(prefix.length());
        if (tail.isEmpty()) {
            return false;
        }
        for (int i = 0; i < tail.length(); i++) {
            if (!Character.isDigit(tail.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    private Mono<ServerResponse> render(ServerRequest request) {
        String path = request.path();
        String key = resolveKey(path);
        if (key == null) {
            // 快照在谓词与 handler 之间发生变化（例如刚检测出冲突）→ 按不存在处理
            return ServerResponse.notFound().build();
        }
        return LoveUnlockContext.withUnlocked(buildModel(request, key), unlockedModules(request))
                .flatMap(model -> templateNameResolver
                        .resolveTemplateNameOrDefault(request.exchange(), templateName(key, model))
                        .flatMap(templateName -> ServerResponse.ok().render(templateName, model)))
                .onErrorResume(e -> {
                    log.error("渲染恋爱日记页面失败：path={}", path, e);
                    return ServerResponse.notFound().build();
                });
    }

    /**
     * 从请求头里识别「这次渲染已经解锁了哪些模块」。
     *
     * <p>只有本插件自己的 JS 在解锁成功后会用同源 fetch 带上这个头 —— 普通导航不带，
     * 所以锁定页照旧只出解锁表单（HTML 里零业务字段）；带上之后同一次渲染就能直出内容，
     * 于是<b>四页模板只有一套，不需要在前端重写一遍 markup</b>。</p>
     *
     * <p>token 由 {@link LoveModuleTokenManager} 校验（HMAC + 30 分钟有效期），
     * 逐个模块试配：一个 token 只对它自己的 scope 生效，跨模块不通用。</p>
     */
    private Set<String> unlockedModules(ServerRequest request) {
        String token = request.headers().firstHeader(LoveUnlockContext.HEADER);
        if (token == null || token.isBlank()) {
            return Set.of();
        }
        Set<String> unlocked = new LinkedHashSet<>();
        for (String module : MODULE_KEYS.values()) {
            if (moduleTokenManager.verify(module, token)) {
                unlocked.add(module);
            }
        }
        return unlocked;
    }

    /**
     * 模板名：{@code auto} → 每页独立名（便于主题单页覆盖）；{@code standalone} → 统一外壳。
     */
    private static String templateName(String key, Map<String, Object> model) {
        return "standalone".equals(model.get("layoutMode")) ? STANDALONE_TEMPLATE
                : TEMPLATE_NAMES.get(key);
    }

    private Mono<Map<String, Object>> buildModel(ServerRequest request, String key) {
        return Mono.zip(routeResolver.resolvePlan(), configResolver.resolve(),
                        uniHaloFinder.loveConfig())
                .flatMap(tuple -> {
                    LoveRoutePlan plan = tuple.getT1();
                    LoveDiaryThemeConfig theme = tuple.getT2();
                    LoveConfigVo config = tuple.getT3();

                    Map<String, Object> model = new LinkedHashMap<>();
                    model.put(ModelConst.TEMPLATE_ID, TEMPLATE_IDS.get(key));
                    // 本页渲染结果可能依请求头（解锁 token）而不同，绝不能进中间缓存
                    model.put(ModelConst.NO_CACHE, true);
                    model.put("pageKey", key);
                    model.put("routes", plan.getPaths());
                    model.put("loveConfig", config);
                    model.put("moduleLocks", moduleLocks(config));
                    model.put("showOverview", theme.isShowOverview());
                    model.put("layoutMode", theme.getLayoutMode());
                    model.put("pageTitle", pageTitle(config, key));

                    if ("home".equals(key)) {
                        return homeModel(model);
                    }
                    if ("albums".equals(key)) {
                        // 相册锁的验证码 scope 与模块锁不同，模板要按各自 scope 决定是否渲染输入框
                        return albumCaptchaRequired().flatMap(required -> {
                            model.put("albumCaptchaRequired", required);
                            return listModel(request, model, key);
                        });
                    }
                    return listModel(request, model, key);
                });
    }

    /**
     * 相册锁 scope 是否需要验证码。
     *
     * <p>读取失败时<b>按 true（fail-closed）</b>：多渲染一个验证码框最多是「填了没用」，
     * 而少渲染一个框会让解锁接口在 {@code requireValid} 处直接 403，用户永远解不开。</p>
     */
    private Mono<Boolean> albumCaptchaRequired() {
        return captchaService.requiredFor(CaptchaScope.LOVE_ALBUM_UNLOCK)
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.warn("读取相册解锁验证码开关失败，按「需要验证码」渲染：{}", e.getMessage());
                    return Mono.just(true);
                });
    }

    /**
     * 首页：把 {@code loveHome()} 的四个部分摊进 model。
     *
     * <p>恋爱日记入口加密时 {@code pageVo.getConfig()}/{@code overview} 为 null，
     * 模板据 {@code access.locked} 只渲染解锁表单。</p>
     *
     * <p><b>⚠️ 安全要点</b>：这里<b>不能</b>在锁定态回落到外层那份「路由层配置」。
     * {@link UniHaloFinder#loveConfig()} 不感知锁状态，它带着昵称/头像/纪念日/模块入口，
     * 一旦回落，锁定首页的 HTML 里就会重新出现这些业务字段 —— 与
     * {@link LovePageVo} 的「锁定态零业务字段」契约直接冲突。故锁定时显式置 null。</p>
     */
    private Mono<Map<String, Object>> homeModel(Map<String, Object> model) {
        return uniHaloFinder.loveHome().map(page -> {
            LovePageVo pageVo = page == null ? new LovePageVo() : page;
            LoveModuleAccess access = pageVo.getAccess() == null
                    ? LoveModuleAccess.unlocked(Constants.LOVE_MODULE_DIARY) : pageVo.getAccess();
            model.put("access", access);
            if (pageVo.getConfig() != null) {
                model.put("loveConfig", pageVo.getConfig());
                model.put("moduleLocks", moduleLocks(pageVo.getConfig()));
            } else if (access.isLocked()) {
                // 锁定首页：配置整体不下发（含昵称/头像/纪念日/模块入口），只留锁状态
                model.put("loveConfig", null);
                model.put("moduleLocks", Map.of());
            }
            model.put("overview", pageVo.getOverview());
            if (pageVo.getRoutes() != null && !pageVo.getRoutes().isEmpty()) {
                model.put("routes", pageVo.getRoutes());
            }
            return model;
        });
    }

    /**
     * 列表页：{@code access} + 分页列表（{@code stories}/{@code albums}/{@code items}）。
     */
    private Mono<Map<String, Object>> listModel(ServerRequest request, Map<String, Object> model,
            String key) {
        int page = resolvePage(request);
        int size = ModelConst.DEFAULT_PAGE_SIZE;
        String path = request.path();
        String status = request.queryParam("status").orElse("").trim();

        Mono<? extends ListResult<?>> listMono = switch (key) {
            case "stories" -> uniHaloFinder.listLoveStories(page, size);
            case "albums" -> uniHaloFinder.listLoveAlbums(page, size);
            default -> uniHaloFinder.listLoveItems(status, page, size);
        };

        String itemKey = switch (key) {
            case "stories" -> "stories";
            case "albums" -> "albums";
            default -> "items";
        };

        return Mono.zip(uniHaloFinder.loveModuleAccess(MODULE_KEYS.get(key)), listMono)
                .map(tuple -> {
                    model.put("access", tuple.getT1());
                    if ("daily".equals(key)) {
                        model.put("status", status);
                    }
                    model.put(itemKey, wrap(tuple.getT2(), path));
                    return model;
                });
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 包成 Halo 标准分页容器（自带 prevUrl / nextUrl；首页无上一页/下一页时为 null）。 */
    private static UrlContextListResult<?> wrap(ListResult<?> result, String path) {
        long totalPages = result.getSize() < 1 ? 0 : (result.getTotal() - 1) / result.getSize() + 1;
        String nextUrl = result.getPage() < totalPages
                ? PageUrlUtils.nextPageUrl(path, totalPages) : null;
        String prevUrl = result.getPage() > 1 ? PageUrlUtils.prevPageUrl(path) : null;
        return new UrlContextListResult<>(result.getPage(), result.getSize(), result.getTotal(),
                result.getItems(), nextUrl, prevUrl);
    }

    /** 分页页码：路径段式 {@code /page/N} 优先，兼容 {@code ?page=N}。 */
    private static int resolvePage(ServerRequest request) {
        int fromPath = PageUrlUtils.pageNum(request);
        if (fromPath > 1) {
            return fromPath;
        }
        return request.queryParam("page")
                .map(value -> {
                    try {
                        return Math.max(1, Integer.parseInt(value.trim()));
                    } catch (NumberFormatException e) {
                        return 1;
                    }
                })
                .orElse(1);
    }

    /**
     * 模块 key → 是否锁定。
     *
     * <p>首页摘要在锁定模块上返回的是<b>空列表</b>（连条数都不给），但 UI 需要区分
     * 「暂无内容」与「已加密」——前者是空状态文案，后者必须是锁占位。
     * 光看列表长度无法区分，故把锁标记单独下发一份给模板。</p>
     */
    private static Map<String, Boolean> moduleLocks(LoveConfigVo config) {
        if (config == null || config.getModules() == null || config.getModules().isEmpty()) {
            return Map.of();
        }
        Map<String, Boolean> locks = new LinkedHashMap<>();
        config.getModules().forEach(module -> locks.put(module.getKey(), module.isLocked()));
        return locks;
    }

    /**
     * 页面标题：优先用站长在「模块入口」里配的名称，其次内置名称。
     * 模板配合 {@code site.title} 组成最终 {@code <title>}。
     */
    private static String pageTitle(LoveConfigVo config, String key) {
        String fallback = switch (key) {
            case "stories" -> "恋爱故事";
            case "albums" -> "恋爱相册";
            case "daily" -> "恋爱清单";
            default -> "恋爱日记";
        };
        if (config == null || config.getModules() == null) {
            return fallback;
        }
        String moduleKey = MODULE_KEYS.get(key);
        return config.getModules().stream()
                .filter(item -> moduleKey.equals(item.getKey()))
                .map(LoveModuleVo::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .findFirst()
                .orElse(fallback);
    }
}
