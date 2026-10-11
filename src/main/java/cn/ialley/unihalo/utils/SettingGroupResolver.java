package cn.ialley.unihalo.utils;

import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 设置组读取工具。
 *
 * 插件设置合并为功能域组后，模块级配置为域组下的子对象
 * （如 safetyConfig.captchaConfig）。本工具按「域组.模块」路径读取，
 * 节点缺失时返回空。
 *
 * @author 小莫唐尼
 */
public final class SettingGroupResolver {

    private SettingGroupResolver() {
    }

    /**
     * 读取域组下的模块子对象；域组或模块缺失时返回空 Mono。
     *
     * @param fetcher 设置读取器
     * @param domain  域组名（safetyConfig / integrationConfig / themeWidgetConfig / themeTemplateConfig）
     * @param module  模块名（域组下的子对象键）
     */
    public static Mono<JsonNode> group(ReactiveSettingFetcher fetcher, String domain,
            String module) {
        Mono<JsonNode> domainMono = fetcher.getSettingValue(domain);
        if (domainMono == null) {
            // 兼容 Mockito 未 stub 域键（返回 null）的调用方/测试
            return Mono.empty();
        }
        return domainMono
                .map(node -> {
                    if (node == null) {
                        return JsonNodeFactory.instance.missingNode();
                    }
                    JsonNode child = node.get(module);
                    return child == null ? JsonNodeFactory.instance.missingNode() : child;
                })
                .filter(node -> !node.isMissingNode() && !node.isNull());
    }
}
