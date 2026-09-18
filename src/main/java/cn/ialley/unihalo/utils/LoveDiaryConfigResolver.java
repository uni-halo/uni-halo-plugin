package cn.ialley.unihalo.utils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;

import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.vo.LoveDiaryThemeConfig;

/**
 * 恋爱日记主题页配置解析器：从 setting.yaml 的 {@code themeConfig.loveDiaryTheme}
 * 组读取配置并归一化。fail-closed：单个字段非法只回落该字段默认值；整组缺失 /
 * 读取异常 / {@code enabled != true} 一律返回 {@link LoveDiaryThemeConfig#disabled()}，
 * 插件据此不注册路由、不注入资源。v1.5 起本组只剩「路由 / 外壳 / 强调色 / 首页摘要」
 * 四件事（背景图移到功能设置，正文排版与高亮不再可配）。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveDiaryConfigResolver {

    private static final String DEFAULT_PRIMARY_COLOR = "#f83856";

    private static final String DEFAULT_ROUTE_HOME = "/love";

    private static final String DEFAULT_ROUTE_STORIES = "stories";

    private static final String DEFAULT_ROUTE_ALBUMS = "albums";

    private static final String DEFAULT_ROUTE_DAILY = "daily";

    /** 允许的强调色写法：#rgb / #rrggbb / #rrggbbaa */
    private static final Pattern COLOR_PATTERN =
            Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");

    private static final Set<String> LAYOUT_MODES = Set.of("auto", "standalone");

    private final ReactiveSettingFetcher settingFetcher;

    /**
     * 读取并归一化配置。永不抛错，异常时返回关闭态。
     */
    public Mono<LoveDiaryThemeConfig> resolve() {
        Mono<LoveDiaryThemeConfig> fallback = Mono.just(LoveDiaryThemeConfig.disabled());
        try {
            Mono<JsonNode> group = SettingGroupResolver.group(settingFetcher,
                    Constants.SETTING_DOMAIN_THEME_CONFIG,
                    Constants.SETTING_MODULE_LOVE_DIARY_THEME);
            if (group == null) {
                return fallback;
            }
            return group.map(this::fromNode)
                    .defaultIfEmpty(LoveDiaryThemeConfig.disabled())
                    .onErrorResume(e -> {
                        log.warn("读取恋爱日记主题页配置失败，按未启用处理：{}", e.getMessage());
                        return fallback;
                    });
        } catch (Exception e) {
            log.warn("解析恋爱日记主题页配置异常，按未启用处理：{}", e.getMessage());
            return fallback;
        }
    }

    /**
     * 读取并归一化配置，若未启用则返回空（调用方据此短路）。
     */
    public Mono<LoveDiaryThemeConfig> resolveEnabled() {
        return resolve().filter(LoveDiaryThemeConfig::isEnabled);
    }

    private LoveDiaryThemeConfig fromNode(JsonNode node) {
        LoveDiaryThemeConfig config = LoveDiaryThemeConfig.disabled();
        config.setEnabled(node.path("enabled").asBoolean(false));

        JsonNode routes = node.path("routes");
        config.setRawRouteHome(routes.path("home").asString(DEFAULT_ROUTE_HOME));
        config.setRawRouteStories(routes.path("stories").asString(DEFAULT_ROUTE_STORIES));
        config.setRawRouteAlbums(routes.path("albums").asString(DEFAULT_ROUTE_ALBUMS));
        config.setRawRouteDaily(routes.path("daily").asString(DEFAULT_ROUTE_DAILY));

        config.setLayoutMode(oneOf(node.path("layoutMode").asString("auto"), LAYOUT_MODES, "auto"));
        config.setPrimaryColor(color(node.path("primaryColor").asString(DEFAULT_PRIMARY_COLOR)));
        config.setShowOverview(node.path("showOverview").asBoolean(true));
        return config;
    }

    private static String oneOf(String value, Set<String> allowed, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (allowed.contains(normalized)) {
            return normalized;
        }
        log.debug("恋爱日记主题页配置取值非法，回落默认值：{} -> {}", normalized, fallback);
        return fallback;
    }

    private static String color(String value) {
        if (value != null && COLOR_PATTERN.matcher(value.trim()).matches()) {
            return value.trim().toLowerCase(Locale.ROOT);
        }
        return DEFAULT_PRIMARY_COLOR;
    }
}
