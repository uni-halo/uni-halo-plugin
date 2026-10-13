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
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 绘画作品公开接口（匿名可访问，仅返回已通过审核的画作）。
 *
 * <p>猜画扩展：guess-board / guess-rank 匿名可访问；guess 作答需登录（PAT）。
 *
 * @author 小莫唐尼
 */
@Component
@RequiredArgsConstructor
public class DrawPublicEndpoint implements CustomEndpoint {

    private final DrawService drawService;
    private final DrawGuessService drawGuessService;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                // 固定路径须先于 /{name} 注册，避免 guess-rank 被当作画作名匹配
                .GET(Constants.DRAW_API_BASE_PATH + "/guess-rank", this::guessRank)
                .GET(Constants.DRAW_API_BASE_PATH, this::listDraws)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}", this::getDraw)
                .GET(Constants.DRAW_API_BASE_PATH + "/{name}/guess-board", this::guessBoard)
                .POST(Constants.DRAW_API_BASE_PATH + "/{name}/guess", this::guess)
                .build();
    }

    private Mono<ServerResponse> listDraws(ServerRequest request) {
        String type = request.queryParam("type").orElse("").trim();
        return drawService.listPublic(queryPage(request), querySize(request),
                        blankToNull(type))
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getDraw(ServerRequest request) {
        return drawService.getPublic(request.pathVariable("name"))
                .flatMap(draw -> ServerResponse.ok().bodyValue(draw));
    }

    /**
     * 提交竞猜作答（需登录；匿名一律 401）
     */
    private Mono<ServerResponse> guess(ServerRequest request) {
        return currentUsername(request)
                .flatMap(username -> request.bodyToMono(GuessRequest.class)
                        .defaultIfEmpty(new GuessRequest(null))
                        .flatMap(body -> drawGuessService.guess(request.pathVariable("name"),
                                username, body.answer())))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    /**
     * 竞猜信息合一（榜单 + 统计；未揭晓不含谜底，猜中者本人可见谜底）
     */
    private Mono<ServerResponse> guessBoard(ServerRequest request) {
        return optionalUsername(request)
                .defaultIfEmpty("")
                .flatMap(username -> drawGuessService.board(request.pathVariable("name"), username))
                .flatMap(board -> ServerResponse.ok().bodyValue(board))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message",
                                        e.getMessage() == null ? "请求不合法" : e.getMessage())));
    }

    /**
     * 猜画积分排行榜（匿名可见，累计猜中画作数）
     */
    private Mono<ServerResponse> guessRank(ServerRequest request) {
        int top = request.queryParam("top").map(Integer::parseInt).orElse(0);
        return drawGuessService.rank(top)
                .flatMap(items -> ServerResponse.ok().bodyValue(items));
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
     * 当前登录用户名；未认证时返回 Mono.empty()（匿名可访问场景）
     */
    private Mono<String> optionalUsername(ServerRequest request) {
        return request.principal()
                .map(Principal::getName)
                .filter(name -> !Constants.ANONYMOUS_USER.equals(name));
    }

    /**
     * 作答请求体
     */
    public record GuessRequest(String answer) {
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
