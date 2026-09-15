package cn.ialley.unihalo.endpoint;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.FeatureConfig;
import cn.ialley.unihalo.services.FeatureConfigService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 功能设置接口（控制台，需登录）。
 *
 * <p>单例读写：GET 不存在时返回默认结构（默认值与存量配置合并），
 * PUT 写入前做非空合并并保存（见 {@link FeatureConfigService}）。</p>
 *
 * @author 小莫唐尼
 */
@Component
public class FeatureConfigEndpoint implements CustomEndpoint {

    private final FeatureConfigService featureConfigService;

    public FeatureConfigEndpoint(FeatureConfigService featureConfigService) {
        this.featureConfigService = featureConfigService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.FEATURE_CONFIG_API_BASE_PATH, this::getFeatureConfig)
                .PUT(Constants.FEATURE_CONFIG_API_BASE_PATH, this::saveFeatureConfig)
                .build();
    }

    private Mono<ServerResponse> getFeatureConfig(ServerRequest request) {
        return featureConfigService.get()
                .flatMap(config -> ServerResponse.ok().bodyValue(config));
    }

    private Mono<ServerResponse> saveFeatureConfig(ServerRequest request) {
        return request.bodyToMono(FeatureConfig.class)
                .flatMap(featureConfigService::save)
                .flatMap(saved -> ServerResponse.ok().bodyValue(saved))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }
}
