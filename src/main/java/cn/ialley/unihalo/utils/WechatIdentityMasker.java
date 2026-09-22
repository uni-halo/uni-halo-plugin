package cn.ialley.unihalo.utils;

/**
 * 微信标识（openid / unionid）脱敏。
 *
 * <p>为什么脱敏：openid 是微信在该小程序下的<b>唯一且永久</b>用户标识，一旦泄露，
 * 拿到的人虽然不能直接用它登录（登录必须过 wx.login 换 code），但可以用它做
 * 跨站关联、社工定向，以及核对「某个人是不是注册了某站」。管理员排查单个用户
 * 时并不需要在页面上长期明文挂着它，所以默认给掩码，需要时再显式查看。
 *
 * <p>掩码规则：保留前 6 位与后 4 位，中间固定 6 个星号 ——
 * 中间长度<b>固定</b>是为了不泄露真实长度（openid 与 unionid 长度不同），
 * 前 6 + 后 4 足够人工核对「是不是同一个微信」，又不足以还原。
 *
 * @author 小莫唐尼
 */
public final class WechatIdentityMasker {

    private static final int HEAD = 6;

    private static final int TAIL = 4;

    /** 中间替换段（固定长度，不随原文长度变化）。 */
    private static final String MIDDLE = "******";

    /** 短到连首尾都遮不住时，全部打码。 */
    private static final int MIN_LENGTH = HEAD + TAIL + 1;

    private WechatIdentityMasker() {
    }

    /**
     * 脱敏；null / 空白原样返回 null（调用方按「未绑定」处理）。
     */
    public static String mask(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.length() < MIN_LENGTH) {
            return "*".repeat(raw.length());
        }
        return raw.substring(0, HEAD) + MIDDLE + raw.substring(raw.length() - TAIL);
    }
}
