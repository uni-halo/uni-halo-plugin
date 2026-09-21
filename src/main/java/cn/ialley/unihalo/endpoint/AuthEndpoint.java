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
import cn.ialley.unihalo.vo.RegisterForm;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 移动端登录公开接口（匿名可访问，由 role-template-anonymous.yaml 全量放行）。
 *
 * 登录成功后返回 Halo 原生 PAT（{@code pat_} 前缀），客户端按
 * {@code Authorization: Bearer <token>} 携带即可访问 Halo 原生 API 与本插件接口。
 * 令牌权限由设置页「移动端登录 → 登录权限」决定，且不会超过用户在 Halo 已有的角色。
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
                // 两段式 POST（资源/name）会被 Halo RequestInfoFactory 降级为非资源请求，
                // 导致 RBAC 资源规则失效；统一用 "-" 占位符构成 资源/-/动作 三段式（官方模式）
                .POST(Constants.AUTH_API_BASE_PATH + "/-/login", this::loginByPassword)
                .POST(Constants.AUTH_API_BASE_PATH + "/login/wechat", this::loginByWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/register", this::registerByPassword)
                .POST(Constants.AUTH_API_BASE_PATH + "/register/wechat", this::registerByWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat", this::bindWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets",
                        this::createBindTicket)
                .GET(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}",
                        this::bindTicketStatus)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}/confirm",
                        this::confirmBindTicket)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/logout", this::logout)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/password/set", this::setInitialPassword)
                .GET(Constants.AUTH_API_BASE_PATH + "/profile", this::profile)
                .GET(Constants.AUTH_API_BASE_PATH + "/my/wechat-binding", this::myWechatBinding)
                .DELETE(Constants.AUTH_API_BASE_PATH + "/my/wechat-binding", this::unbindWechat)
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

    /**
     * 账号密码注册并登录（注册即登录）：返回结构与登录接口一致
     * （{@code LoginResult}），APP 端注册后直接进站，无需二次登录。
     */
    private Mono<ServerResponse> registerByPassword(ServerRequest request) {
        var clientIp = clientIpOf(request);
        return request.bodyToMono(RegisterRequest.class)
                .switchIfEmpty(Mono.error(
                        new AuthException("BAD_REQUEST", "缺少请求体")))
                .map(body -> new RegisterForm(body.username(), body.displayName(),
                        body.password(), body.confirmPassword(),
                        body.email(), body.emailCode(), body.agreedToTerms()))
                .flatMap(form -> authService.registerByPassword(form, clientIp))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /** 微信一键注册并登录（已绑定则登录，未绑定自动建号，兼做「注册 + 登录」）。 */
    private Mono<ServerResponse> registerByWechat(ServerRequest request) {
        return request.bodyToMono(WechatLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        new AuthException("BAD_REQUEST", "缺少请求体")))
                .flatMap(body -> authService.registerByWechat(body.code()))
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

    /**
     * UC 侧创建扫码绑定票据：要求登录身份（PAT 或会话），票据创建即锁定该用户名。
     */
    private Mono<ServerResponse> createBindTicket(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> authService.createBindTicket(identity.username()))
                .flatMap(ticket -> ServerResponse.ok().bodyValue(Map.of(
                        "ticket", ticket.ticket(),
                        // 二维码内容带业务前缀，小程序端按前缀识别分发
                        "qrContent", Constants.QR_BIND_WECHAT_PREFIX + ticket.ticket(),
                        "expiresAt", ticket.expiresAt().toString())))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /** UC 侧轮询票据状态（等待扫码 / 已确认 / 已过期）。 */
    private Mono<ServerResponse> bindTicketStatus(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return authService.bindTicketStatus(ticket)
                .flatMap(status -> ServerResponse.ok()
                        .bodyValue(Map.of("ticket", status.ticket(),
                                "status", status.status().name())))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 小程序端确认绑定（匿名调用）：身份由 wx.login() 的 code 换取，
     * 绑定目标用户名在票据创建时已锁定，客户端无法指定。
     */
    private Mono<ServerResponse> confirmBindTicket(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return request.bodyToMono(WechatLoginRequest.class)
                .switchIfEmpty(Mono.error(new AuthException("BAD_REQUEST", "缺少请求体")))
                .flatMap(body -> authService.confirmBindTicket(ticket, body.code()))
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

    /**
     * 首次设置密码（免旧密码）：要求登录身份（PAT 或会话），匿名一律拒绝；
     * 仅「从未自主设置过密码」的用户可用，防滥用见服务层注解判定。
     */
    private Mono<ServerResponse> setInitialPassword(ServerRequest request) {
        return currentIdentity()
                .filter(identity -> !Constants.ANONYMOUS_USER.equals(identity.username()))
                .switchIfEmpty(Mono.error(new AuthException("UNAUTHENTICATED", "未登录")))
                .flatMap(identity -> request.bodyToMono(SetPasswordRequest.class)
                        .switchIfEmpty(Mono.error(
                                new AuthException("BAD_REQUEST", "缺少请求体")))
                        .flatMap(body -> authService.setInitialPassword(
                                identity.username(), body.newPassword())))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> profile(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> authService.profile(identity.username()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /** UC 侧「我的微信绑定」状态（只需登录自身身份，无需管理权限）。 */
    private Mono<ServerResponse> myWechatBinding(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> authService.myWechatBinding(identity.username()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 移动端解除自己的微信绑定（幂等：未绑定时同样返回成功）。
     * 该组 API 虽对匿名放行（role-anonymous 全量），但解绑是写操作，
     * 匿名身份一律拒绝，不做无害空转。
     */
    private Mono<ServerResponse> unbindWechat(ServerRequest request) {
        return currentIdentity()
                .filter(identity -> !Constants.ANONYMOUS_USER.equals(identity.username()))
                .switchIfEmpty(Mono.error(
                        new AuthException("UNAUTHENTICATED", "未登录")))
                .flatMap(identity -> authService.unbindMyWechat(identity.username()))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
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
     * {@link AuthException} 按自带状态码返回（凭据类 401、限流 429）。其余异常一律收敛：
     * 不加这道兜底，Halo 内部异常会以原始 problem detail 漏给客户端 —— 例如
     * {@code {"detail":"User unihalo01 was not found","status":404}}，既暴露内部用户名，
     * 又破坏了本接口 {@code {code,message}} 的契约，客户端也没法按 code 分支。
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

    /** 首次设置密码请求体。 */
    public record SetPasswordRequest(String newPassword) {
    }

    /**
     * 账号密码注册请求体（字段与 {@link RegisterForm} 一致，
     * 收到后原样映射为表单交给服务层，校验逻辑在服务端 fail closed）。
     */
    public record RegisterRequest(String username, String displayName, String password,
            String confirmPassword, String email, String emailCode, Boolean agreedToTerms) {
    }

    private record Identity(String username, String patName) {
    }
}
