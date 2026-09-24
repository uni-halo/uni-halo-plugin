package cn.ialley.unihalo.endpoint;

import java.util.Map;
import java.util.regex.Pattern;

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
import cn.ialley.unihalo.captcha.CaptchaScope;
import cn.ialley.unihalo.captcha.CaptchaService;
import cn.ialley.unihalo.captcha.CaptchaValidationException;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.exception.AuthException;
import cn.ialley.unihalo.exception.BizErrorCode;
import cn.ialley.unihalo.services.AuthService;
import cn.ialley.unihalo.utils.RegisterEmailCodeClient;
import cn.ialley.unihalo.utils.RegisterEmailCodeRateLimiter;
import cn.ialley.unihalo.vo.RegisterForm;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.infra.ExternalUrlSupplier;
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

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final AuthService authService;

    private final RegisterEmailCodeClient registerEmailCodeClient;

    private final RegisterEmailCodeRateLimiter emailCodeRateLimiter;

    private final ExternalUrlSupplier externalUrlSupplier;

    private final CaptchaService captchaService;

    public AuthEndpoint(AuthService authService,
            RegisterEmailCodeClient registerEmailCodeClient,
            RegisterEmailCodeRateLimiter emailCodeRateLimiter,
            ExternalUrlSupplier externalUrlSupplier,
            CaptchaService captchaService) {
        this.authService = authService;
        this.registerEmailCodeClient = registerEmailCodeClient;
        this.emailCodeRateLimiter = emailCodeRateLimiter;
        this.externalUrlSupplier = externalUrlSupplier;
        this.captchaService = captchaService;
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
                .POST(Constants.AUTH_API_BASE_PATH + "/-/send-register-email-code",
                        this::sendRegisterEmailCode)
                .POST(Constants.AUTH_API_BASE_PATH + "/register/wechat", this::registerByWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/register/wechat-email",
                        this::registerWechatEmail)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat", this::bindWechat)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets",
                        this::createBindTicket)
                .GET(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}",
                        this::bindTicketStatus)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}/confirm",
                        this::scanBindTicket)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}/approve",
                        this::approveBindTicket)
                .POST(Constants.AUTH_API_BASE_PATH + "/bind/wechat/qr/tickets/{ticket}/reject",
                        this::rejectBindTicket)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/logout", this::logout)
                .POST(Constants.AUTH_API_BASE_PATH + "/-/password/set", this::setInitialPassword)
                .GET(Constants.AUTH_API_BASE_PATH + "/profile", this::profile)
                .GET(Constants.AUTH_API_BASE_PATH + "/token-check", this::tokenCheck)
                .GET(Constants.AUTH_API_BASE_PATH + "/my/wechat-binding", this::myWechatBinding)
                .DELETE(Constants.AUTH_API_BASE_PATH + "/my/wechat-binding", this::unbindWechat)
                .build();
    }

    private Mono<ServerResponse> loginByPassword(ServerRequest request) {
        var clientIp = clientIpOf(request);
        return request.bodyToMono(PasswordLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                .flatMap(body -> authService.loginByPassword(
                        body.username(), body.password(), clientIp))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> loginByWechat(ServerRequest request) {
        return request.bodyToMono(WechatLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
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
                        BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                .map(body -> new RegisterForm(body.username(), body.displayName(),
                        body.password(), body.confirmPassword(),
                        body.email(), body.emailCode(), body.agreedToTerms()))
                .flatMap(form -> authService.registerByPassword(form, clientIp))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 发送注册邮箱验证码：Halo 匿名端点在站点根下，CSRF 不豁免，小程序无法携带
     * CSRF token 会 403，故经插件服务端完成 CSRF 握手后代发（保持 202 契约）。
     */
    private Mono<ServerResponse> sendRegisterEmailCode(ServerRequest request) {
        var clientIp = clientIpOf(request);
        if (clientIp == null || clientIp.isBlank()) {
            return Mono.error(BizErrorCode.BAD_REQUEST.toException("无法识别请求来源"));
        }
        var baseUrl = externalUrlSupplier.getURL(request.exchange().getRequest()).toString();
        return captchaService.requireValid(request, CaptchaScope.REGISTER_EMAIL_CODE)
                .then(request.bodyToMono(SendEmailCodeRequest.class)
                        .switchIfEmpty(Mono.error(
                                BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                        .map(body -> body.email() == null ? "" : body.email().trim())
                        .filter(email -> EMAIL_PATTERN.matcher(email).matches())
                        .switchIfEmpty(Mono.error(
                                BizErrorCode.BAD_REQUEST.toException("请填写正确的邮箱")))
                        .flatMap(email -> emailCodeRateLimiter
                                .check(clientIp, email.toLowerCase())
                                .thenReturn(email))
                        .flatMap(email -> registerEmailCodeClient
                                .send(baseUrl, email, clientIp)))
                .then(ServerResponse.accepted().build())
                .onErrorResume(CaptchaValidationException.class, this::captchaForbidden)
                .onErrorMap(RegisterEmailCodeRateLimiter.QuotaExceededException.class,
                        e -> BizErrorCode.TOO_MANY_ATTEMPTS.toException())
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /** 验证码校验失败：403 返回提示与一枚新验证码（App 端展示后随下次请求携带）。 */
    private Mono<ServerResponse> captchaForbidden(CaptchaValidationException e) {
        return captchaService.generate()
                .flatMap(captcha -> ServerResponse.status(HttpStatus.FORBIDDEN)
                        .bodyValue(Map.of("message", e.getMessage(), "captcha", captcha)));
    }

    /** 微信一键注册（已绑定则登录，未绑定自动建号）。 */
    private Mono<ServerResponse> registerByWechat(ServerRequest request) {
        return request.bodyToMono(WechatLoginRequest.class)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                .flatMap(body -> authService.registerByWechat(body.code()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 微信补邮箱注册（第二段）：站点开启注册邮箱验证时，一键注册被
     * {@code WECHAT_EMAIL_REQUIRED} 拦下并下发票据，客户端补齐邮箱与验证码后
     * 凭票据在此完成注册，返回结构与登录接口一致（{@code LoginResult}）。
     */
    private Mono<ServerResponse> registerWechatEmail(ServerRequest request) {
        return request.bodyToMono(WechatEmailRegisterRequest.class)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                .flatMap(body -> authService.registerByWechatEmail(
                        body.ticket(), body.email(), body.emailCode(), body.code()))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> bindWechat(ServerRequest request) {
        return currentUser()
                .flatMap(identity -> request.bodyToMono(WechatLoginRequest.class)
                        .switchIfEmpty(Mono.error(
                                BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                        .flatMap(body -> authService.bindWechat(identity.username(), body.code())))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * UC 侧创建扫码绑定票据：要求登录身份（PAT 或会话），票据创建即锁定该用户名。
     */
    private Mono<ServerResponse> createBindTicket(ServerRequest request) {
        return currentUser()
                .flatMap(identity -> authService.createBindTicket(identity.username()))
                .flatMap(ticket -> ServerResponse.ok().bodyValue(Map.of(
                        "ticket", ticket.ticket(),
                        // 二维码内容带业务前缀，小程序端按前缀识别分发
                        "qrContent", Constants.QR_BIND_WECHAT_PREFIX + ticket.ticket(),
                        "expiresAt", ticket.expiresAt().toString())))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 轮询票据状态（匿名可调），供 UC 与小程序两端共用。
     *
     * <p>响应中的 {@code mine} 是给小程序端的<b>登录态预检</b>：带了有效令牌时返回
     * 「当前登录账号是否就是票据归属」（true/false）；匿名调用恒为 null（无登录态可比，
     * 匿名扫码本来就是合法主流程）。只回答是不是「你的票」，不回显归属用户名——
     * 该接口匿名可查，返回归属名等于凭一张二维码照片探测站内用户名。
     *
     * <p>预检结果仅供客户端提前给出失败提示，<b>不是</b>安全边界：
     * 真正的归属裁决始终在 confirm（{@link #scanBindTicket}）时由服务端完成。
     */
    private Mono<ServerResponse> bindTicketStatus(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return authService.bindTicketStatus(ticket)
                .flatMap(status -> currentIdentityOrNull()
                        .flatMap(identity -> {
                            var visitor = identity.username();
                            var anonymous = visitor == null
                                    || Constants.ANONYMOUS_USER.equals(visitor);
                            var body = new java.util.HashMap<String, Object>();
                            body.put("ticket", status.ticket());
                            body.put("status", status.status().name());
                            // 失败原因（仅 FAILED 有值），让 UC 弹窗能给出可执行提示，
                            // 而不是让用户干等到二维码过期
                            body.put("reason", status.reason() == null ? "" : status.reason());
                            // 扫码方微信标识的脱敏尾号（仅 SCANNED 有值），
                            // 供 PC 端核对「是谁在扫」，不给完整 openid
                            body.put("hint", status.hint() == null ? "" : status.hint());
                            if (anonymous) {
                                body.put("mine", null);
                                return ServerResponse.ok().bodyValue(body);
                            }
                            return authService.bindTicketOwner(ticket)
                                    .map(owner -> owner != null && owner.equals(visitor))
                                    .defaultIfEmpty(Boolean.FALSE)
                                    .flatMap(mine -> {
                                        body.put("mine", mine);
                                        return ServerResponse.ok().bodyValue(body);
                                    });
                        }))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * 小程序端扫码（匿名可调）：身份由 wx.login() 的 code 换取，绑定目标用户名在票据
     * 创建时已锁定，客户端无法指定。
     *
     * <p>只登记微信身份，<b>不建立绑定</b>，等 PC 端 {@link #approveBindTicket} 确认。
     * 若小程序端已登录另一个账号，服务端直接拒绝（见
     * {@link cn.ialley.unihalo.exception.BizErrorCode#BIND_SIGNED_IN_OTHER_ACCOUNT}）——
     * 这一点要求客户端带上登录态，未登录时保持匿名调用不变。
     */
    private Mono<ServerResponse> scanBindTicket(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return currentIdentityOrNull()
                .flatMap(identity -> request.bodyToMono(WechatLoginRequest.class)
                        .switchIfEmpty(Mono.error(
                                BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
                        .flatMap(body -> authService.scanBindTicket(ticket, body.code(),
                                identity.username())))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /**
     * PC 端确认绑定：必须由票据归属者本人调用（匿名一律拒绝）。
     *
     * <p>这是两阶段模型的第二枪 —— 票据泄露最多让攻击者「扫个码」，
     * 没有本人这一步确认绑不上。
     */
    private Mono<ServerResponse> approveBindTicket(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return currentUser()
                .flatMap(identity -> authService.approveBindTicket(ticket, identity.username()))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    /** PC 端拒绝本次扫码（用户在弹窗里点「取消」）：票据落到 FAILED 并回传原因。 */
    private Mono<ServerResponse> rejectBindTicket(ServerRequest request) {
        var ticket = request.pathVariable("ticket");
        return currentUser()
                .flatMap(identity -> authService.rejectBindTicket(ticket, identity.username()))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(AuthEndpoint::handleFailure);
    }

    private Mono<ServerResponse> logout(ServerRequest request) {
        return currentIdentity()
                .flatMap(identity -> {
                    if (identity.patName() == null) {
                        return Mono.error(BizErrorCode.UNAUTHENTICATED.toException());
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
        return currentUser()
                .flatMap(identity -> request.bodyToMono(SetPasswordRequest.class)
                        .switchIfEmpty(Mono.error(
                                BizErrorCode.BAD_REQUEST.toException("缺少请求体")))
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

    /**
     * 令牌探活（App 端启动 / 回前台时调用）：200 = 有效，401 = 已失效
     * （exp 过期 / 被新设备互踢 / 已吊销）。
     *
     * <p>无效令牌在认证链上就不成立，请求会以匿名身份到达本端点，
     * 由 {@link #currentUser()} 统一拒绝并收敛为 401 —— 端点本身无需
     * 判别失效原因；能进入服务层的都是「认证链认为有效」的令牌，
     * 服务层只做 revoked / expiresAt 的显式兜底（fail closed）。
     *
     * <p>补充说明 401 与 403 的分工：与本插件业务接口不同，本端点对
     * 「有效但权限不足」的概念不敏感——探活只回答「令牌还活着吗」，
     * 权限问题留给各业务接口自行裁决，客户端不必在这里区分二者。
     */
    private Mono<ServerResponse> tokenCheck(ServerRequest request) {
        return currentUser()
                .flatMap(identity ->
                        authService.tokenCheck(identity.username(), identity.patName()))
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
        return currentUser()
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
                .switchIfEmpty(Mono.error(BizErrorCode.UNAUTHENTICATED.toException()));
    }

    /**
     * 取当前登录身份；无令牌/无上下文时返回 {@code username=null} 的占位，不报错。
     *
     * <p>用于「允许匿名但需要感知来访身份」的端点（如扫码 confirm）：
     * 匿名就是 {@code anonymousUser}，带了令牌就能看出他是不是登录着别的账号。
     */
    private Mono<Identity> currentIdentityOrNull() {
        return currentIdentity().defaultIfEmpty(new Identity(null, null));
    }

    /**
     * 取当前登录身份，并<b>显式拒绝匿名</b>（fail-closed）。
     *
     * 该组 API 对匿名全量放行（role-anonymous 授予 {@code resources:* verbs:*}），
     * 匿名请求的 {@code Authentication#getName} 是 {@code anonymousUser}。
     * 写操作端点若不在这一层拦住，匿名调用就会以 anonymousUser 身份落数据 ——
     * 典型后果：把他人已绑定的微信关系改绑到 anonymousUser，导致对方微信登录失效。
     * 因此绑定类写接口一律走这里，而不是 {@link #currentIdentity()}。
     */
    private Mono<Identity> currentUser() {
        return currentIdentity()
                .filter(identity -> !Constants.ANONYMOUS_USER.equals(identity.username()))
                .switchIfEmpty(Mono.error(BizErrorCode.UNAUTHENTICATED.toException()));
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
            var body = new java.util.HashMap<String, Object>();
            body.put("code", auth.getCode());
            body.put("message", auth.getMessage());
            // 附加数据（如 WECHAT_EMAIL_REQUIRED 的注册票据）仅在存在时下发
            if (auth.getData() != null && !auth.getData().isEmpty()) {
                body.put("data", auth.getData());
            }
            return ServerResponse.status(auth.getStatus()).bodyValue(body);
        }
        log.warn("【UniHalo】认证接口出现未预期的错误", e);
        var fallback = BizErrorCode.INTERNAL_ERROR;
        return ServerResponse.status(fallback.getStatus())
                .bodyValue(Map.of("code", fallback.getCode(), "message", fallback.getMessage()));
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

    /**
     * 微信补邮箱注册请求体（{@code ticket/email/emailCode} 来自补邮箱弹层，
     * {@code code} 为客户端重新获取的 wx.login 凭证，用于服务端二次校验微信身份）。
     */
    public record WechatEmailRegisterRequest(String ticket, String email, String emailCode,
            String code) {
    }

    /** 首次设置密码请求体。 */
    public record SetPasswordRequest(String newPassword) {
    }

    /** 账号密码注册请求体（字段与 {@link RegisterForm} 一致，校验在服务层）。 */
    public record RegisterRequest(String username, String displayName, String password,
            String confirmPassword, String email, String emailCode, Boolean agreedToTerms) {
    }

    /** 发送注册邮箱验证码请求体（邮箱格式由官方端点 @Email 校验兜底）。 */
    public record SendEmailCodeRequest(String email) {
    }

    private record Identity(String username, String patName) {
    }
}
