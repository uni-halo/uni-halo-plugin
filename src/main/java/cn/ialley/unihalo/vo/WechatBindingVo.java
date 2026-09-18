package cn.ialley.unihalo.vo;

import java.time.Instant;

/**
 * 用户详情「微信绑定」选项卡的数据。
 *
 * {@code providerUserId} 是微信侧 openid/unionid，输出原值不脱敏：本接口在 console
 * 组受 RBAC 保护，且管理员排查「微信登不上」正需要此标识核对；openid 不能用于登录，
 * 泄露风险有限。
 *
 * @param username       Halo 用户名
 * @param bound          是否已绑定微信
 * @param providerUserId 绑定的微信标识（openid 或 unionid），未绑定为 null
 * @param boundAt        绑定关系最近一次更新时间，未绑定为 null
 * @author 小莫唐尼
 */
public record WechatBindingVo(
        String username,
        boolean bound,
        String providerUserId,
        Instant boundAt
) {

    public static WechatBindingVo unbound(String username) {
        return new WechatBindingVo(username, false, null, null);
    }
}
