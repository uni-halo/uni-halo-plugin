package cn.ialley.unihalo.services;

import reactor.core.publisher.Mono;

/**
 * 微信小程序登录能力。
 *
 * <p>目标地址固定为 {@code https://api.weixin.qq.com}，不接受任何用户自定义基础地址，
 * 避免 SSRF；凭据只在目标确认后作为 query 参数附带。</p>
 *
 * @author 小莫唐尼
 */
public interface WechatService {

    /**
     * 用临时登录凭证换取会话。
     *
     * @param appId     小程序 AppID
     * @param appSecret 小程序 AppSecret
     * @param code      wx.login() 返回的 code（一次性）
     * @return openid / unionid
     */
    Mono<WechatSession> code2Session(String appId, String appSecret, String code);

    /**
     * 微信侧会话信息。
     *
     * @param openid  小程序内唯一标识
     * @param unionid 开放平台唯一标识，未绑定开放平台时为 null
     */
    record WechatSession(String openid, String unionid) {
    }
}
