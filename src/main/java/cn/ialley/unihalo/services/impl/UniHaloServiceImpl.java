package cn.ialley.unihalo.services.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.services.UniHaloService;
import cn.ialley.unihalo.utils.PublicConfigAssembler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务实现
 *
 * @author 小莫唐尼
 */
@Component
@RequiredArgsConstructor
public class UniHaloServiceImpl implements UniHaloService {

    private final ReactiveSettingFetcher settingFetcher;
    private final FeatureConfigService featureConfigService;
    private final PublicConfigAssembler publicConfigAssembler = new PublicConfigAssembler();

    /**
     * 获取移动端的所有配置
     *
     * @return 配置
     */
    @Override
    public Mono<Map<String, JsonNode>> getAppConfigs() {
        return settingFetcher.getSettingValues()
                .defaultIfEmpty(Map.of())
                .flatMap(settings -> featureConfigService.get()
                        .map(featureConfig -> toMap(
                                publicConfigAssembler.assemble(settings, featureConfig))));
    }

    /*
     * 根据分组名称获取配置
     *
     * @param groupName 分组名称
     * @return 配置
     */
    @Override
    public Mono<JsonNode> getAppConfigsByGroupName(String groupName) {
        return settingFetcher.getSettingValues()
                .defaultIfEmpty(Map.of())
                .flatMap(settings -> featureConfigService.get()
                        .map(featureConfig -> {
                            JsonNode root = publicConfigAssembler.assemble(settings, featureConfig);
                            JsonNode group = root.get(groupName);
                            return group == null ? JsonNodeFactory.instance.objectNode() : group;
                        }));
    }

    private static Map<String, JsonNode> toMap(JsonNode root) {
        Map<String, JsonNode> result = new HashMap<>();
        if (root != null && root.isObject()) {
            root.properties().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
