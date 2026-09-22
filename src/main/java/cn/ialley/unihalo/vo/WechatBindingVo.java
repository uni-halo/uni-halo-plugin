package cn.ialley.unihalo.vo;

import java.time.Instant;

import cn.ialley.unihalo.utils.WechatIdentityMasker;

/**
 * 用户详情 / 个人中心「微信绑定」选项卡的数据。
 *
 * <p>{@code providerUserId} 是微信侧 openid/unionid，**默认只给脱敏值**
 * （{@link WechatIdentityMasker}）：它是该微信在小程序下唯一且永久的标识，
 * 页面长期明文挂着没有业务必要，反而给社工与跨站关联提供素材。
 *
 * <p>排查确实需要原值时走 {@link #revealed}：由 Console 端点在收到显式
 * {@code reveal=true} 时构造，并必须记审计日志（见调用方）。原值不再随
 * 普通查询下发，避免「谁都能顺手看一眼」。
 *
 * @param username            Halo 用户名
 * @param bound               是否已绑定微信
 * @param providerUserId      脱敏后的微信标识（未绑定为 null）
 * @param providerUserIdFull  完整微信标识，仅显式查看时非 null，其余一律 null
 * @param boundAt             绑定关系最近一次更新时间，未绑定为 null
 * @author 小莫唐尼
 */
public record WechatBindingVo(
        String username,
        boolean bound,
        String providerUserId,
        String providerUserIdFull,
        Instant boundAt
) {

    public static WechatBindingVo unbound(String username) {
        return new WechatBindingVo(username, false, null, null, null);
    }

    /** 默认视图：只带脱敏标识。 */
    public static WechatBindingVo masked(String username, String rawIdentity, Instant boundAt) {
        return new WechatBindingVo(username, true, WechatIdentityMasker.mask(rawIdentity),
                null, boundAt);
    }

    /** 显式查看视图：脱敏值仍在，另附完整值（调用方已记审计日志）。 */
    public static WechatBindingVo revealed(String username, String rawIdentity, Instant boundAt) {
        return new WechatBindingVo(username, true, WechatIdentityMasker.mask(rawIdentity),
                rawIdentity, boundAt);
    }
}
