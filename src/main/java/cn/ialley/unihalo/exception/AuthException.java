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

    /** 请求过于频繁（限流）。 */
    public static final int STATUS_TOO_MANY_REQUESTS = 429;

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
}
