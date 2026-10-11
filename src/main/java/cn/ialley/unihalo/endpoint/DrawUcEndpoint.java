package cn.ialley.unihalo.endpoint;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.services.DrawGuessService;
import cn.ialley.unihalo.services.DrawService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 绘画作品用户中心接口（App 端，需登录且持有 uc-draw-use 角色）。
 *
 * 所有查询/删除均以当前登录用户为 owner，只能看到和操作自己的绘画。
 *
 * @author 小莫唐尼
 */
@Component
@RequiredArgsConstructor
public class DrawUcEndpoint implements CustomEndpoint {

    private final DrawService drawService;
    private final DrawGuessService drawGuessService;
    private final ReactiveExtensionClient client;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.UC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.DRAW_API_BASE_PATH, this::listMyDraws)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}", this::getMyDraw)
                .POST(Constants.DRAW_API_BASE_PATH, this::createMyDraw)
                .DELETE(Constants.DRAW_API_BASE_PATH + "/{name}", this::deleteMyDraw)
                .PUT(Constants.DRAW_API_BASE_PATH + "/{name}/hidden", this::setMyHidden)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}/puzzle", this::getMyPuzzle)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/reveal", this::revealMyPuzzle)
                .build();
    }

    /**
     * 切换我的绘画显隐请求体：hidden=true 隐藏（公开列表/详情不展示），false 恢复展示
     */
    public record HiddenRequest(Boolean hidden) {
    }

    private Mono<ServerResponse> setMyHidden(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> request.bodyToMono(HiddenRequest.class)
                        .switchIfEmpty(Mono.just(new HiddenRequest(Boolean.FALSE)))
                        // map 禁止 null 返回值（hidden 字段可为 null），须用 flatMap 承接；
                        // 请求体解析失败按 400 处理，避免静默降级为「恢复公开」
                        .flatMap(body -> drawService.setMineHidden(request.pathVariable("name"),
                                username, Boolean.TRUE.equals(body.hidden())))
                        .onErrorResume(e -> !(e instanceof ResponseStatusException),
                                e -> Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "请求体不合法"))))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    private Mono<ServerResponse> listMyDraws(ServerRequest request) {
        String status = request.queryParam("status").orElse("").trim();
        return currentUsername(request)
                .flatMap(username -> drawService.listMine(username, blankToNull(status),
                        queryPage(request), querySize(request)))
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getMyDraw(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> drawService.getMine(request.pathVariable("name"), username))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw));
    }

    private Mono<ServerResponse> createMyDraw(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> request.bodyToMono(Draw.class)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "绘画数据不能为空")))
                        .flatMap(draw -> Mono.zip(Mono.just(draw), fetchUser(username)))
                        .flatMap(tuple -> drawService.create(tuple.getT1(), tuple.getT2())))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    private Mono<ServerResponse> deleteMyDraw(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> drawService.deleteMine(
                        request.pathVariable("name"), username))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)));
    }

    /**
     * 我的竞猜管理信息（谜底、竞猜记录，仅出题人可见）
     */
    private Mono<ServerResponse> getMyPuzzle(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> drawGuessService.myPuzzle(
                        request.pathVariable("name"), username))
                .flatMap(info -> ServerResponse.ok().bodyValue(info))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    /**
     * 手动揭晓我的竞猜画作（仅出题人，幂等）
     */
    private Mono<ServerResponse> revealMyPuzzle(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> drawGuessService.revealByOwner(
                        request.pathVariable("name"), username))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    /**
     * 当前登录用户名；未认证或匿名身份一律 401（fail-closed）
     */
    private Mono<String> currentUsername(ServerRequest request) {
        return request.principal()
                .map(Principal::getName)
                .filter(name -> !Constants.ANONYMOUS_USER.equals(name))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "请先登录")));
    }

    private Mono<User> fetchUser(String username) {
        return client.fetch(User.class, username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "用户不存在")));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int queryPage(ServerRequest request) {
        return request.queryParam("page").map(Integer::parseInt).orElse(1);
    }

    private static int querySize(ServerRequest request) {
        return request.queryParam("size").map(Integer::parseInt)
                .orElse(Constants.DEFAULT_PAGE_SIZE);
    }
}
