package cn.ialley.unihalo.exception;

import lombok.Getter;

/**
 * 移动端业务错误码总表（{@code code + message + status} 的唯一来源）。
 *
 * <p>命名说明：这是<b>业务</b>错误码（Business Error Code），与 HTTP 状态码分工不同 ——
 * HTTP 状态码表达「传输/协议层面的类别」（400/401/403/409/429/500），
 * 业务 code 表达「具体是哪一件事」（{@code WECHAT_ALREADY_BOUND}、
 * {@code ACCOUNT_ALREADY_BOUND}…）。同一 HTTP 状态会有多个业务 code，
 * 客户端按业务 code 分支、按 HTTP 状态决定重试/退避策略。
 *
 * <p>认证类接口（{@code /apis/api.unihalo.ialley.cn/v1alpha1/auth/**}）失败时统一返回
 * {@code {"code": "...", "message": "..."}} 与对应 HTTP 状态码，客户端按 {@code code}
 * 走分支（如 {@code WECHAT_ALREADY_BOUND} 提示换微信、{@code TOO_MANY_ATTEMPTS} 退避重试），
 * 而不是解析 message 文本。
 *
 * <p>约定：
 * <ul>
 * <li>新增业务分支必须先在此登记，禁止在业务代码里散写字符串常量；</li>
 * <li>{@code message} 是默认文案，动态文案（如剩余分钟数）用
 *     {@link #toException(String)} 覆盖，code 不变；</li>
 * <li>文案面向终端用户，<b>不得</b>包含他人账号名、内部异常细节或凭据，
 *     防账号枚举与信息泄露；</li>
 * <li>状态码语义：400 参数/状态无效、401 身份/凭据问题、403 已识别但被拒绝
 *     （配置不允许等）、409 资源冲突（唯一性约束被破坏）、429 限流、500 兜底。</li>
 * </ul>
 *
 * @author 小莫唐尼
 */
@Getter
public enum BizErrorCode {

    // ==================== 通用 ====================

    /** 请求体缺失或参数不合法。 */
    BAD_REQUEST("BAD_REQUEST", "请求参数有误", AuthException.STATUS_BAD_REQUEST),

    /** 未携带有效登录身份（含匿名身份访问需登录的写接口）。 */
    UNAUTHENTICATED("UNAUTHENTICATED", "未登录或登录已失效", AuthException.STATUS_UNAUTHORIZED),

    /** 兜底：未预期的服务端异常，不暴露内部细节。 */
    INTERNAL_ERROR("INTERNAL_ERROR", "服务异常，请稍后重试",
            AuthException.STATUS_INTERNAL_ERROR),

    // ==================== 登录 ====================

    /** 账号或密码为空 / 错误。 */
    BAD_CREDENTIALS("BAD_CREDENTIALS", "用户名或密码错误", AuthException.STATUS_UNAUTHORIZED),

    /** 账号被站长禁用。 */
    USER_DISABLED("USER_DISABLED", "账号已被禁用", AuthException.STATUS_UNAUTHORIZED),

    /** 账号开启二次验证，须改用微信登录。 */
    TWO_FACTOR_REQUIRED("TWO_FACTOR_REQUIRED", "该账号已开启二次验证，请使用微信登录",
            AuthException.STATUS_UNAUTHORIZED),

    /** 账号密码登录未开启（站点配置）。 */
    PASSWORD_LOGIN_DISABLED("PASSWORD_LOGIN_DISABLED", "账号密码登录未开启",
            AuthException.STATUS_UNAUTHORIZED),

    /** 登录/绑定尝试过于频繁（限流闸门命中）。 */
    TOO_MANY_ATTEMPTS("TOO_MANY_ATTEMPTS", "操作过于频繁，请稍后再试",
            AuthException.STATUS_TOO_MANY_REQUESTS),

    // ==================== 微信绑定 ====================

    /** 微信登录未开启（站点配置）。 */
    WECHAT_LOGIN_DISABLED("WECHAT_LOGIN_DISABLED", "微信登录未开启",
            AuthException.STATUS_UNAUTHORIZED),

    /** 微信侧失败：code 无效/已使用、响应解析失败等。 */
    WECHAT_LOGIN_FAILED("WECHAT_LOGIN_FAILED", "微信登录失败，请稍后重试",
            AuthException.STATUS_UNAUTHORIZED),

    /**
     * 站点尚未配置微信密钥（配置类问题，站长可自助修复）。
     *
     * <p>用 403 而非 401：这不是「你没通过认证」，而是「站点还没配好，谁都登不了」，
     * 与 {@link #REGISTER_FORBIDDEN} 同语义，客户端不该引导用户重试。
     */
    WECHAT_NOT_CONFIGURED("WECHAT_NOT_CONFIGURED", "微信登录未配置，请联系站长",
            AuthException.STATUS_FORBIDDEN),

