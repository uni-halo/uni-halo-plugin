package cn.ialley.unihalo.services.impl;

import java.time.Duration;
import java.time.Instant;
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
import cn.ialley.unihalo.services.AuthService;
import cn.ialley.unihalo.services.BindTicketService;
import cn.ialley.unihalo.services.PatIssuer;
import cn.ialley.unihalo.services.WechatService;
import cn.ialley.unihalo.utils.LoginAttemptGuard;
import cn.ialley.unihalo.utils.LoginConfigResolver;
import cn.ialley.unihalo.vo.LoginConfig;
import cn.ialley.unihalo.vo.LoginResult;
import cn.ialley.unihalo.vo.ProfileVo;
import cn.ialley.unihalo.vo.RegisterForm;
import cn.ialley.unihalo.vo.WechatBindingVo;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.UserConnection;
import run.halo.app.core.extension.Role;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.core.user.service.SignUpData;
import run.halo.app.core.user.service.UserService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

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

    /**
     * 注册失败中「用户可修复」的错误码（计入限流）：
     * 用户名重复/受限、验证码无效、邮箱被占、未同意协议。
     * 配置类错误（站点未开放注册、默认角色未配置）不计 —— 那是站长侧问题，
     * 客户端重试无意义，计入限流只会把配置问题变成 429 迷惑用户。
     */
    private static final Set<String> ACCOUNTABLE_REGISTER_CODES = Set.of(
            "USERNAME_EXISTS", "NAME_RESTRICTED", "EMAIL_CODE_INVALID",
            "EMAIL_ALREADY_TAKEN", "AGREEMENT_REQUIRED");

    private final UserService userService;
    private final RoleService roleService;
    private final PatIssuer patIssuer;
    private final LoginConfigResolver configResolver;
    private final WechatService wechatService;
    private final ReactiveExtensionClient client;
    private final LoginAttemptGuard attemptGuard;
    private final BindTicketService bindTicketService;
    private final cn.ialley.unihalo.utils.NotificationHelper notificationHelper;

    @Override
    public Mono<LoginResult> loginByPassword(String username, String rawPassword, String clientIp) {
        return configResolver.config()
                .filter(LoginConfig::passwordLoginEnabled)
                .switchIfEmpty(Mono.error(
                        new AuthException("PASSWORD_LOGIN_DISABLED", "账号密码登录未开启")))
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
                        new AuthException("WECHAT_LOGIN_DISABLED", "微信登录未开启")))
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
            // Bean Validation 不经过内部服务调用（那是 Web 层注解的职责），基础项自行把关，
            // 提示语与 APP 端文案对齐；更细的格式校验（用户名 4-63 位、密码 ≥5 位、
            // 邮箱格式/验证码、注册协议）由 Halo signUp 内部 fail closed 校验并在此映射成中文。
            if (form == null || isBlank(form.username()) || isBlank(form.displayName())
                    || isBlank(form.password())) {
                return Mono.error(new AuthException("BAD_REQUEST", "请填写完整的注册信息"));
            }
            if (!form.password().equals(form.confirmPassword())) {
                return Mono.error(new AuthException("BAD_REQUEST", "两次输入的密码不一致"));
            }
            return requireNotLocked(form.username(), clientIp)
                    .then(configResolver.config())
                    .flatMap(config -> userService.signUp(toSignUpData(form))
                            .doOnSuccess(user -> attemptGuard.resetUsername(form.username()))
                            // signUp 失败先映射再计数：只有「用户可修复」的错误计入限流，
                            // 与登录闸门共用（用户名 5 / IP 20 → 锁 15 分钟）
                            .onErrorMap(this::mapSignUpFailure)
                            .doOnError(AuthException.class,
                                    e -> recordRegisterFailure(form.username(), clientIp, e))
                            .flatMap(user -> issueFor(user, config)));
        });
    }

    @Override
    public Mono<LoginResult> registerByWechat(String code) {
        // 与微信登录同源：已绑定则登录，未绑定自动建号（signUp 内部校验注册开关，
        // 关闭时 REGISTER_FORBIDDEN fail closed）。复用实现避免双路径漂移，
        // 注册页按钮语义 =「没有账号就建号，有账号就登录」。
        return loginByWechat(code);
    }

    @Override
    public Mono<Void> bindWechat(String username, String code) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        new AuthException("WECHAT_LOGIN_DISABLED", "微信登录未开启")))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .flatMap(credential -> wechatService.code2Session(
                                credential.appId(), credential.appSecret(), code)))
                .flatMap(session -> connect(username, identity(session)))
                .onErrorMap(e -> !(e instanceof AuthException),
                        this::unexpectedWechatFailure)
                .then();
    }

    @Override
    public Mono<Void> logout(String patName, String username) {
        return patIssuer.revoke(patName, username);
    }

    @Override
    public Mono<BindTicketService.IssuedTicket> createBindTicket(String username) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        new AuthException("WECHAT_LOGIN_DISABLED", "微信登录未开启")))
                .then(bindTicketService.issue(username));
    }

    @Override
    public Mono<BindTicketService.TicketStatus> bindTicketStatus(String ticket) {
        return bindTicketService.status(ticket);
    }

    @Override
    public Mono<Void> confirmBindTicket(String ticket, String code) {
        return configResolver.config()
                .filter(LoginConfig::wechatLoginEnabled)
                .switchIfEmpty(Mono.error(
                        new AuthException("WECHAT_LOGIN_DISABLED", "微信登录未开启")))
                .flatMap(config -> configResolver.wechatCredential(config.wechatSecretName())
                        .onErrorResume(e -> Mono.error(e instanceof AuthException ? e
                                : new AuthException("WECHAT_LOGIN_FAILED",
                                        "微信登录未配置，请联系站长",
                                        AuthException.STATUS_FORBIDDEN)))
                        .flatMap(credential -> bindTicketService.consume(ticket)
                                .flatMap(consumed -> {
                                    if (!consumed.success()) {
                                        return Mono.error(new AuthException(
                                                "BIND_TICKET_INVALID", consumed.reason(),
                                                AuthException.STATUS_BAD_REQUEST));
                                    }
                                    return wechatService.code2Session(
                                            credential.appId(), credential.appSecret(), code)
                                            .flatMap(session -> connect(consumed.username(),
                                                    identity(session)))
                                            .onErrorMap(e -> !(e instanceof AuthException),
                                                    this::unexpectedWechatFailure);
                                })))
                .then();
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
        return client.list(UserConnection.class,
                        connection -> connection.getSpec() != null
                                && Constants.WECHAT_REGISTRATION_ID.equals(
                                        connection.getSpec().getRegistrationId())
                                && username.equals(connection.getSpec().getUsername()),
                        null)
                .next()
                .map(connection -> new WechatBindingVo(
                        username,
                        true,
                        connection.getSpec().getProviderUserId(),
                        connection.getSpec().getUpdatedAt()))
                .defaultIfEmpty(WechatBindingVo.unbound(username));
    }

    @Override
    public Mono<Void> setInitialPassword(String username, String newPassword) {
        return client.fetch(User.class, username)
                .switchIfEmpty(Mono.error(new AuthException("USER_NOT_FOUND", "用户不存在")))
                .flatMap(user -> {
                    var annotations = user.getMetadata() == null
                            ? null : user.getMetadata().getAnnotations();
                    if (annotations != null
                            && Boolean.parseBoolean(annotations.get(
                                    Constants.PASSWORD_SET_BY_USER_ANNOTATION))) {
                        // 已自主设置过密码：免旧密码通道关闭，防止登录态泄露后被直接换密
                        return Mono.error(new AuthException("PASSWORD_ALREADY_SET",
                                "密码已设置，请使用旧密码修改"));
                    }
                    if (newPassword == null || newPassword.length() < Constants.PASSWORD_MIN_LENGTH) {
                        return Mono.error(new AuthException("BAD_REQUEST",
                                "密码长度至少 " + Constants.PASSWORD_MIN_LENGTH + " 位"));
                    }
                    return userService.updatePassword(username, newPassword)
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

    @Override
    public Mono<Void> unbindMyWechat(String username) {
        return client.list(UserConnection.class,
                        connection -> connection.getSpec() != null
                                && Constants.WECHAT_REGISTRATION_ID.equals(
                                        connection.getSpec().getRegistrationId())
                                && username.equals(connection.getSpec().getUsername()),
                        null)
                .next()
                .flatMap(client::delete)
                .then();
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
        return Mono.error(new AuthException("TOO_MANY_ATTEMPTS",
                "登录尝试次数过多，请 %d 分钟后再试".formatted(minutes),
                AuthException.STATUS_TOO_MANY_REQUESTS));
    }

    private Mono<User> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null
                || rawPassword.isBlank()) {
            return Mono.error(new AuthException("BAD_CREDENTIALS", "请输入用户名和密码"));
        }
        return userService.getUser(username)
                .onErrorMap(e -> new AuthException("BAD_CREDENTIALS", "用户名或密码错误"))
                .flatMap(user -> {
                    if (Boolean.TRUE.equals(user.getSpec().getDisabled())) {
                        return Mono.error(new AuthException("USER_DISABLED", "账号已被禁用"));
                    }
                    if (Boolean.TRUE.equals(user.getSpec().getTwoFactorAuthEnabled())) {
                        return Mono.error(new AuthException("TWO_FACTOR_REQUIRED",
                                "该账号已开启二次验证，请使用微信登录"));
                    }
                    return userService.confirmPassword(username, rawPassword)
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(Mono.error(
                                    new AuthException("BAD_CREDENTIALS", "用户名或密码错误")))
                            .thenReturn(user);
                });
    }

    /** 表单 → Halo 注册数据（email/emailCode/agreedToTerms 原样透传，由 signUp 校验）。 */
    private SignUpData toSignUpData(RegisterForm form) {
        var data = new SignUpData();
        data.setUsername(form.username().trim());
        data.setDisplayName(form.displayName().trim());
        data.setPassword(form.password());
        data.setConfirmPassword(form.confirmPassword());
        data.setEmail(isBlank(form.email()) ? null : form.email().trim());
        data.setEmailCode(isBlank(form.emailCode()) ? null : form.emailCode().trim());
        data.setAgreedToTerms(Boolean.TRUE.equals(form.agreedToTerms()));
        return data;
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
            return new AuthException("USERNAME_EXISTS", "用户名已被占用，换一个试试",
                    AuthException.STATUS_BAD_REQUEST);
        }
        if ("run.halo.app.infra.exception.RestrictedNameException".equals(name)) {
            return new AuthException("NAME_RESTRICTED", "用户名或昵称不符合规范",
                    AuthException.STATUS_BAD_REQUEST);
        }
        if ("run.halo.app.infra.exception.EmailVerificationFailed".equals(name)) {
            return new AuthException("EMAIL_CODE_INVALID", "邮箱验证码无效或已过期",
                    AuthException.STATUS_BAD_REQUEST);
        }
        if ("run.halo.app.infra.exception.EmailAlreadyTakenException".equals(name)) {
            return new AuthException("EMAIL_ALREADY_TAKEN", "该邮箱已被其他账号占用",
                    AuthException.STATUS_BAD_REQUEST);
        }
        if ("run.halo.app.infra.exception.AgreementNotAcceptedException".equals(name)) {
            return new AuthException("AGREEMENT_REQUIRED", "请先阅读并同意用户协议",
                    AuthException.STATUS_BAD_REQUEST);
        }
        if (e instanceof ServerWebInputException inputException) {
            var reason = String.valueOf(inputException.getReason());
            if (reason.contains("registration")) {
                return new AuthException("REGISTER_FORBIDDEN",
                        "未开放新用户注册",
                        AuthException.STATUS_FORBIDDEN);
            }
            if (reason.contains("default role")) {
                return new AuthException("REGISTER_FORBIDDEN",
                        "站点未配置新用户默认角色，请联系站长检查 Halo 系统设置",
                        AuthException.STATUS_FORBIDDEN);
            }
            return new AuthException("BAD_REQUEST", "注册信息不合法，请检查后重试",
                    AuthException.STATUS_BAD_REQUEST);
        }
        log.warn("【UniHalo】账号密码注册失败", e);
        return new AuthException("REGISTER_FAILED", "注册失败，请稍后重试");
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
                .switchIfEmpty(Mono.defer(() -> registerWechatUser(config, identity)
                        .flatMap(user -> connect(user.getMetadata().getName(), identity)
                                .thenReturn(user))
                        .flatMap(user -> issueFor(user, config))));
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
            return Mono.error(new AuthException("USER_DISABLED", "账号已被禁用"));
        }
        return Mono.just(user);
    }

    /**
     * 自动注册，按配置的用户名类型生成用户名与昵称：
     * <ul>
     * <li>prefix_seq：用户名 = {@code 前缀 + 两位递增序号}（如 {@code unihalo01}），
     * 昵称 = {@code 微信用户 + 同序号}，便于在用户列表里区分；</li>
     * <li>uuid / hash：用户名 = {@code uhu- + 12 位标识段}（统一前缀 uhu-），
     * 昵称 = {@code 微信用户 + 6 位标识尾巴}。</li>
     * </ul>
     */
    private Mono<User> registerWechatUser(LoginConfig config, String identity) {
        var type = config.usernameType();
        if (LoginConfig.TYPE_UUID.equals(type)) {
            return createWithCandidateUsernames(
                    Flux.range(0, MAX_USERNAME_ATTEMPTS)
                            .map(i -> randomUuidUsername()),
                    config);
        }
        if (LoginConfig.TYPE_HASH.equals(type)) {
            var base = hashedUsername(identity);
            // 哈希本应唯一，被占用极可能是同身份的历史残留或极端碰撞；
            // 追加递增尾巴重试（uhu-xxxx…2）保证注册不被卡死。
            return createWithCandidateUsernames(
                    Flux.concat(Flux.just(base),
                            Flux.range(1, MAX_USERNAME_ATTEMPTS - 1)
                                    .map(i -> base + i)),
                    config);
        }
        // prefix_seq（默认，存量行为不变）
        var prefix = config.usernamePrefix();
        return nextSequence(prefix)
                .flatMap(start -> createWithCandidateUsernames(
                        Flux.range(start, MAX_USERNAME_ATTEMPTS)
                                .map(seq -> prefix + String.format(SEQUENCE_FORMAT, seq)),
                        config));
    }

    /** 依次尝试候选用户名，第一个创建成功的即返回；全部占用才报错。 */
    private Mono<User> createWithCandidateUsernames(Flux<String> candidates,
            LoginConfig config) {
        return candidates
                .concatMap(username -> signUp(username, config)
                        // 并发抢号等 DuplicateName 场景吞掉本次，继续试下一个候选；
                        // 其余错误（未开放注册/默认角色未配置等）直接透出终止。
                        .onErrorResume(e -> isDuplicateName(e) ? Mono.empty() : Mono.error(e)))
                .next()
                .switchIfEmpty(Mono.error(new AuthException("REGISTER_FAILED",
                        "自动注册失败，请稍后重试")));
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

    /**
     * 创建用户：昵称 = {@code 微信用户 + 6 位标识尾巴}（prefix_seq 为序号），
     * 超过 {@link Constants#DISPLAY_NAME_MAX_LENGTH} 字符硬性截断。
     */
    private Mono<User> signUp(String username, LoginConfig config) {
        var data = new SignUpData();
        data.setUsername(username);
        data.setDisplayName(displayName(username, config));
        var plainPassword = randomPassword();
        data.setPassword(plainPassword);
        data.setConfirmPassword(plainPassword);
        // 微信一键登录是服务端静默注册，无表单勾选动作；Halo 仅在系统设置配置了
        // 必读协议页时才校验该值，未配置时校验整体跳过，置 true 两种情况均通过。
        // 用户侧的协议确认由 app 端登录前流程承担。
        data.setAgreedToTerms(true);
        return userService.signUp(data)
                // 注册成功后发欢迎通知（含用户名/昵称/初始密码，仅此一次；失败不阻断注册）
                .doOnSuccess(user -> notificationHelper
                        .emitUserRegistered(username, data.getDisplayName(), plainPassword)
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
                        return Mono.error(new AuthException("REGISTER_FORBIDDEN", message,
                                AuthException.STATUS_FORBIDDEN));
                    }
                    log.warn("【UniHalo】微信自动注册用户名 {} 创建失败", username, e);
                    return Mono.error(e);
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

    private Mono<UserConnection> connect(String username, String identity) {
        return findConnection(identity)
                .flatMap(existing -> {
                    existing.getSpec().setUsername(username);
                    return client.update(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    var connection = new UserConnection();
                    connection.setMetadata(new Metadata());
                    connection.getMetadata().setGenerateName("wechat-");
                    var spec = new UserConnection.UserConnectionSpec();
                    spec.setRegistrationId(Constants.WECHAT_REGISTRATION_ID);
                    spec.setUsername(username);
                    spec.setProviderUserId(identity);
                    connection.setSpec(spec);
                    return client.create(connection);
                }));
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
        return client.list(UserConnection.class,
                        connection -> connection.getSpec() != null
                                && Constants.WECHAT_REGISTRATION_ID.equals(
                                        connection.getSpec().getRegistrationId())
                                && identity.equals(connection.getSpec().getProviderUserId()),
                        null)
                .next();
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
            return new AuthException("WECHAT_LOGIN_FAILED", e.getMessage());
        }
        log.warn("【UniHalo】微信登录出现未预期的错误", e);
        return new AuthException("WECHAT_LOGIN_FAILED", "微信登录失败，请稍后重试");
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
                user.getSpec().getEmail());
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

    private static String randomPassword() {
        return "Wx" + UUID.randomUUID().toString().replace("-", "") + "a1!";
    }
}
