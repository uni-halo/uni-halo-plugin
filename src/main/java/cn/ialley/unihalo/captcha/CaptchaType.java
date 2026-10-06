package cn.ialley.unihalo.captcha;

/**
 * 验证码类型（区分图形等不同呈现方式）。
 *
 * @author 小莫唐尼
 */
public enum CaptchaType {

    /**
     * 图形字符验证码（可配长度）
     */
    ALPHANUMERIC,

    /**
     * 算术验证码（可配范围）
     */
    ARITHMETIC
}
