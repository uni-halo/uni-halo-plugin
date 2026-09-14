package cn.ialley.unihalo.vo;

import java.util.Locale;

import cn.ialley.unihalo.constants.Constants;

/**
 * 移动端登录配置（设置页 loginConfig 组）。
 *
 * <p>{@code wechatSecretName} 只保存 Secret 资源名称，不含任何密钥明文；
 * 真实凭据由 {@code LoginConfigResolver} 在服务端按名称读取。</p>
 *
 * <p>密码登录与微信登录是两条互相独立的能力，各自有自己的开关，不存在从属关系，
 * 也没有「登录能力总开关」——两个开关全关即登录能力整体不可用。
 * 微信一键登录固定为「已绑定则登录、未绑定则自动注册并登录」，没有「必须先注册」的中间态；
 * 老用户想复用已有账号时走 {@code /auth/bind/wechat} 主动关联。</p>
 *
 * <p>注册策略完全沿用 Halo 系统设置-用户设置：能否注册由「允许注册」决定，
 * 新用户角色由「默认角色」决定；令牌权限恒等于用户在 Halo 已有的角色（不提权也不裁剪）。</p>
 *
 * @param passwordLoginEnabled 账号密码登录开关
 * @param wechatLoginEnabled   微信一键登录开关
 * @param wechatSecretName     微信凭据所在的 Secret 资源名
 * @param wechatUsernamePrefix 微信自动注册的用户名前缀，最终用户名 = 前缀 + 两位序号
 * @param tokenTtlDays         令牌有效期（天）
 * @author 小莫唐尼
 */
public record LoginConfig(
        boolean passwordLoginEnabled,
        boolean wechatLoginEnabled,
        String wechatSecretName,
        String wechatUsernamePrefix,
        int tokenTtlDays
) {

    public static LoginConfig defaults() {
        return new LoginConfig(true, false, null,
                Constants.DEFAULT_WECHAT_USERNAME_PREFIX, 30);
    }

    /**
     * 归一化后的注册用户名前缀：空值、非法字符或长度越界一律回落到
     * {@link Constants#DEFAULT_WECHAT_USERNAME_PREFIX}。
     *
     * <p>回落而非抛错的原因：前缀写错不至于让整个微信注册功能瘫痪，
     * 设置页另有即时校验提示站长。序号固定占 2 位，故按 {@code prefix + 2} 预判总长度。</p>
     */
    public String usernamePrefix() {
        var prefix = wechatUsernamePrefix == null
                ? Constants.DEFAULT_WECHAT_USERNAME_PREFIX
                : wechatUsernamePrefix.trim().toLowerCase(Locale.ROOT);
        int totalLength = prefix.length() + 2;
        if (prefix.isEmpty()
                || !prefix.matches(Constants.USERNAME_PREFIX_REGEX)
                || totalLength < Constants.USERNAME_MIN_LENGTH
                || totalLength > Constants.USERNAME_MAX_LENGTH) {
            return Constants.DEFAULT_WECHAT_USERNAME_PREFIX;
        }
        return prefix;
    }
}
