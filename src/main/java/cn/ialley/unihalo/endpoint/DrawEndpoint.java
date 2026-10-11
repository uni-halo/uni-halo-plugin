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
import cn.ialley.unihalo.services.DrawGuessService;
import cn.ialley.unihalo.services.DrawService;
import cn.ialley.unihalo.vo.DrawVo;
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
    private final DrawGuessService drawGuessService;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.DRAW_API_BASE_PATH, this::listDraws)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}", this::getDraw)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}/puzzle", this::getPuzzle)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/approve", this::approveDraw)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/reject", this::rejectDraw)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/reveal", this::revealPuzzle)
                .DELETE(Constants.DRAW_API_BASE_PATH + "/{name}", this::deleteDraw)
                .PUT(Constants.DRAW_API_BASE_PATH + "/{name}/hidden", this::setHidden)
                .build();
    }

    /**
     * 切换显隐请求体：hidden=true 隐藏（公开列表/详情不展示），false 恢复展示
     */
    public record HiddenRequest(Boolean hidden) {
    }

    private Mono<ServerResponse> setHidden(ServerRequest request) {
        return request.bodyToMono(HiddenRequest.class)
                .switchIfEmpty(Mono.just(new HiddenRequest(Boolean.FALSE)))
                // map 禁止 null 返回值（hidden 字段可为 null），须用 flatMap 承接；
                // 请求体解析失败按 400 处理，避免静默降级为「恢复公开」
                .flatMap(body -> drawService.setHidden(request.pathVariable("name"),
                        Boolean.TRUE.equals(body.hidden())))
                .onErrorResume(e -> !(e instanceof ResponseStatusException),
                        e -> Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "请求体不合法")))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    private Mono<ServerResponse> deleteDraw(ServerRequest request) {
        return drawService.deleteManage(request.pathVariable("name"))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)));
    }

    private Mono<ServerResponse> listDraws(ServerRequest request) {
        String status = request.queryParam("status").orElse("").trim();
        String keyword = request.queryParam("keyword").orElse("").trim();
        String sort = request.queryParam("sort").orElse("").trim();
        String type = request.queryParam("type").orElse("").trim();
        String hidden = request.queryParam("hidden").orElse("").trim();
        return drawService.list(blankToNull(status), blankToNull(keyword), blankToNull(sort),
                        blankToNull(type), blankToNull(hidden),
                        queryPage(request), querySize(request))
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getDraw(ServerRequest request) {
        return drawService.getManage(request.pathVariable("name"))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw));
    }

    /**
     * 竞猜管理信息（谜底原文 + 竞猜记录，审核员核对谜底合规性）
     */
    private Mono<ServerResponse> getPuzzle(ServerRequest request) {
        return drawGuessService.managePuzzle(request.pathVariable("name"))
                .flatMap(info -> ServerResponse.ok().bodyValue(info))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    /**
     * 强制揭晓（管理端，处理争议画作，幂等）
     */
    private Mono<ServerResponse> revealPuzzle(ServerRequest request) {
        return currentUsername(request)
                .flatMap(reviewer -> drawGuessService.revealByManager(
                        request.pathVariable("name"), reviewer))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    private Mono<ServerResponse> approveDraw(ServerRequest request) {
        return review(request, ReviewRequest::note, drawService::approve);
    }

    private Mono<ServerResponse> rejectDraw(ServerRequest request) {
        return review(request, ReviewRequest::reason, drawService::reject);
    }

    /**
     * 审核动作公共流程：取当前用户为审核人（匿名一律 401），空请求体/解析失败均视为无备注/原因。
     * 注意 valueOf 可能返回 null（备注/原因选填），故用 flatMap 承接而非 map（map 禁止 null 返回值）。
     */
    private Mono<ServerResponse> review(ServerRequest request,
            java.util.function.Function<ReviewRequest, String> valueOf,
            TriReviewAction action) {
        return currentUsername(request)
                .flatMap(reviewer -> request.bodyToMono(ReviewRequest.class)
                        .defaultIfEmpty(new ReviewRequest(null, null))
                        .onErrorResume(e -> Mono.just(new ReviewRequest(null, null)))
                        .flatMap(body -> action.apply(request.pathVariable("name"),
                                reviewer, blankToNull(valueOf.apply(body)))))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw))
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
        Mono<DrawVo> apply(String name, String reviewer, String value);
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
