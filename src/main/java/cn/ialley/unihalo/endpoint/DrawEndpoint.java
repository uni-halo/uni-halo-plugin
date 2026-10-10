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
import cn.ialley.unihalo.services.DrawService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 绘画作品控制台接口（需登录且持有 draws 管理角色，审核人仅限管理员）。
 *
 * @author 小莫唐尼
 */
@Component
@RequiredArgsConstructor
public class DrawEndpoint implements CustomEndpoint {

    private final DrawService drawService;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.DRAW_API_BASE_PATH, this::listDraws)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}", this::getDraw)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/approve", this::approveDraw)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/reject", this::rejectDraw)
                .build();
    }

    private Mono<ServerResponse> listDraws(ServerRequest request) {
        String status = request.queryParam("status").orElse("").trim();
        String keyword = request.queryParam("keyword").orElse("").trim();
        return drawService.list(blankToNull(status), blankToNull(keyword),
                        queryPage(request), querySize(request))
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getDraw(ServerRequest request) {
        return drawService.getManage(request.pathVariable("name"))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw));
    }

    private Mono<ServerResponse> approveDraw(ServerRequest request) {
        return review(request, ReviewRequest::note, drawService::approve);
    }

    private Mono<ServerResponse> rejectDraw(ServerRequest request) {
        return review(request, ReviewRequest::reason, drawService::reject);
    }

    /**
     * 审核动作公共流程：取当前用户为审核人（匿名一律 401），空请求体视为无备注/原因
     */
    private Mono<ServerResponse> review(ServerRequest request,
            java.util.function.Function<ReviewRequest, String> valueOf,
            TriReviewAction action) {
        return currentUsername(request)
                .flatMap(reviewer -> request.bodyToMono(ReviewRequest.class)
                        .defaultIfEmpty(new ReviewRequest(null, null))
                        .map(valueOf)
                        .flatMap(value -> action.apply(request.pathVariable("name"),
                                reviewer, value)))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message", e.getMessage())));
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

    /**
     * 审核请求体：note 为通过时的备注（选填）；reason 为拒绝原因（拒绝时必填）
     */
    public record ReviewRequest(String note, String reason) {
    }

    /**
     * 审核动作签名（name, reviewer, noteOrReason）
     */
    @FunctionalInterface
    private interface TriReviewAction {
        Mono<Draw> apply(String name, String reviewer, String value);
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
