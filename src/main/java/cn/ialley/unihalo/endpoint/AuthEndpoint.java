package cn.ialley.unihalo.endpoint;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.exception.AuthException;
import cn.ialley.unihalo.services.AuthService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 移动端登录公开接口（匿名可访问，由 role-anonymous.yaml 全量放行）。
 *
 * <p>登录成功后返回 Halo 原生 PAT（{@code pat_} 前缀），客户端按
 * {@code Authorization: Bearer <token>} 携带即可访问 Halo 原生 API 与本插件接口。
 * 令牌权限由设置页「移动端登录 → 登录权限」决定，且不会超过用户在 Halo 已有的角色。</p>
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
public class AuthEndpoint implements CustomEndpoint {

    private final AuthService authService;

    public AuthEndpoint(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .POST(Constants.AUTH_API_BASE_PATH + "/login", this::loginByPassword)
                .POST(Constants.AUTH_API_BASE_PATH + "/login/wechat", this::loginByWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat", this::bindWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/logout", this::logout)
                .GET(Constants.AUTH_API_BASE_PATH + "/profile", this::profile)
                .build();
    }

    private Mono<ServerResponse> loginByPassword(ServerRequest request) {
        var clientIp = clientIpOf(request);
        return request.bodyToMono(PasswordLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        new AuthException("BAD_REQUEST", "缺少请求体")))
                .flatMap(body -> authService.loginByPassword(
                        body.username(), body.password(), clientIp))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> loginByWechat(ServerRequest request) {
        return request.bodyToMono(WechatLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        new AuthException("BAD_REQUEST", "缺少请求体")))
                .flatMap(body -> authService.loginByWechat(body.code()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> bindWechat(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> request.bodyToMono(WechatLoginRequest.class)
                        .switchIfEmpty(Mono.error(
                                new AuthException("BAD_REQUEST", "缺少请求体")))
                        .flatMap(body -> authService.bindWechat(identity.username(), body.code())))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> logout(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> {
                    if (identity.patName() == null) {
                        return Mono.error(new AuthException("UNAUTHENTICATED", "未登录"));
                    }
                    return authService.logout(identity.patName(), identity.username());
                })
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> profile(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> authService.profile(identity.username()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 从安全上下文取当前身份：用户名为 {@code Authentication#getName}，
     * PAT 名称取自 JWT 的 {@code pat_name} 声明（为空说明不是 PAT 登录）。
     */
    private Mono<Identity> currentIdentity() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    String patName = null;
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        patName = jwt.getToken().getClaimAsString("pat_name");
                    }
                    return new Identity(auth.getName(), patName);
                })
                .switchIfEmpty(Mono.error(
                        new AuthException("UNAUTHENTICATED", "未登录")));
    }

    /**
     * 认证接口的统一错误出口。
     *
     * <p>{@link AuthException} 按自带状态码返回（凭据类 401、限流 429）。<b>其余异常一律收敛</b>：
     * 不加这道兜底，Halo 内部异常会以原始 problem detail 漏给客户端 —— 例如
     * {@code {"detail":"User unihalo01 was not found","status":404}}，既暴露内部用户名，
     * 又破坏了本接口 {@code {code,message}} 的契约，客户端也没法按 code 分支。</p>
     */
    private static Mono<ServerResponse> handleFailure(Throwable e) {
        if (e instanceof AuthException auth) {
            return ServerResponse.status(auth.getStatus())
                    .bodyValue(Map.of("code", auth.getCode(), "message", auth.getMessage()));
        }
        log.warn("【UniHalo】认证接口出现未预期的错误", e);
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(Map.of("code", "INTERNAL_ERROR", "message", "服务异常，请稍后重试"));
    }

    /**
     * 取来源 IP 用于限流。用 TCP 源地址而非 {@code X-Forwarded-For}：
     * 后者由客户端提供、可随意伪造，拿它做限流等于没做。
     */
    private static String clientIpOf(ServerRequest request) {
        return request.remoteAddress()
                .map(address -> address.getAddress() == null
                        ? address.getHostString()
                        : address.getAddress().getHostAddress())
                .orElse(null);
    }

    /** 账号密码登录请求体。 */
    public record PasswordLoginRequest(String username, String password) {
    }

    /** 微信登录请求体。 */
    public record WechatLoginRequest(String code) {
    }

    private record Identity(String username, String patName) {
    }
}
