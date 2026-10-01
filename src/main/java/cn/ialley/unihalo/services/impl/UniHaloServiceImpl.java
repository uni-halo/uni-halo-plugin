package cn.ialley.unihalo.services.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import cn.ialley.unihalo.scheme.AuditDataConfig;
import cn.ialley.unihalo.services.AuditDataService;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.services.UniHaloService;
import cn.ialley.unihalo.utils.PublicConfigAssembler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

import java.util.HashMap;
import java.util.List;
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
    private final AuditDataService auditDataService;
    private final PublicConfigAssembler publicConfigAssembler = new PublicConfigAssembler();

    /**
     * 获取移动端的所有配置
     *
     * @return 配置
     */
    @Override
    public Mono<Map<String, JsonNode>> getAppConfigs() {
        return Mono.zip(settingFetcher.getSettingValues().defaultIfEmpty(Map.of()),
                        featureConfigService.get(), auditDataService.get())
                .map(tuple -> toMap(publicConfigAssembler.assemble(tuple.getT1(), tuple.getT2(),
                        isAuditModeEnabled(tuple.getT3()), auditHiddenNavKeys(tuple.getT3()))));
    }

    /*
     * 根据分组名称获取配置
     *
     * @param groupName 分组名称
     * @return 配置
     */
    @Override
    public Mono<JsonNode> getAppConfigsByGroupName(String groupName) {
        return Mono.zip(settingFetcher.getSettingValues().defaultIfEmpty(Map.of()),
                        featureConfigService.get(), auditDataService.get())
                .map(tuple -> {
                    JsonNode root = publicConfigAssembler.assemble(tuple.getT1(), tuple.getT2(),
                            isAuditModeEnabled(tuple.getT3()), auditHiddenNavKeys(tuple.getT3()));
                    JsonNode group = root.get(groupName);
                    return group == null ? JsonNodeFactory.instance.objectNode() : group;
                });
    }

    /** 审核模式开关（AuditDataConfig.spec.enabled，单例缺失/字段缺失视为关闭） */
    private static boolean isAuditModeEnabled(AuditDataConfig config) {
        return config != null && config.getSpec() != null
                && Boolean.TRUE.equals(config.getSpec().getEnabled());
    }

    /** 审核期间隐藏的功能入口 key 列表（缺失返回空列表） */
    private static List<String> auditHiddenNavKeys(AuditDataConfig config) {
        if (config == null || config.getSpec() == null || config.getSpec().getHiddenNavKeys() == null) {
            return List.of();
        }
        return config.getSpec().getHiddenNavKeys();
    }

    private static Map<String, JsonNode> toMap(JsonNode root) {
        Map<String, JsonNode> result = new HashMap<>();
        if (root != null && root.isObject()) {
            root.properties().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
