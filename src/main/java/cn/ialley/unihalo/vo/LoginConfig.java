package cn.ialley.unihalo.vo;

import java.util.Locale;

import cn.ialley.unihalo.constants.Constants;

/**
 * 移动端登录配置（设置页 loginConfig 组）。
 *
 * {@code wechatSecretName} 只保存 Secret 资源名称，不含密钥明文，真实凭据由服务端读取。
 * 密码登录与微信登录是两条独立能力，无「登录能力总开关」，两个开关全关即整体不可用；
 * 微信一键登录固定为「已绑定则登录、未绑定则自动注册并登录」，老用户复用已有账号走
 * {@code /auth/bind/wechat} 主动关联。注册策略沿用 Halo 系统设置（允许注册 + 默认角色），
 * 令牌权限恒等于用户在 Halo 已有的角色，不提权也不裁剪。
 *
 * @param passwordLoginEnabled 账号密码登录开关
 * @param wechatLoginEnabled   微信一键登录开关
 * @param wechatSecretName     微信凭据所在的 Secret 资源名
 * @param wechatUsernamePrefix 微信自动注册的用户名前缀，仅 prefix_seq 类型生效
 * @param wechatUsernameType   自动注册用户名类型：prefix_seq / uuid / hash
 * @param tokenTtlDays         令牌有效期（天）
 * @author 小莫唐尼
 */
public record LoginConfig(
        boolean passwordLoginEnabled,
        boolean wechatLoginEnabled,
        String wechatSecretName,
        String wechatUsernamePrefix,
        String wechatUsernameType,
        int tokenTtlDays
) {

    public static LoginConfig defaults() {
        return new LoginConfig(true, false, null,
                Constants.DEFAULT_WECHAT_USERNAME_PREFIX,
                Constants.WECHAT_USERNAME_TYPE_PREFIX_SEQ, 30);
    }

    /** 用户名类型枚举值：前缀 + 两位序号（默认）。 */
    public static final String TYPE_PREFIX_SEQ = Constants.WECHAT_USERNAME_TYPE_PREFIX_SEQ;
    /** 用户名类型枚举值：uhu- + 随机 UUID 前 12 位。 */
    public static final String TYPE_UUID = Constants.WECHAT_USERNAME_TYPE_UUID;
    /** 用户名类型枚举值：uhu- + 微信身份哈希前 12 位（确定性，可复用原用户名）。 */
    public static final String TYPE_HASH = Constants.WECHAT_USERNAME_TYPE_HASH;

    /**
     * 归一化后的用户名类型：未知值一律回落到 {@link #TYPE_PREFIX_SEQ}，
     * 与前缀的回落策略一致——配置写错不至于让注册功能瘫痪。
     */
    public String usernameType() {
        if (TYPE_UUID.equals(wechatUsernameType) || TYPE_HASH.equals(wechatUsernameType)) {
            return wechatUsernameType;
        }
        return TYPE_PREFIX_SEQ;
    }

    /**
     * 归一化后的注册用户名前缀：空值、非法字符或长度越界一律回落到
     * {@link Constants#DEFAULT_WECHAT_USERNAME_PREFIX}。
     *
     * 回落而非抛错的原因：前缀写错不至于让整个微信注册功能瘫痪，
     * 设置页另有即时校验提示站长。序号固定占 2 位，故按 {@code prefix + 2} 预判总长度。
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
