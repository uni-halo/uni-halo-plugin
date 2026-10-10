package cn.ialley.unihalo.endpoint;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.services.DrawService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 绘画作品公开接口（匿名可访问，仅返回已通过审核的画作）。
 *
 * @author 小莫唐尼
 */
@Component
public class DrawPublicEndpoint implements CustomEndpoint {

    private final DrawService drawService;

    public DrawPublicEndpoint(DrawService drawService) {
        this.drawService = drawService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.DRAW_API_BASE_PATH, this::listDraws)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}", this::getDraw)
                .build();
    }

    private Mono<ServerResponse> listDraws(ServerRequest request) {
        return drawService.listPublic(queryPage(request), querySize(request))
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getDraw(ServerRequest request) {
        return drawService.getPublic(request.pathVariable("name"))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw));
    }

    private static int queryPage(ServerRequest request) {
        return request.queryParam("page").map(Integer::parseInt).orElse(1);
    }

    private static int querySize(ServerRequest request) {
        return request.queryParam("size").map(Integer::parseInt)
                .orElse(Constants.DEFAULT_PAGE_SIZE);
    }
}
