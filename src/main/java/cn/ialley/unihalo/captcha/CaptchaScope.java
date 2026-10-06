package cn.ialley.unihalo.captcha;

/**
 * 验证码生效范围（对应设置页 captchaConfig.scope 的子开关）。
 *
 * 接口侧按范围生效：总开关 {@code captchaConfig.enabled} 开启后，
 * 仅对 scope 开启的功能要求输入验证码；scope 子键缺省时按各 scope 的
 * {@code defaultEnabled} 取值（login 缺省关闭以兼容旧版客户端，其余缺省开启）。
 *
 * @author 小莫唐尼
 */
public enum CaptchaScope {

    /** 小程序链接申请提交（POST /mini-program-links/submissions，键 linkSubmission） */
    LINK_SUBMISSION("linkSubmission", true),

    /** 加密恋爱相册解锁（POST /love-albums/{name}/unlock，键 loveAlbumUnlock） */
    LOVE_ALBUM_UNLOCK("loveAlbumUnlock", true),

    /** 恋爱模块入口解锁（POST /love-modules/-/unlock，键 loveModuleUnlock；
     * 覆盖恋爱日记/恋爱故事/恋爱相册入口/恋爱清单等模块入口） */
    LOVE_MODULE_UNLOCK("loveModuleUnlock", true),

    /** 注册邮箱验证码发送（POST /auth/-/send-register-email-code，键 registerEmailCode） */
    REGISTER_EMAIL_CODE("registerEmailCode", true),

    /** 密码重置邮箱验证码发送（POST /auth/-/send-reset-email-code，键 resetEmailCode） */
    RESET_EMAIL_CODE("resetEmailCode", true),

    /** 账号密码登录（POST /auth/-/login，键 login；缺省关闭——登录高频且旧版客户端不带验证码） */
    LOGIN("login", false);

    private final String configKey;
    private final boolean defaultEnabled;

    CaptchaScope(String configKey, boolean defaultEnabled) {
        this.configKey = configKey;
        this.defaultEnabled = defaultEnabled;
    }

    public String configKey() {
        return configKey;
    }

    /** scope 配置缺省时该功能是否要求验证码（存量配置快照无子键时生效）。 */
    public boolean defaultEnabled() {
        return defaultEnabled;
    }
}