    /**
     * 该微信已绑定其他账号（一对一约束：一个微信只能绑一个账号）。
     *
     * <p>409 而非 400：冲突的是「资源唯一性」，客户端应引导用户换微信或先解绑，
     * 而不是当成参数错误重试。文案刻意不回显占用方的账号名，防账号枚举。
     */
    WECHAT_ALREADY_BOUND("WECHAT_ALREADY_BOUND", "该微信已绑定其他账号，请先解绑后重试",
            AuthException.STATUS_CONFLICT),

    /**
     * 当前账号已绑定另一个微信（一对一约束：一个账号只能绑一个微信）。
     *
     * <p>与 {@link #WECHAT_ALREADY_BOUND} 方向相反：前者是「微信被别人占了」，
     * 这里是「这个账号已经占了别的微信」。两者分开便于客户端给出不同引导。
     */
    ACCOUNT_ALREADY_BOUND("ACCOUNT_ALREADY_BOUND", "当前账号已绑定其他微信，请先解除绑定",
            AuthException.STATUS_CONFLICT),

    // ==================== 扫码绑定票据 ====================

    /** 绑定票据不存在 / 已过期 / 已使用 / 重复扫码。 */
    BIND_TICKET_INVALID("BIND_TICKET_INVALID", "二维码已失效，请重新生成",
            AuthException.STATUS_BAD_REQUEST),

    /**
     * 扫码时手机端已登录<b>另一个</b>账号（票据归属与来访身份不一致）。
     *
     * <p>409 而非 400：冲突的是「扫码绑定要服务哪个账号」这一资源归属。
     * 文案必须给出出口——用户真正想要的多半是给手机上这个账号绑微信，
     * 应引导去 App 内绑定，而不是让他反复扫码。
     *
     * <p>本拒绝发生在票据消费之前，用户退出登录后同一个二维码仍可使用。
     */
    BIND_SIGNED_IN_OTHER_ACCOUNT("BIND_SIGNED_IN_OTHER_ACCOUNT",
            "当前已登录其他账号，扫码绑定只能用于发起二维码的账号；请退出登录后重新扫码，"
                    + "或在 App「我的 - 个人资料」中直接绑定微信",
            AuthException.STATUS_CONFLICT),

    // ==================== 注册 ====================

    /** 用户名已被占用。 */
    USERNAME_EXISTS("USERNAME_EXISTS", "用户名已被占用，换一个试试",
            AuthException.STATUS_BAD_REQUEST),

    /** 用户名或昵称不符合 Halo 命名规范。 */
    NAME_RESTRICTED("NAME_RESTRICTED", "用户名或昵称不符合规范",
            AuthException.STATUS_BAD_REQUEST),

    /** 邮箱验证码无效或已过期。 */
    EMAIL_CODE_INVALID("EMAIL_CODE_INVALID", "邮箱验证码无效或已过期",
            AuthException.STATUS_BAD_REQUEST),

    /** 邮箱已被其他账号占用。 */
    EMAIL_ALREADY_TAKEN("EMAIL_ALREADY_TAKEN", "该邮箱已被其他账号占用",
            AuthException.STATUS_BAD_REQUEST),

    /** 未同意注册协议。 */
    AGREEMENT_REQUIRED("AGREEMENT_REQUIRED", "请先阅读并同意用户协议",
            AuthException.STATUS_BAD_REQUEST),

    /** 站点未开放注册 / 未配置默认角色（配置类，站长可修复）。 */
    REGISTER_FORBIDDEN("REGISTER_FORBIDDEN", "未开放新用户注册",
            AuthException.STATUS_FORBIDDEN),

    /**
     * 微信一键注册被邮箱验证拦截：随错误下发补邮箱注册票据（{@code data.ticket}），
     * 客户端凭票据 + 邮箱 + 验证码走补邮箱注册接口完成注册。
     */
    WECHAT_EMAIL_REQUIRED("WECHAT_EMAIL_REQUIRED",
            "已开启验证邮箱，请补充邮箱完成注册", AuthException.STATUS_BAD_REQUEST),

    /** 注册失败（用户名候选全部占用或内部错误）。 */
    REGISTER_FAILED("REGISTER_FAILED", "注册失败，请稍后重试",
            AuthException.STATUS_UNAUTHORIZED),

    // ==================== 账号设置 ====================

    /** 用户不存在。 */
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在", AuthException.STATUS_UNAUTHORIZED),

    /** 已自主设置过密码，免旧密码通道关闭。 */
    PASSWORD_ALREADY_SET("PASSWORD_ALREADY_SET", "密码已设置，请使用旧密码修改",
            AuthException.STATUS_FORBIDDEN);

    private final String code;

    private final String message;

    private final int status;

    BizErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    /** 以默认文案构造异常。 */
    public AuthException toException() {
        return new AuthException(code, message, status);
    }

    /** 以自定义文案（动态内容）构造异常，code 与 status 不变。 */
    public AuthException toException(String message) {
        return new AuthException(code, message, status);
    }
}
