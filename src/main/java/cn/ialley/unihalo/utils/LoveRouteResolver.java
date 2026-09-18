package cn.ialley.unihalo.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.SinglePage;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;

import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.vo.LoveDiaryThemeConfig;
import cn.ialley.unihalo.vo.LoveRoutePlan;

import static run.halo.app.extension.index.query.Queries.isNull;

/**
 * 恋爱日记路由解析与冲突检测。
 *
 * 路径契约：以 {@code /} 开头为绝对路径原样使用；否则解析为 {@code <home>/<子段>}；
 * 留空则该页不注册。
 *
 * 冲突检测逐条 fail-closed（冲突的那一条不注册，不影响其余）：非法路径/保留段
 * {@code page}、Halo 保留根段、已占用路径（比对现有内容的实际 permalink 值而非模板，
 * 站长改过 permalink 规则也准确）、四条路由解析后自身重复。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveRouteResolver {

    /** 单个路径段允许的字符（Halo permalink 段的安全子集） */
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_.~]+$");

    /**
     * Halo 占用的根级路径段（来自官方「模板路由」文档的内置路由 + 系统固定路径）。
     * 仅比对第一段，因为这些命名空间都挂在根下。
     */
    private static final Set<String> RESERVED_FIRST_SEGMENTS = Set.of(
            // 官方内置模板路由
            "index", "page", "archives", "tags", "categories", "authors",
            // 用户中心 / 认证
            "login", "logout", "signup", "signup-success", "activate", "password-reset",
            "oauth2", "uc",
            // 系统与基础设施
            "console", "api", "apis", "assets", "plugins", "upload", "attachments",
            "actuator", "errors", "error", "preview",
            // 站点级固定文件与订阅
            "search", "feed", "rss", "atom", "sitemap", "sitemap.xml", "robots.txt",
            "favicon.ico", "manifest.webmanifest"
    );

    /**
     * 路由计划的短期缓存时长。
     *
     * {@link #collectOccupied()} 会列举全站 Post/Category/Tag/SinglePage ——
     * 每次页面渲染都跑一遍是不可接受的。路由只在「配置变更 / 内容 permalink 变更」时
     * 才需要重算，故此处做 30s 短缓存：最坏情况下新内容占用我们路径后 30s 内仍可能
     * 被插件路由抢先，之后自动收敛；路由注册路径用 {@link #freshPlan()} 不吃缓存。
     */
    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final ReactiveExtensionClient client;

    private final LoveDiaryConfigResolver configResolver;

    private volatile CachedPlan cache;

    /**
     * 同步读取当前路由快照（可能为空计划；永不返回 null）。
     *
     * 存在的唯一理由：Spring WebFlux 的 {@code RequestPredicate#test} 是同步的，
     * 而 Reactor Netty 的事件循环线程上调用 {@code block()} 会直接抛
     * {@code IllegalStateException}。所以路由匹配谓词只能读这个 volatile 快照，
     * 不能现算。
     */
    public LoveRoutePlan snapshot() {
        CachedPlan cached = cache;
        return cached == null ? new LoveRoutePlan() : cached.plan();
    }

    /**
     * 快照是否已过期（或尚未生成）。
     *
     * 供同步的路由谓词在「未命中」时决定「是否值得触发一次异步重算」——
     * 由于任何一次刷新都会把过期时间往后推一个 TTL，所以最坏也只是每个 TTL 窗口
     * 触发一次全站扫描，不会被随机 404 放大成 DoS。
     */
    public boolean isStale() {
        CachedPlan cached = cache;
        return cached == null || System.currentTimeMillis() >= cached.expiresAt();
    }

    /**
     * 解析配置并产出路由计划（含冲突检测）。配置未启用时返回空计划
     * （不是 {@code Mono.empty()}，调用方用 {@link LoveRoutePlan#isEmpty()} 判空）。
     *
     * 带 30s 短缓存，适合页面渲染路径反复调用。
     */
    public Mono<LoveRoutePlan> resolvePlan() {
        CachedPlan cached = cache;
        if (cached != null && System.currentTimeMillis() < cached.expiresAt()) {
            return Mono.just(cached.plan());
        }
        return refresh();
    }

    /**
     * 强制重新解析并写入快照（跳过缓存）。
     *
     * 调用时机：插件启动、{@code PluginConfigUpdatedEvent}（设置保存）到达时。
     * 冲突与未配置项会在此处打日志，站长可在 Halo 日志里看到「哪条路为什么不注册」。
     */
    public Mono<LoveRoutePlan> refresh() {
        return freshPlan().doOnNext(plan -> {
            cache(plan);
            if (!plan.getConflicts().isEmpty()) {
                log.warn("恋爱日记主题页有 {} 条路由因冲突未注册（fail-closed）：{}",
                        plan.getConflicts().size(), plan.getConflicts());
            }
            if (!plan.getSkipped().isEmpty()) {
                log.info("恋爱日记主题页有 {} 条路由未配置（主动不注册）：{}",
                        plan.getSkipped().size(), plan.getSkipped());
            }
            if (!plan.isEmpty()) {
                log.info("恋爱日记主题页已注册路由：{}", plan.getPaths());
            }
        });
    }

    /**
     * 强制重新解析（不吃缓存、不写快照）。供测试与显式场景使用。
     */
    public Mono<LoveRoutePlan> freshPlan() {
        return configResolver.resolveEnabled()
                .flatMap(this::resolvePlan)
                .defaultIfEmpty(new LoveRoutePlan());
    }

    /**
     * 失效缓存（设置保存后由控制台接口调用，使新路径立即生效）。
     */
    public void invalidateCache() {
        cache = null;
    }

    private void cache(LoveRoutePlan plan) {
        cache = new CachedPlan(plan, System.currentTimeMillis() + CACHE_TTL_MILLIS);
    }

    private record CachedPlan(LoveRoutePlan plan, long expiresAt) {
    }

    /**
     * 对给定配置解析路由计划。
     */
    public Mono<LoveRoutePlan> resolvePlan(LoveDiaryThemeConfig config) {
        // 1. 纯函数解析（不依赖外部数据）
        LoveRoutePlan plan = new LoveRoutePlan();

        String homePath = null;
        String homeInvalid = invalidAbsolute(config.getRawRouteHome());
        if (isBlank(config.getRawRouteHome())) {
            plan.getSkipped().add("home");
        } else if (homeInvalid != null) {
            plan.getConflicts().put("home", homeInvalid);
        } else {
            homePath = normalizeAbsolute(config.getRawRouteHome());
        }

        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("stories", config.getRawRouteStories());
        raw.put("albums", config.getRawRouteAlbums());
        raw.put("daily", config.getRawRouteDaily());

        Map<String, String> resolved = new LinkedHashMap<>();
        if (homePath != null) {
            resolved.put("home", homePath);
        }
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isBlank(value)) {
                plan.getSkipped().add(key);
                continue;
            }
            String path;
            String reason;
            if (value.trim().startsWith("/")) {
                reason = invalidAbsolute(value);
                path = reason == null ? normalizeAbsolute(value) : null;
            } else {
                if (homePath == null) {
                    reason = "首页路径未配置或非法，无法解析相对子段";
                    path = null;
                } else {
                    String segment = trimSlashes(value);
                    String combined = homePath + "/" + segment;
                    reason = invalidAbsolute(combined);
                    path = reason == null ? normalizeAbsolute(combined) : null;
                }
            }
            if (reason != null) {
                plan.getConflicts().put(key, reason);
            } else if (path != null) {
                resolved.put(key, path);
            }
        }

        // 2. 自身重复（先做，避免无谓的外部查询）
        Set<String> seen = new LinkedHashSet<>();
        Map<String, String> deduped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : resolved.entrySet()) {
            if (seen.add(entry.getValue())) {
                deduped.put(entry.getKey(), entry.getValue());
            } else {
                plan.getConflicts().put(entry.getKey(), "与另一条路由路径重复：" + entry.getValue());
            }
        }

        if (deduped.isEmpty()) {
            plan.setCheckedSources("无需比对（无有效路径）");
            return Mono.just(plan);
        }

        // 3. 保留根段：直接判定
        List<String> reservedHit = deduped.entrySet().stream()
                .filter(e -> RESERVED_FIRST_SEGMENTS.contains(firstSegment(e.getValue())))
                .map(Map.Entry::getKey)
                .toList();
        Map<String, String> candidates = new LinkedHashMap<>();
        deduped.forEach((key, path) -> {
            if (reservedHit.contains(key)) {
                plan.getConflicts().put(key, "占用 Halo 保留根段：" + firstSegment(path));
            } else {
                candidates.put(key, path);
            }
        });

        if (candidates.isEmpty()) {
            plan.setCheckedSources("Halo 保留根段");
            return Mono.just(plan);
        }

        // 4. 与 Halo 现有内容的实际 permalink 比对
        return collectOccupied()
                .map(occupied -> {
                    candidates.forEach((key, path) -> {
                        if (occupied.contains(path)) {
                            plan.getConflicts().put(key, "已被现有内容占用：" + path);
                        } else {
                            plan.getPaths().put(key, path);
                        }
                    });
                    plan.setCheckedSources("Halo 保留根段 + Post/Category/Tag 的 status.permalink + SinglePage 的 slug");
                    return plan;
                });
    }

    /**
     * 收集 Halo 已占用的路径。
     *
     * 只保留「归一化后的路径字符串」，内存为 O(1) 量级的字符串集合；
     * 但 {@code listAll} 会实例化全部内容对象，故仅在功能启用时执行一次。
     */
    private Mono<Set<String>> collectOccupied() {
        ListOptions alive = ListOptions.builder()
                .fieldQuery(isNull("metadata.deletionTimestamp"))
                .build();

        Mono<Set<String>> singlePages = client.listAll(SinglePage.class, alive, Sort.unsorted())
                .map(page -> {
                    String permalink = page.getStatus() == null ? null : page.getStatus().getPermalink();
                    if (!isBlank(permalink)) {
                        return permalink;
                    }
                    String slug = page.getSpec() == null ? null : page.getSpec().getSlug();
                    return "/" + (isBlank(slug) ? page.getMetadata().getName() : slug);
                })
                .collectList()
                .map(LoveRouteResolver::normalizeAll);

        Mono<Set<String>> posts = client.listAll(Post.class, alive, Sort.unsorted())
                .map(post -> post.getStatus() == null ? null : post.getStatus().getPermalink())
                .filter(permalink -> !isBlank(permalink))
                .collectList()
                .map(LoveRouteResolver::normalizeAll);

        Mono<Set<String>> categories = client.listAll(Category.class, alive, Sort.unsorted())
                .map(category -> category.getStatus() == null ? null : category.getStatus().getPermalink())
                .filter(permalink -> !isBlank(permalink))
                .collectList()
                .map(LoveRouteResolver::normalizeAll);

        Mono<Set<String>> tags = client.listAll(Tag.class, alive, Sort.unsorted())
                .map(tag -> tag.getStatus() == null ? null : tag.getStatus().getPermalink())
                .filter(permalink -> !isBlank(permalink))
                .collectList()
                .map(LoveRouteResolver::normalizeAll);

        return Flux.merge(singlePages, posts, categories, tags)
                .collectList()
                .map(sets -> {
                    Set<String> merged = new LinkedHashSet<>();
                    sets.forEach(merged::addAll);
                    return merged;
                })
                .onErrorResume(e -> {
                    // 读取失败不阻断注册，但记录告警：此时仅剩「保留根段」这一层保护
                    log.warn("读取 Halo 已占用 permalink 失败，本次仅按保留根段做冲突检测：{}", e.getMessage());
                    return Mono.just(Set.of());
                });
    }

    private static Set<String> normalizeAll(List<String> paths) {
        Set<String> result = new LinkedHashSet<>();
        for (String path : paths) {
            String normalized = normalizeAbsolute(path);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    /** 校验绝对路径；合法返回 null，非法返回原因。 */
    private static String invalidAbsolute(String raw) {
        if (raw == null) {
            return "路径为空";
        }
        String path = raw.trim();
        if (path.isEmpty()) {
            return "路径为空";
        }
        if (!path.startsWith("/")) {
            return "绝对路径必须以 / 开头";
        }
        if (path.contains("?") || path.contains("#")) {
            return "路径不能包含 ? 或 #";
        }
        if (path.contains("//")) {
            return "路径不能包含连续斜杠";
        }
        String trimmed = trimTrailingSlash(path);
        if ("/".equals(trimmed)) {
            return "不允许占用站点根路径 /";
        }
        for (String segment : trimmed.substring(1).split("/", -1)) {
            if (segment.isEmpty()) {
                return "路径存在空段";
            }
            if (!SEGMENT_PATTERN.matcher(segment).matches()) {
                return "路径段含非法字符：" + segment;
            }
            if (Constants.PAGE_SEGMENT.equals(segment)) {
                return "路径段 page 与本插件的 /page/{page} 分页路径冲突";
            }
        }
        return null;
    }

    /** 归一化为以 / 开头、无末尾斜杠的路径；非法返回 null。 */
    private static String normalizeAbsolute(String raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.trim();
        if (path.isEmpty() || !path.startsWith("/")) {
            return null;
        }
        path = trimTrailingSlash(path);
        return "/".equals(path) ? null : path;
    }

    private static String trimTrailingSlash(String path) {
        String result = path;
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String trimSlashes(String value) {
        String result = value == null ? "" : value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String firstSegment(String path) {
        String rest = path.startsWith("/") ? path.substring(1) : path;
        int index = rest.indexOf('/');
        return index < 0 ? rest : rest.substring(0, index);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
