package cn.ialley.unihalo.exception;

import lombok.Getter;

/**
 * 登录认证业务异常。
 *
 * 对外返回 {@code status} 与 {@code code}，便于客户端按 code 走不同分支
 * （如 {@code TOO_MANY_ATTEMPTS} 提示稍后再试、{@code TWO_FACTOR_REQUIRED} 提示改用微信登录）。
 * 消息面向终端用户，不得包含凭据或内部细节。
 *
 * 状态码默认 401（凭据/身份类问题）；限流用 429，让客户端与网关能区分
 * 「你错了」和「你太频繁了」—— 前者不该重试，后者应当退避重试。
 *
 * @author 小莫唐尼
 */
@Getter
public class AuthException extends RuntimeException {

    /** 认证失败（默认）。 */
    public static final int STATUS_UNAUTHORIZED = 401;

    /** 已识别但被拒绝（如配置不允许）。 */
    public static final int STATUS_FORBIDDEN = 403;

    /** 请求参数/状态无效（如票据过期）。 */
    public static final int STATUS_BAD_REQUEST = 400;

    /**
     * 资源冲突（唯一性约束被破坏）。
     *
     * 用于「一对一」类约束：同一个微信绑第二个账号、同一个账号绑第二个微信。
     * 与 400 的区别是 —— 400 是「你这请求不对，改改再来」，409 是「请求没问题，
     * 但和已有状态冲突，得先处理掉冲突那一方」。
     */
    public static final int STATUS_CONFLICT = 409;

    /** 请求过于频繁（限流）。 */
    public static final int STATUS_TOO_MANY_REQUESTS = 429;

    /** 兜底：未预期的服务端异常。 */
    public static final int STATUS_INTERNAL_ERROR = 500;

    private final String code;

    private final int status;

    public AuthException(String code, String message) {
        this(code, message, STATUS_UNAUTHORIZED);
    }

    public AuthException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /** 按业务错误码总表构造（推荐入口，避免散写字符串常量）。 */
    public AuthException(BizErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getStatus());
    }

    /** 按业务错误码总表构造并覆盖文案（动态内容，code 与 status 不变）。 */
    public AuthException(BizErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, errorCode.getStatus());
    }
}
