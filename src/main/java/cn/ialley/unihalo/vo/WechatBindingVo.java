package cn.ialley.unihalo.vo;

import java.time.Instant;

/**
 * 用户详情「微信绑定」选项卡的数据。
 *
 * <p>{@code providerUserId} 是微信侧的 openid / unionid。这里输出原值而不脱敏：
 * 该接口位于 console 组，访问受 RBAC 保护，而管理员排查「用户说微信登不上」时
 * 恰恰需要拿这个标识去核对；openid 本身不能用于登录，泄露风险有限。</p>
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
