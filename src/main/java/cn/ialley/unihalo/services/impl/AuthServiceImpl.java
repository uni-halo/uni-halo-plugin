package cn.ialley.unihalo.services.impl;

import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.exception.AuthException;
import cn.ialley.unihalo.exception.BizErrorCode;
import cn.ialley.unihalo.services.AuthService;
import cn.ialley.unihalo.services.BindTicketService;
import cn.ialley.unihalo.services.PatIssuer;
import cn.ialley.unihalo.services.WechatService;
import cn.ialley.unihalo.utils.LoginAttemptGuard;
import cn.ialley.unihalo.utils.LoginConfigResolver;
import cn.ialley.unihalo.utils.NotificationHelper;
import cn.ialley.unihalo.utils.UserConnectionSupport;
import cn.ialley.unihalo.utils.WechatRegisterTicketManager;
import cn.ialley.unihalo.vo.LoginConfig;
import cn.ialley.unihalo.vo.LoginResult;
import cn.ialley.unihalo.vo.ProfileVo;
import cn.ialley.unihalo.vo.RegisterForm;
import cn.ialley.unihalo.vo.TokenCheckVo;
import cn.ialley.unihalo.vo.WechatBindingVo;
import run.halo.app.extension.ConfigMap;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.UserConnection;
import run.halo.app.core.extension.Role;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.core.user.service.SignUpData;
import run.halo.app.core.user.service.UserService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.SystemSetting;
import run.halo.app.security.PersonalAccessToken;
import tools.jackson.databind.ObjectMapper;

