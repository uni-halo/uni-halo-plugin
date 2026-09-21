package cn.ialley.unihalo.endpoint;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.LoveInfo;
import cn.ialley.unihalo.services.LoveInfoService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.Metadata;

/**
 * 恋爱信息接口（控制台，需登录）。
 *
 * 单例读写（固定 {@code name = love-info}）：GET 不存在时返回空 spec；
 * PUT upsert，强制以固定 name 落库。
 *
 * @author 小莫唐尼
 */
@Component
public class LoveInfoEndpoint implements CustomEndpoint {

    private final LoveInfoService loveInfoService;

    public LoveInfoEndpoint(LoveInfoService loveInfoService) {
        this.loveInfoService = loveInfoService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.LOVE_INFO_API_BASE_PATH, this::getLoveInfo)
                .PUT(Constants.LOVE_INFO_API_BASE_PATH, this::saveLoveInfo)
                .build();
    }

    private Mono<ServerResponse> getLoveInfo(ServerRequest request) {
        return loveInfoService.fetchOrDefault()
                .flatMap(info -> ServerResponse.ok().bodyValue(info));
    }

    private Mono<ServerResponse> saveLoveInfo(ServerRequest request) {
        return request.bodyToMono(LoveInfo.class)
                .flatMap(body -> {
                    if (body.getMetadata() == null) {
                        body.setMetadata(new Metadata());
                    }
                    body.getMetadata().setName(Constants.LOVE_INFO_SINGLETON_NAME);
                    return loveInfoService.save(body.getSpec() == null
                            ? new LoveInfo.LoveInfoSpec() : body.getSpec());
                })
                .flatMap(saved -> ServerResponse.ok().bodyValue(saved))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }
}
