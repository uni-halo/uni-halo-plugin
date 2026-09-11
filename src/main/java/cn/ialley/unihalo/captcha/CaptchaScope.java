package cn.ialley.unihalo.captcha;

/**
 * 验证码生效范围（对应设置页 captchaConfig.scope 的子开关）。
 *
 * <p>接口侧按范围生效：总开关 {@code captchaConfig.enabled} 开启后，
 * 仅对 scope 开启的功能要求输入验证码（scope 缺省视为开启，行为与旧"总开关统一
 * 判定"一致）。</p>
 *
 * @author 小莫唐尼
 */
public enum CaptchaScope {

    /** 小程序链接申请提交（POST /mini-program-links/submissions，键 linkSubmission） */
    LINK_SUBMISSION("linkSubmission"),

    /** 加密恋爱相册解锁（POST /love-albums/{name}/unlock，键 loveAlbumUnlock） */
    LOVE_ALBUM_UNLOCK("loveAlbumUnlock"),

    /** 恋爱模块入口解锁（POST /love-modules/unlock，键 loveModuleUnlock；
     * 覆盖恋爱日记/恋爱故事/恋爱相册入口/恋爱清单等模块入口） */
    LOVE_MODULE_UNLOCK("loveModuleUnlock");

    private final String configKey;

    CaptchaScope(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
