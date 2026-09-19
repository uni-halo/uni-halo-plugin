package cn.ialley.unihalo.vo;

/**
 * 移动端账号密码注册表单（{@code POST /auth/register} 请求体）。
 *
 * 字段对齐 Halo {@code SignUpData}：注册开关（系统设置「允许注册」）、用户名/昵称限制、
 * 默认角色、注册协议（requiredAgreementPages 时必须 agreedToTerms）与注册邮箱验证
 * （mustVerifyEmailOnRegistration 时必须 email + emailCode）均由
 * {@code UserService.signUp} 内部校验（fail closed），本模型仅做透传；
 * confirmPassword 的一致性在插件端先行把关，给出面向用户的中文提示。
 *
 * @param username        用户名（4-63 位，字母数字）
 * @param displayName     昵称（必填）
 * @param password        密码（≥5 位）
 * @param confirmPassword 确认密码（须与 password 一致）
 * @param email           邮箱（站点开启注册邮箱验证时必填）
 * @param emailCode       邮箱验证码（站点开启注册邮箱验证时必填）
 * @param agreedToTerms   是否同意注册协议（站点配置了注册协议时必须为 true）
 * @author 小莫唐尼
 */
public record RegisterForm(
        String username,
        String displayName,
        String password,
        String confirmPassword,
        String email,
        String emailCode,
        Boolean agreedToTerms
) {
}