/**
 * 移动端登录实现。三种登录方式（密码 / 微信 / 已登录绑定微信）最终都收敛到
 * {@link #issueFor(User, LoginConfig)}：以用户在 Halo 已有的角色签发一枚 Halo 原生 PAT，
 * 并把角色模板展开成 RBAC 规则一并返回。注册开关与默认角色沿用 Halo 系统设置。
 *
 * 安全约束：① 防提权 —— intersect 模式下只授予「配置角色 ∩ 用户已有角色」
 * （与 Halo 官方 {@code PatServiceImpl#hasSufficientRoles} 同语义）；② 开启 2FA 的账号
 * 拒绝密码登录，不绕过二次验证。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 用户名序号格式：至少两位，超过 99 自然升到三位（unihalo100）。 */
    private static final String SEQUENCE_FORMAT = "%02d";

    /** 自动注册时最多连续尝试多少个序号（并发抢号 + 已存在时的兜底次数）。 */
    private static final int MAX_USERNAME_ATTEMPTS = 5;

    /** PC 端主动取消扫码时回传给用户的原因文案（票据落到 FAILED，UC 弹窗据此提示）。 */
    private static final String REJECT_REASON = "你已取消本次绑定，请重新生成二维码";

    /**
     * 注册失败中「用户可修复」的错误码（计入限流）：
     * 用户名重复/受限、验证码无效、邮箱被占、未同意协议。
     * 配置类错误（站点未开放注册、默认角色未配置）不计 —— 那是站长侧问题，
     * 客户端重试无意义，计入限流只会把配置问题变成 429 迷惑用户。
     */
    private static final Set<String> ACCOUNTABLE_REGISTER_CODES = Set.of(
            BizErrorCode.USERNAME_EXISTS.getCode(),
            BizErrorCode.NAME_RESTRICTED.getCode(),
            BizErrorCode.EMAIL_CODE_INVALID.getCode(),
            BizErrorCode.EMAIL_ALREADY_TAKEN.getCode(),
            BizErrorCode.AGREEMENT_REQUIRED.getCode());

    private final UserService userService;
    private final RoleService roleService;
    private final PatIssuer patIssuer;
    private final LoginConfigResolver configResolver;
    private final WechatService wechatService;
    private final ReactiveExtensionClient client;
    private final LoginAttemptGuard attemptGuard;
    private final BindTicketService bindTicketService;
    private final NotificationHelper notificationHelper;
    private final WechatRegisterTicketManager registerTicketManager;

    /** 系统设置 JSON 解析（ConfigMap system → user 组，与 FeatureConfigServiceImpl 同源）。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<LoginResult> loginByPassword(String username, String rawPassword, String clientIp) {
        return configResolver.config()
                .filter(LoginConfig::passwordLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.PASSWORD_LOGIN_DISABLED.toException()))
                .flatMap(config -> requireNotLocked(username, clientIp)
                        .then(Mono.defer(() -> authenticate(username, rawPassword)
                                .doOnSuccess(user -> attemptGuard.resetUsername(username))
                                .doOnError(AuthException.class,
                                        e -> attemptGuard.recordFailure(username, clientIp))))
                        .flatMap(user -> issueFor(user, config)));
    }

    @Override
    public Mono<LoginResult> loginByWechat(String code) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.WECHAT_LOGIN_DISABLED.toException()))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .flatMap(credential -> wechatService.code2Session(
                                credential.appId(), credential.appSecret(), code))
                        .flatMap(session -> resolveWechatUser(session, config))
                        .onErrorMap(e -> !(e instanceof AuthException),
                                this::unexpectedWechatFailure));
    }

    @Override
    public Mono<LoginResult> registerByPassword(RegisterForm form, String clientIp) {
        return Mono.defer(() -> {
            // 基础项自行把关；细粒度格式校验由 Halo signUp 完成，经 mapSignUpFailure 映射中文。
            if (form == null || isBlank(form.username()) || isBlank(form.displayName())
                    || isBlank(form.password())) {
                return Mono.error(BizErrorCode.BAD_REQUEST
                        .toException("请填写完整的注册信息"));
            }
            if (!form.password().equals(form.confirmPassword())) {
                return Mono.error(BizErrorCode.BAD_REQUEST
                        .toException("两次输入的密码不一致"));
            }
            return requireNotLocked(form.username(), clientIp)
                    .then(configResolver.config())
                    .flatMap(config -> mustVerifyEmailOnRegistration()
                            .flatMap(emailVerifyRequired -> {
                                var data = toSignUpData(form, emailVerifyRequired);
                                return userService.signUp(data)
                                        .doOnSuccess(user -> attemptGuard
                                                .resetUsername(form.username()))
                                        // 只把「用户可修复」的错误计入限流（与登录闸门共用）
                                        .onErrorMap(this::mapSignUpFailure)
                                        .doOnError(AuthException.class,
                                                e -> recordRegisterFailure(form.username(), clientIp, e))
                                        // 注册成功发欢迎通知；失败不阻断注册
                                        .doOnSuccess(user -> notificationHelper
                                                .emitUserRegistered(form.username(),
                                                        form.displayName().trim(), null,
                                                        data.getEmail())
                                                .subscribe());
                            })
                            .flatMap(user -> issueFor(user, config)));
        });
    }

    @Override
    public Mono<LoginResult> registerByWechat(String code) {
        // 与微信登录同源：已绑定即登录，未绑定自动建号
        return loginByWechat(code);
    }

    @Override
    public Mono<LoginResult> registerByWechatEmail(String ticket, String email, String emailCode,
            String code) {
        var verified = registerTicketManager.verify(ticket);
        if (verified == null) {
            return Mono.error(BizErrorCode.BAD_REQUEST
                    .toException("注册会话已失效，请重新点击微信一键注册"));
        }
        if (isBlank(email) || isBlank(emailCode) || isBlank(code)) {
            return Mono.error(BizErrorCode.BAD_REQUEST
                    .toException("请填写邮箱与验证码"));
        }
        // 绑定身份与静默注册口径一致：unionid 优先（票据签发时已按此原则锁定）
        var identity = verified.unionid() == null || verified.unionid().isBlank()
                ? verified.openid() : verified.unionid();
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.WECHAT_LOGIN_DISABLED.toException()))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .flatMap(credential -> wechatService.code2Session(
                                credential.appId(), credential.appSecret(), code))
                        // 用新提交的 wx.login code 二次换取微信身份，与票据比对：
                        // 票据是「这个微信想注册」，二次校验确认「此刻操作的就是这个微信」，
                        // 票据被截获也无法用别的微信冒名注册
                        .filter(session -> verified.openid().equals(session.openid()))
                        .switchIfEmpty(Mono.error(BizErrorCode.BAD_REQUEST
                                .toException("微信身份校验失败，请重新操作")))
                        .flatMap(session -> registerWechatUser(config, identity,
                                email.trim(), emailCode.trim())
                                // signUp 失败映射中文（验证码无效/邮箱被占/协议未同意等）
                                .onErrorMap(this::mapSignUpFailure)
                                .flatMap(user -> connect(user.getMetadata().getName(), identity)
                                        .thenReturn(user))
                                .flatMap(user -> issueFor(user, config))))
                .onErrorMap(e -> !(e instanceof AuthException),
                        this::unexpectedWechatFailure);
    }

    @Override
    public Mono<Void> bindWechat(String username, String code) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.WECHAT_LOGIN_DISABLED.toException()))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .flatMap(credential -> wechatService.code2Session(
                                credential.appId(), credential.appSecret(), code)))
                .flatMap(session -> connect(username, identity(session)))
                .onErrorMap(e -> !(e instanceof AuthException),
                        this::unexpectedWechatFailure)
                // 绑定成功发通知（失败不阻断主流程，沿用通知侧 onErrorResume 口径）
                .then(Mono.defer(() -> notificationHelper.emitWechatBound(
                        username, NotificationHelper.BIND_WAY_APP)));
    }

    @Override
    public Mono<Void> logout(String patName, String username) {
        return patIssuer.revoke(patName, username);
    }

    @Override
    public Mono<TokenCheckVo> tokenCheck(String username, String patName) {
        if (patName == null) {
            // 非令牌登录（如浏览器会话 Cookie）：认证链放行即有效，
            // 没有 PAT 扩展可查，也就没有令牌元数据可回传
            return Mono.just(new TokenCheckVo(true, username, null, null));
        }
        return client.fetch(PersonalAccessToken.class, patName)
                .switchIfEmpty(Mono.error(BizErrorCode.UNAUTHENTICATED.toException()))
                .flatMap(pat -> {
                    if (!username.equals(pat.getSpec().getUsername())) {
                        return Mono.error(BizErrorCode.UNAUTHENTICATED.toException());
                    }
                    if (pat.getSpec().isRevoked()) {
                        return Mono.error(
                                BizErrorCode.UNAUTHENTICATED.toException("令牌已吊销"));
                    }
                    var expiresAt = pat.getSpec().getExpiresAt();
                    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
                        return Mono.error(
                                BizErrorCode.UNAUTHENTICATED.toException("令牌已过期"));
                    }
                    return Mono.just(new TokenCheckVo(true, username, patName, expiresAt));
                });
    }

    @Override
    public Mono<BindTicketService.IssuedTicket> createBindTicket(String username) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.WECHAT_LOGIN_DISABLED.toException()))
                .then(bindTicketService.issue(username));
    }

    @Override
    public Mono<BindTicketService.TicketStatus> bindTicketStatus(String ticket) {
        return bindTicketService.status(ticket);
    }

    @Override
    public Mono<String> bindTicketOwner(String ticket) {
        return bindTicketService.owner(ticket);
    }

    @Override
    public Mono<Void> scanBindTicket(String ticket, String code, String visitor) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        BizErrorCode.WECHAT_LOGIN_DISABLED.toException()))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .onErrorResume(e -> Mono.error(e instanceof AuthException ? e
                                : BizErrorCode.WECHAT_NOT_CONFIGURED.toException()))
                        .flatMap(credential -> bindTicketService.owner(ticket)
                                .switchIfEmpty(Mono.error(BizErrorCode.BIND_TICKET_INVALID
                                        .toException("二维码已失效，请重新生成")))
                                // 登录态校验必须早于换票：否则「手机登录着 B 却扫了 A 的码」
                                // 会先把二维码作废，用户退出登录还得回电脑重新生成
                                .flatMap(owner -> rejectIfSignedInAsOther(owner, visitor))
                                .flatMap(owner -> wechatService.code2Session(
                                                credential.appId(), credential.appSecret(), code)
                                        .flatMap(session -> bindTicketService.scan(ticket,
                                                identity(session)))
                                        .flatMap(scanned -> scanned.success()
                                                ? Mono.<Void>empty()
                                                : Mono.error(BizErrorCode.BIND_TICKET_INVALID
                                                        .toException(scanned.reason())))
                                        .onErrorMap(e -> !(e instanceof AuthException),
                                                this::unexpectedWechatFailure))))
                .then();
    }

    /**
     * 手机端已登录另一个账号时拒绝扫码。
     *
     * <p>扫码绑定的语义是「给<b>发起二维码的那个账号</b>绑上你手机上的微信」，
     * 不是「给你当前登录的账号绑」。手机号登录着 B 却扫了 A 的码，用户几乎一定是
     * 想给 B 绑 —— 放行只会把微信静默绑给 A（一对一约束管不了：微信本来就是空闲的）。
     * 正确出口是引导他去 App 内「我的 - 个人资料」直接绑定。
     */
    private Mono<String> rejectIfSignedInAsOther(String owner, String visitor) {
        boolean anonymous = visitor == null || Constants.ANONYMOUS_USER.equals(visitor);
        if (anonymous || owner.equals(visitor)) {
            return Mono.just(owner);
        }
        log.warn("【UniHalo】扫码绑定被拒绝（手机端已登录其他账号）：票据归属 {}，来访身份 {}",
                owner, visitor);
        return Mono.error(BizErrorCode.BIND_SIGNED_IN_OTHER_ACCOUNT.toException());
    }

    @Override
    public Mono<Void> approveBindTicket(String ticket, String username) {
        return bindTicketService.approve(ticket, username)
                .flatMap(approved -> {
                    if (!approved.success()) {
                        return Mono.error(BizErrorCode.BIND_TICKET_INVALID
                                .toException(approved.reason()));
                    }
                    return connect(username, approved.identity())
                            .onErrorMap(e -> !(e instanceof AuthException),
                                    this::unexpectedWechatFailure)
                            // 成功：落 CONFIRMED 终态 + 发绑定通知
                            .flatMap(connection -> bindTicketService.confirm(ticket)
                                    .then(notificationHelper.emitWechatBound(username,
                                            NotificationHelper.BIND_WAY_SCAN))
                                    .thenReturn(connection))
                            // 失败：落 FAILED 终态（含原因）+ 记插件日志
                            .onErrorResume(e -> settleBindFailure(ticket, username, e)
                                    .then(Mono.<UserConnection>error(e)));
                })
                .then();
    }

    @Override
    public Mono<Void> rejectBindTicket(String ticket, String username) {
        return bindTicketService.reject(ticket, username, REJECT_REASON).then();
    }

    /**
     * 扫码绑定失败收尾：把票据落到 {@code FAILED} 并回传原因文案，同时记插件日志。
     *
     * <p>票据消费与绑定是两步，若不落终态，UC 轮询只会看到「已消费」并误报成功
     * （历史缺陷）。日志侧记录目标账号与业务错误码，供站长定位「用户绑不上」类问题；
     * 微信 code 属一次性凭据，不入日志。
     */
    private Mono<Void> settleBindFailure(String ticket, String username, Throwable e) {
        if (!(e instanceof AuthException auth)) {
            log.warn("【UniHalo】扫码绑定失败（未预期错误）：ticket={}, 目标账号={}",
                    ticket, username, e);
            return bindTicketService.fail(ticket,
                    BizErrorCode.INTERNAL_ERROR.getMessage());
        }
        log.warn("【UniHalo】扫码绑定失败：ticket={}, 目标账号={}, code={}, message={}",
                ticket, username, auth.getCode(), auth.getMessage());
        return bindTicketService.fail(ticket, auth.getMessage());
    }

    @Override
    public Mono<ProfileVo> profile(String username) {
        return configResolver.config()
                .flatMap(config -> userService.getUser(username)
                        .flatMap(this::requireEnabledUser)
                        .flatMap(user -> resolveRoles(username)
                                .flatMap(roles -> permissions(roles)
                                        .map(rules -> new ProfileVo(
                                                loginUserOf(user), roles, rules)))));
    }

    @Override
    public Mono<WechatBindingVo> myWechatBinding(String username) {
        return UserConnectionSupport.newest(connectionsOf(username))
                // 本人视角同样只给脱敏值：本人在页面上也不需要完整 openid
                .map(connection -> WechatBindingVo.masked(
                        username,
                        connection.getSpec().getProviderUserId(),
                        connection.getSpec().getUpdatedAt()))
                .defaultIfEmpty(WechatBindingVo.unbound(username));
    }

    @Override
    public Mono<Void> setInitialPassword(String username, String newPassword) {
        return client.fetch(User.class, username)
                .switchIfEmpty(Mono.error(BizErrorCode.USER_NOT_FOUND.toException()))
                .flatMap(user -> {
                    var annotations = user.getMetadata() == null
                            ? null : user.getMetadata().getAnnotations();
                    if (annotations != null
                            && Boolean.parseBoolean(annotations.get(
                                    Constants.PASSWORD_SET_BY_USER_ANNOTATION))) {
                        // 已自主设置过密码：免旧密码通道关闭，防止登录态泄露后被直接换密
                        return Mono.error(BizErrorCode.PASSWORD_ALREADY_SET.toException());
                    }
                    if (newPassword == null || newPassword.length() < Constants.PASSWORD_MIN_LENGTH) {
                        return Mono.error(BizErrorCode.BAD_REQUEST.toException(
                                "密码长度至少 " + Constants.PASSWORD_MIN_LENGTH + " 位"));
                    }
                    return userService.updateWithRawPassword(username, newPassword)
                            .then(Mono.defer(() -> markPasswordSet(username)))
                            // 设密成功后发确认通知（通知失败不阻断主流程）
                            .then(notificationHelper.emitPasswordSet(username));
                });
    }

    /** 在用户注解上标记「已自主设置过密码」，幂等。 */
    private Mono<Void> markPasswordSet(String username) {
        return client.fetch(User.class, username)
                .flatMap(user -> {
                    var metadata = user.getMetadata();
                    if (metadata == null) {
                        return Mono.empty();
                    }
                    var annotations = metadata.getAnnotations();
                    if (annotations == null) {
                        annotations = new java.util.HashMap<>();
                        metadata.setAnnotations(annotations);
                    }
                    annotations.put(Constants.PASSWORD_SET_BY_USER_ANNOTATION, "true");
                    return client.update(user).then();
                });
    }

    /**
     * 解除自己的微信绑定（幂等：未绑定时同样返回成功，但不发通知）。
     *
     * <p>删除<b>全部</b>绑定记录而非只删第一条：历史重复数据可能给同一账号留下多条
     * UserConnection，只删一条会出现「已解绑但仍能微信登录」。
     */
    @Override
    public Mono<Void> unbindMyWechat(String username) {
        return connectionsOf(username)
                .collectList()
                .flatMap(connections -> {
                    if (connections.isEmpty()) {
                        // 幂等：本来就未绑定，不产生无意义的通知
                        return Mono.<Void>empty();
                    }
                    return Flux.fromIterable(connections)
                            .flatMap(client::delete)
                            .then(Mono.defer(() -> notificationHelper.emitWechatUnbound(
                                    username, NotificationHelper.OPERATOR_SELF)));
                });
    }

    // ---------- 内部实现 ----------

    /**
     * 限流闸门：用户名维度或来源 IP 维度任一处于锁定窗口内即拒绝。
     *
     * 用 429 而非 401 —— 凭据错与太频繁是两种语义，客户端应当区分：前者不该重试，
     * 后者应当退避后再试。
     */
    private Mono<Void> requireNotLocked(String username, String clientIp) {
        var remaining = attemptGuard.usernameLockRemaining(username);
        if (remaining == null) {
            remaining = attemptGuard.ipLockRemaining(clientIp);
        }
        if (remaining == null) {
            return Mono.empty();
        }
        long minutes = Math.max(1, remaining.toMinutes() + 1);
        return Mono.error(BizErrorCode.TOO_MANY_ATTEMPTS.toException(
                "登录尝试次数过多，请 %d 分钟后再试".formatted(minutes)));
    }

    private Mono<User> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null
                || rawPassword.isBlank()) {
            return Mono.error(BizErrorCode.BAD_CREDENTIALS
                    .toException("请输入用户名和密码"));
        }
        return userService.getUser(username)
                .onErrorMap(e -> BizErrorCode.BAD_CREDENTIALS.toException())
                .flatMap(user -> {
                    if (Boolean.TRUE.equals(user.getSpec().getDisabled())) {
                        return Mono.error(BizErrorCode.USER_DISABLED.toException());
                    }
                    if (Boolean.TRUE.equals(user.getSpec().getTwoFactorAuthEnabled())) {
                        return Mono.error(BizErrorCode.TWO_FACTOR_REQUIRED.toException());
                    }
                    return userService.confirmPassword(username, rawPassword)
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(Mono.error(
                                    BizErrorCode.BAD_CREDENTIALS.toException()))
                            .thenReturn(user);
                });
    }

    /**
     * 表单 → Halo 注册数据（emailCode/agreedToTerms 原样透传，由 signUp 校验）。
     * 邮箱：用户填写优先；未填写且站点未强制邮箱验证时兜底 {@code 用户名@example.com}。
     */
    private SignUpData toSignUpData(RegisterForm form, boolean emailVerifyRequired) {
        var data = new SignUpData();
        data.setUsername(form.username().trim());
        data.setDisplayName(form.displayName().trim());
        data.setPassword(form.password());
        data.setConfirmPassword(form.confirmPassword());
        data.setEmail(isBlank(form.email())
                ? (emailVerifyRequired ? null : defaultEmail(form.username().trim()))
                : form.email().trim());
        data.setEmailCode(isBlank(form.emailCode()) ? null : form.emailCode().trim());
        data.setAgreedToTerms(Boolean.TRUE.equals(form.agreedToTerms()));
        return data;
    }

    /**
     * 站点是否要求注册时验证邮箱（系统设置 user.mustVerifyEmailOnRegistration）。
     * 读取失败按未开启处理（signUp 内部对邮箱/验证码仍会校验）。
     */
    private Mono<Boolean> mustVerifyEmailOnRegistration() {
        return client.fetch(ConfigMap.class, SystemSetting.SYSTEM_CONFIG)
                .map(cm -> {
                    var raw = cm.getData() == null
                            ? null : cm.getData().get(SystemSetting.User.GROUP);
                    if (raw == null || raw.isBlank()) {
                        return false;
                    }
                    try {
                        return objectMapper.readTree(raw)
                                .path("mustVerifyEmailOnRegistration").asBoolean(false);
                    } catch (Exception e) {
                        log.warn("【UniHalo】解析系统设置 user 组失败，按未开启邮箱验证处理", e);
                        return false;
                    }
                })
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.warn("【UniHalo】读取系统设置 user 组失败，按未开启邮箱验证处理", e);
                    return Mono.just(false);
                });
    }

    /**
     * 注册默认邮箱：{@code 用户名@example.com}。
     *
     * 给新号兜底一个合法邮箱
     */
    private static String defaultEmail(String username) {
        return username + "@example.com";
    }

    /**
     * Halo 注册失败 → 业务错误。
     *
     * {@code signUp} 的校验异常多为 application 模块私有类型（与 DuplicateNameException
     * 同理，api 依赖里没有），无法按类型 import，只能按类名/原因文本识别 —— 与微信
     * 自动注册路径的映射口径保持一致。消息面向用户，不暴露内部细节。
     */
    private AuthException mapSignUpFailure(Throwable e) {
        if (e instanceof AuthException auth) {
            return auth;
        }
        var name = e.getClass().getName();
        if ("run.halo.app.infra.exception.DuplicateNameException".equals(name)) {
            return BizErrorCode.USERNAME_EXISTS.toException();
        }
        if ("run.halo.app.infra.exception.RestrictedNameException".equals(name)) {
            return BizErrorCode.NAME_RESTRICTED.toException();
        }
        if ("run.halo.app.infra.exception.EmailVerificationFailed".equals(name)) {
            return BizErrorCode.EMAIL_CODE_INVALID.toException();
        }
        if ("run.halo.app.infra.exception.EmailAlreadyTakenException".equals(name)) {
            return BizErrorCode.EMAIL_ALREADY_TAKEN.toException();
        }
        if ("run.halo.app.infra.exception.AgreementNotAcceptedException".equals(name)) {
            return BizErrorCode.AGREEMENT_REQUIRED.toException();
        }
        if (e instanceof ServerWebInputException inputException) {
            var reason = String.valueOf(inputException.getReason());
            if (reason.contains("registration")) {
                return BizErrorCode.REGISTER_FORBIDDEN.toException();
            }
            if (reason.contains("default role")) {
                return BizErrorCode.REGISTER_FORBIDDEN.toException(
                        "站点未配置新用户默认角色，请联系站长检查 Halo 系统设置");
            }
            return BizErrorCode.BAD_REQUEST.toException("注册信息不合法，请检查后重试");
        }
        log.warn("【UniHalo】账号密码注册失败", e);
        return BizErrorCode.REGISTER_FAILED.toException();
    }

    /** 仅「用户可修复」的注册错误计入限流（见 ACCOUNTABLE_REGISTER_CODES）。 */
    private void recordRegisterFailure(String username, String clientIp, AuthException e) {
        if (ACCOUNTABLE_REGISTER_CODES.contains(e.getCode())) {
            attemptGuard.recordFailure(username, clientIp);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 按 openid/unionid 找到已绑定的用户，否则自动注册并绑定。 */
    private Mono<LoginResult> resolveWechatUser(WechatService.WechatSession session,
            LoginConfig config) {
        var identity = identity(session);
        return findConnection(session)
                .flatMap(this::loadBoundUser)
                .flatMap(user -> issueFor(user, config))
                .switchIfEmpty(Mono.defer(() -> requireEmailVerificationForSignup(session)
                        .then(Mono.defer(() -> registerWechatUser(config, identity)
                                .flatMap(user -> connect(user.getMetadata().getName(), identity)
                                        .thenReturn(user))
                                .flatMap(user -> issueFor(user, config))))));
    }

    /**
     * 站点开启「注册必须验证邮箱」时拦截微信静默注册：signUp 对空邮箱 fail closed，
     * 故不建号，签发短时 HMAC 票据随 {@code WECHAT_EMAIL_REQUIRED} 下发，
     * 客户端补齐邮箱 + 验证码后走 {@link #registerByWechatEmail(String, String, String, String)}。
     */
    private Mono<Void> requireEmailVerificationForSignup(WechatService.WechatSession session) {
        return mustVerifyEmailOnRegistration().flatMap(required -> {
            if (!required) {
                return Mono.empty();
            }
            var ticket = registerTicketManager.issue(session.openid(), session.unionid());
            return Mono.error(new AuthException(BizErrorCode.WECHAT_EMAIL_REQUIRED,
                    BizErrorCode.WECHAT_EMAIL_REQUIRED.getMessage(), java.util.Map.of("ticket", ticket)));
        });
    }

    /**
     * 取绑定关系指向的用户。
     *
     * 孤儿绑定自愈：{@code UserConnection} 存在但 Halo 用户已被删除时（站长清理用户、
     * 早期版本残留等），如果直接抛 404，这个微信就会永久卡死 —— 既登不进来，
     * 也走不到注册分支。这里改为清掉脏绑定、返回空，让上层按新用户重新注册。
     * 由于 {@code nextSequence} 会复用已释放的序号，用户一般能拿回原来的用户名。
     *
     * 用 {@code client.fetch} 而非 {@code userService.getUser}：前者「查不到」返回空 Mono，
     * 后者抛异常 —— 这里需要的正是「可选」语义。
     */
    private Mono<User> loadBoundUser(UserConnection connection) {
        var username = connection.getSpec().getUsername();
        return client.fetch(User.class, username)
                .flatMap(this::requireEnabledUser)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("【UniHalo】微信绑定指向的用户 {} 已不存在，清理该孤儿绑定后重新注册",
                            username);
                    return discardConnection(connection).then(Mono.empty());
                }));
    }

    /** 删除脏绑定关系；失败只记日志，不阻断登录（下次登录会再试）。 */
    private Mono<Void> discardConnection(UserConnection connection) {
        return client.delete(connection)
                .onErrorResume(e -> {
                    log.warn("【UniHalo】清理孤儿绑定失败，将在下次登录时重试", e);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * 禁用账号不得登录（P0）。
     *
     * 与 {@link #authenticate} 保持一致的语义：少了这一步，站长在后台禁用某个微信用户后，
     * 对方仍能一键登录进来，禁用功能对移动端形同虚设。
     */
    private Mono<User> requireEnabledUser(User user) {
        if (Boolean.TRUE.equals(user.getSpec().getDisabled())) {
            return Mono.error(BizErrorCode.USER_DISABLED.toException());
        }
        return Mono.just(user);
    }

    private Mono<User> registerWechatUser(LoginConfig config, String identity) {
        return registerWechatUser(config, identity, null, null);
    }

    /**
     * 自动注册，按配置的用户名类型生成用户名与昵称：
     * <ul>
     * <li>prefix_seq：用户名 = {@code 前缀 + 两位递增序号}（如 {@code unihalo01}），
     * 昵称 = {@code 微信用户 + 同序号}，便于在用户列表里区分；</li>
     * <li>uuid / hash：用户名 = {@code uhu- + 12 位标识段}（统一前缀 uhu-），
     * 昵称 = {@code 微信用户 + 6 位标识尾巴}。</li>
     * </ul>
     *
     * @param email     补邮箱注册路径的邮箱（静默注册传 null，走默认邮箱/强制验证逻辑）
     * @param emailCode 补邮箱注册路径的验证码（静默注册传 null）
     */
    private Mono<User> registerWechatUser(LoginConfig config, String identity,
            String email, String emailCode) {
        var type = config.usernameType();
        if (LoginConfig.TYPE_UUID.equals(type)) {
            return createWithCandidateUsernames(
                    Flux.range(0, MAX_USERNAME_ATTEMPTS)
                            .map(i -> randomUuidUsername()),
                    config, email, emailCode);
        }
        if (LoginConfig.TYPE_HASH.equals(type)) {
            var base = hashedUsername(identity);
            // 哈希本应唯一，被占用极可能是同身份的历史残留或极端碰撞；
            // 追加递增尾巴重试（uhu-xxxx…2）保证注册不被卡死。
            return createWithCandidateUsernames(
                    Flux.concat(Flux.just(base),
                            Flux.range(1, MAX_USERNAME_ATTEMPTS - 1)
                                    .map(i -> base + i)),
                    config, email, emailCode);
        }
        // prefix_seq（默认）
        var prefix = config.usernamePrefix();
        return nextSequence(prefix)
                .flatMap(start -> createWithCandidateUsernames(
                        Flux.range(start, MAX_USERNAME_ATTEMPTS)
                                .map(seq -> prefix + String.format(SEQUENCE_FORMAT, seq)),
                        config, email, emailCode));
    }

    /** 依次尝试候选用户名，第一个创建成功的即返回；全部占用才报错。 */
    private Mono<User> createWithCandidateUsernames(Flux<String> candidates,
            LoginConfig config, String email, String emailCode) {
        return candidates
                .concatMap(username -> signUp(username, config, email, emailCode)
                        // 并发抢号等 DuplicateName 场景吞掉本次，继续试下一个候选；
                        // 其余错误（未开放注册/默认角色未配置等）直接透出终止。
                        .onErrorResume(e -> isDuplicateName(e) ? Mono.empty() : Mono.error(e)))
                .next()
                .switchIfEmpty(Mono.error(BizErrorCode.REGISTER_FAILED
                        .toException("自动注册失败，请稍后重试")));
    }

    /** 随机标识用户名：uhu- + UUID 去连字符后的前 12 位（小写十六进制）。 */
    private static String randomUuidUsername() {
        var segment = UUID.randomUUID().toString().replace("-", "")
                .substring(0, Constants.USERNAME_ID_SEGMENT_LENGTH);
        return Constants.USERNAME_ID_PREFIX + segment;
    }

    /** 微信标识用户名：uhu- + SHA-256(unionid/openid) 的前 12 位（确定性）。 */
    private static String hashedUsername(String identity) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (int i = 0; sb.length() < Constants.USERNAME_ID_SEGMENT_LENGTH; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return Constants.USERNAME_ID_PREFIX + sb;
        } catch (java.security.NoSuchAlgorithmException e) {
            // JVM 必带 SHA-256，不会发生；防御性回落随机标识。
            return randomUuidUsername();
        }
    }

    /**
     * 昵称 = {@code 微信用户 + 标识尾巴}（prefix_seq 为序号、其余为用户名末 6 位），
     * 超过 {@link Constants#DISPLAY_NAME_MAX_LENGTH} 字符硬性截断。
     */
    private static String displayName(String username, LoginConfig config) {
        String tail;
        if (LoginConfig.TYPE_PREFIX_SEQ.equals(config.usernameType())) {
            tail = username.substring(config.usernamePrefix().length());
        } else {
            tail = username.substring(username.length() - 6);
        }
        var name = "微信用户" + tail;
        return name.length() <= Constants.DISPLAY_NAME_MAX_LENGTH ? name
                : name.substring(0, Constants.DISPLAY_NAME_MAX_LENGTH);
    }

    private Mono<User> signUp(String username, LoginConfig config) {
        return signUp(username, config, null, null);
    }

    /**
     * 创建用户：昵称 = {@code 微信用户 + 6 位标识尾巴}（prefix_seq 为序号），
     * 超过 {@link Constants#DISPLAY_NAME_MAX_LENGTH} 字符硬性截断。
     *
     * @param email     补邮箱注册路径的邮箱（非空时连同验证码写入注册数据，
     *                  由 signUp 校验验证码与邮箱一致）；静默注册传 null
     * @param emailCode 补邮箱注册路径的验证码（随 email 一同传入）
     */
    private Mono<User> signUp(String username, LoginConfig config, String email,
            String emailCode) {
        return mustVerifyEmailOnRegistration().flatMap(emailVerifyRequired -> {
            var data = new SignUpData();
            data.setUsername(username);
            data.setDisplayName(displayName(username, config));
            // 初始密码按设置页类型取值：固定密码（所有自动注册用户共用，需≥5位，
            // 不满足回落随机）或随机强密码（默认）；密码会经注册欢迎通知告知本人。
            var plainPassword = LoginConfig.PASSWORD_TYPE_FIXED.equals(config.passwordType())
                    ? config.fixedPassword()
                    : randomPassword();
            data.setPassword(plainPassword);
            data.setConfirmPassword(plainPassword);
            if (email != null) {
                // 补邮箱注册路径：邮箱与验证码由客户端提供（验证码在注册前已发往
                // 该邮箱），signUp 内部校验验证码有效性及与邮箱一致
                data.setEmail(email);
                data.setEmailCode(emailCode);
            } else {
                // 静默注册路径：默认邮箱兜底（仅站点未强制邮箱验证时）——
                // 用户名@example.com，规避空邮箱问题；强制验证时保持空邮箱
                // （上游 requireEmailVerificationForSignup 已拦截，不会走到这里）
                data.setEmail(emailVerifyRequired ? null : defaultEmail(username));
            }
            // 微信一键登录是服务端静默注册，无表单勾选动作；Halo 仅在系统设置配置了
            // 必读协议页时才校验该值，未配置时校验整体跳过，置 true 两种情况均通过。
            // 用户侧的协议确认由 app 端登录前流程承担。
            data.setAgreedToTerms(true);
            return userService.signUp(data)
                    // 注册成功后发欢迎通知（含用户名/昵称/注册邮箱/初始密码，仅此一次；
                    // 失败不阻断注册）
                    .doOnSuccess(user -> notificationHelper
                            .emitUserRegistered(username, data.getDisplayName(), plainPassword,
                                    data.getEmail())
                            .subscribe())
                    .onErrorResume(e -> {
                    // 并发抢号（用户名已被占用）时吞掉本次，让调用方继续试下一个序号；
                    // 连试 MAX_USERNAME_ATTEMPTS 次都失败才向上抛业务错误。
                    // 其余失败（站点未开放注册、默认角色未配置等配置问题）必须透出：
                    // 换序号重试没有意义，吞掉只会把明确的配置问题变成含糊的 REGISTER_FAILED。
                    // DuplicateNameException 在 Halo application 模块、api 依赖里没有，
                    // 只能按类名识别（父类 ResponseStatusException 400 与「注册未开放」共用，无法按类型区分）。
                    if (isDuplicateName(e)) {
                        log.info("【UniHalo】微信自动注册用户名 {} 已被占用，尝试下一个候选", username);
                        return Mono.error(e);
                    }
                    // 注册失败的配置类原因（未开放注册 / 默认角色未配置）转成明确的业务错误，
                    // 提示站长去 Halo 系统设置修复；AuthException 会穿过 unexpectedWechatFailure 兜底。
                    if (e instanceof ServerWebInputException inputException) {
                        var reason = String.valueOf(inputException.getReason());
                        var message = reason.contains("registration")
                                ? "未开启新用户一键登录"
                                : reason.contains("default role")
                                        ? "站点未配置新用户默认角色，请在 Halo 系统设置中选择默认角色"
                                        : "自动注册失败：" + reason;
                        return Mono.error(BizErrorCode.REGISTER_FORBIDDEN
                                .toException(message));
                    }
                    log.warn("【UniHalo】微信自动注册用户名 {} 创建失败", username, e);
                    return Mono.error(e);
                    });
        });
    }

    private static boolean isDuplicateName(Throwable e) {
        return "run.halo.app.infra.exception.DuplicateNameException"
                .equals(e.getClass().getName());
    }

    /**
     * 扫描现有「前缀 + 纯数字」的用户名取最大序号，返回下一个可用的起始序号。
     *
     * 只做一次探测是不够的（并发下两个请求可能拿到同一序号），因此结果仅作为起点，
     * 真正防重由 {@link #tryCreate} 的占用探测 + 多序号重试兜底。
     */
    private Mono<Integer> nextSequence(String prefix) {
        return client.list(User.class,
                        user -> user.getMetadata() != null
                                && user.getMetadata().getName() != null
                                && user.getMetadata().getName().startsWith(prefix),
                        null)
                .map(user -> sequenceOf(user.getMetadata().getName(), prefix))
                .reduce(0, Math::max)
                .map(max -> max + 1)
                .defaultIfEmpty(1);
    }

    private static int sequenceOf(String username, String prefix) {
        var tail = username.substring(prefix.length());
        if (tail.isEmpty() || tail.length() > 9 || !tail.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        return Integer.parseInt(tail);
    }

    /**
     * 建立绑定关系（一对一：一个微信只能绑一个账号，一个账号只能绑一个微信）。
     *
     * <p>全部绑定路径（一键绑定 / 扫码绑定 / 微信自动注册）都收敛到这里，
     * 冲突校验必须放在这一层 —— 放在上游会被扫码票据路径绕过。
     *
     * <p>历史缺陷：命中已有绑定后直接 {@code setUsername} 改写，导致原账号被静默
     * 解绑，且原账号用该微信登录会登进新账号（串号）。现改为<b>一律拒绝</b>：
     * 一对一约束下换绑必须显式先解绑，服务端不做隐式转移。
     *
     * <p>冲突时记 warn 日志（含双方用户名与微信标识，仅供站长排查），
     * 但对外文案不回显占用方账号名，防账号枚举。
     */
    private Mono<UserConnection> connect(String username, String identity) {
        return findConnection(identity)
                .flatMap(existing -> {
                    var owner = existing.getSpec().getUsername();
                    if (username.equals(owner)) {
                        // 同一账号重复绑同一个微信：幂等成功，不重复建记录
                        return Mono.just(existing);
                    }
                    log.warn("【UniHalo】微信绑定冲突（微信已绑其他账号）：微信标识已绑定账号 {}，"
                            + "账号 {} 的绑定请求被拒绝", owner, username);
                    return Mono.<UserConnection>error(
                            BizErrorCode.WECHAT_ALREADY_BOUND.toException());
                })
                .switchIfEmpty(Mono.defer(() -> findConnectionByUsername(username)
                        .flatMap(owned -> {
                            log.warn("【UniHalo】微信绑定冲突（账号已绑其他微信）：账号 {} 已绑定"
                                            + "微信 {}，绑定新微信的请求被拒绝",
                                    username, owned.getSpec().getProviderUserId());
                            return Mono.<UserConnection>error(
                                    BizErrorCode.ACCOUNT_ALREADY_BOUND.toException());
                        })
                        .switchIfEmpty(Mono.defer(
                                () -> createConnection(username, identity)))));
    }

    /** 新建绑定关系（调用方已确保双向均无冲突）。 */
    private Mono<UserConnection> createConnection(String username, String identity) {
        var connection = new UserConnection();
        connection.setMetadata(new Metadata());
        connection.getMetadata().setGenerateName("wechat-");
        var spec = new UserConnection.UserConnectionSpec();
        spec.setRegistrationId(Constants.WECHAT_REGISTRATION_ID);
        spec.setUsername(username);
        spec.setProviderUserId(identity);
        connection.setSpec(spec);
        return client.create(connection);
    }

    /**
     * 按用户名查该账号已有的微信绑定（一对一约束的<b>反向</b>校验）。
     *
     * <p>旧实现只按微信身份查重、从不按用户名查重，于是同一个账号能绑多个微信、
     * 攒下多条 {@code UserConnection}；而查询与解绑都只取第一条，
     * 造成「解绑后仍能微信登录」。这里只要存在任意一条就拒绝再绑。
     */
    private Mono<UserConnection> findConnectionByUsername(String username) {
        return UserConnectionSupport.newest(connectionsOf(username));
    }

    /** 列出该账号的全部微信绑定关系（含历史重复记录）。 */
    private Flux<UserConnection> connectionsOf(String username) {
        return client.list(UserConnection.class,
                connection -> connection.getSpec() != null
                        && Constants.WECHAT_REGISTRATION_ID.equals(
                                connection.getSpec().getRegistrationId())
                        && username.equals(connection.getSpec().getUsername()),
                null);
    }

    /**
     * 按微信身份查绑定关系：优先 unionid，未命中再回落 openid。
     *
     * 回落是必需的：站点早期未绑定开放平台时只拿得到 openid，老用户存的是 openid；
     * 后期绑定开放平台后 code2Session 开始返回 unionid，只按 unionid 查会漏掉这批老用户，
     * 把他们当成新用户再注册一个号。
     */
    private Mono<UserConnection> findConnection(WechatService.WechatSession session) {
        var unionid = session.unionid();
        var openid = session.openid();
        boolean hasBoth = unionid != null && !unionid.isBlank()
                && openid != null && !openid.isBlank();
        if (!hasBoth) {
            return findConnection(identity(session));
        }
        return findConnection(unionid)
                .switchIfEmpty(Mono.defer(() -> findConnection(openid)));
    }

    private Mono<UserConnection> findConnection(String identity) {
        return UserConnectionSupport.newest(client.list(UserConnection.class,
                connection -> connection.getSpec() != null
                        && Constants.WECHAT_REGISTRATION_ID.equals(
                                connection.getSpec().getRegistrationId())
                        && identity.equals(connection.getSpec().getProviderUserId()),
                null));
    }

    /**
     * 微信侧失败：code 无效/已用过（{@link IllegalArgumentException}）、
     * 密钥未配置或缺少字段（{@link IllegalStateException}）。两者都要收敛成业务错误，
     * 否则会以 500 暴露给客户端，掩盖「站长还没配密钥」这类可自助修复的问题。
     */
    private boolean isWechatFailure(Throwable e) {
        return e instanceof IllegalArgumentException || e instanceof IllegalStateException;
    }

    /**
     * 微信登录路径的兜底映射：除 {@link AuthException} 外一律收敛为业务错误。
     *
     * 不做兜底会让 Halo 内部异常以原始 problem detail 漏给客户端 —— 例如
     * {@code {"detail":"User unihalo01 was not found","status":404}}，既暴露内部用户名，
     * 又不符合本接口 {@code {code,message}} 的契约。
     *
     * 已知的微信侧失败保留原始消息（code 无效、密钥未配置），站长据此可自助排查；
     * 其余只记日志、对外给通用提示，避免把内部细节透出去。
     */
    private AuthException unexpectedWechatFailure(Throwable e) {
        if (isWechatFailure(e)) {
            return BizErrorCode.WECHAT_LOGIN_FAILED.toException(e.getMessage());
        }
        log.warn("【UniHalo】微信登录出现未预期的错误", e);
        return BizErrorCode.WECHAT_LOGIN_FAILED.toException();
    }

    private static String identity(WechatService.WechatSession session) {
        return session.unionid() == null || session.unionid().isBlank()
                ? session.openid() : session.unionid();
    }

    private Mono<LoginResult> issueFor(User user, LoginConfig config) {
        String username = user.getMetadata().getName();
        return resolveRoles(username)
                .flatMap(roles -> patIssuer.issue(username, roles,
                        Instant.now().plus(Duration.ofDays(config.tokenTtlDays()))))
                .flatMap(token -> permissions(token.roles())
                        .map(rules -> new LoginResult(
                                token.token(),
                                "Bearer",
                                token.expiresAt(),
                                token.patName(),
                                loginUserOf(user),
                                token.roles(),
                                rules)));
    }

    /** 用户摘要（不含任何敏感字段）。token 与 profile 两个出口共用，避免字段漂移。 */
    private static LoginResult.LoginUser loginUserOf(User user) {
        return new LoginResult.LoginUser(
                user.getMetadata().getName(),
                user.getSpec().getDisplayName(),
                user.getSpec().getAvatar(),
                user.getSpec().getEmail(),
                passwordSetByUser(user));
    }

    /** 用户是否「自主设置过密码」：读首次设密接口写入的注解，缺失即 false。 */
    private static boolean passwordSetByUser(User user) {
        var annotations = user.getMetadata() == null
                ? null : user.getMetadata().getAnnotations();
        return annotations != null
                && Boolean.parseBoolean(annotations.get(
                        Constants.PASSWORD_SET_BY_USER_ANNOTATION));
    }

    /**
     * 令牌角色恒等于用户在 Halo 已有的角色（不提权也不裁剪）：
     * 新用户注册为什么角色由 Halo 系统设置-用户设置的「默认角色」决定。
     */
    private Mono<Set<String>> resolveRoles(String username) {
        return roleService.getRolesByUsername(username)
                .collect(Collectors.toSet());
    }

    private Mono<List<LoginResult.PermissionRule>> permissions(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Mono.just(List.of());
        }
        return roleService.listPermissions(roles)
                .flatMap(this::rulesOf)
                .distinct()
                .collect(Collectors.toList());
    }

    private Flux<LoginResult.PermissionRule> rulesOf(Role role) {
        var rules = role.getRules();
        if (rules == null || rules.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(rules)
                .filter(Objects::nonNull)
                .map(rule -> new LoginResult.PermissionRule(
                        List.of(nullToEmpty(rule.getApiGroups())),
                        List.of(nullToEmpty(rule.getResources())),
                        List.of(nullToEmpty(rule.getVerbs()))));
    }

    private static String[] nullToEmpty(String[] values) {
        return values == null ? new String[0] : values;
    }

    /**
     * 随机强密码：Wx + 12 位随机十六进制 + a1!（总长 16 位，含大小写/数字/特殊字符，
     * 满足 Halo 密码策略与「最长 16 位」要求；经注册欢迎通知告知本人）。
     */
    /* ---------- 随机初始密码：uhc 前缀 + 数字/大小写/符号随机混合 ---------- */

    private static final String PASSWORD_DIGITS = "0123456789";

    private static final String PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz";

    private static final String PASSWORD_SYMBOLS = "!@#$%^&*";

    private static final String PASSWORD_ALPHABET =
            PASSWORD_DIGITS + PASSWORD_UPPER + PASSWORD_LOWER + PASSWORD_SYMBOLS;

    private static final int PASSWORD_RANDOM_LENGTH = 13;

    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

    /**
     * 生成随机初始密码：{@code uhc} 前缀 + 随机数字/大小写字母/符号混合，无固定后缀；
     * 随机段保证四类字符各至少一个（先各取一枚，其余从全池随机，最后洗牌）。
     */
    private static String randomPassword() {
        var chars = new ArrayList<Character>(PASSWORD_RANDOM_LENGTH);
        chars.add(PASSWORD_DIGITS.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_DIGITS.length())));
        chars.add(PASSWORD_UPPER.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_UPPER.length())));
        chars.add(PASSWORD_LOWER.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_LOWER.length())));
        chars.add(PASSWORD_SYMBOLS.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_SYMBOLS.length())));
        for (int i = chars.size(); i < PASSWORD_RANDOM_LENGTH; i++) {
            chars.add(PASSWORD_ALPHABET.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        Collections.shuffle(chars, PASSWORD_RANDOM);
        var sb = new StringBuilder("uhc");
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }
}
