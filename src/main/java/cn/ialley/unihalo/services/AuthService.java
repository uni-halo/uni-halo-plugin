package cn.ialley.unihalo.services;

import reactor.core.publisher.Mono;
import cn.ialley.unihalo.vo.LoginResult;
import cn.ialley.unihalo.vo.ProfileVo;

/**
 * 移动端登录服务。
 *
 * <p>所有登录方式最终都收敛为「签发一枚 Halo 原生 PAT」，因此移动端可直连
 * Halo 原生 API，插件无需为每一项能力再包一层代理接口。</p>
 *
 * <p>密码登录与微信登录是两条互相独立的能力：各自有开关，互不为前提。</p>
 *
 * @author 小莫唐尼
 */
public interface AuthService {

    /**
     * 账号密码登录。失败会计入限流，达到阈值后返回 429。
     *
     * @param username    Halo 用户名
     * @param rawPassword 明文密码
     * @param clientIp    来源 IP（TCP 源地址），用于限流；未知时传 null
     */
    Mono<LoginResult> loginByPassword(String username, String rawPassword, String clientIp);

    /**
     * 微信小程序一键登录：已绑定则登录，未绑定则自动建号并登录。
     *
     * @param code wx.login() 得到的临时登录凭证（一次性）
     */
    Mono<LoginResult> loginByWechat(String code);

    /**
     * 已登录用户绑定微信（老账号主动关联，绑定后可用微信一键登录进这个账号）。
     *
     * @param username 当前登录用户
     * @param code     wx.login() 得到的临时登录凭证
     */
    Mono<Void> bindWechat(String username, String code);

    /**
     * 吊销当前令牌（置 revoked，不影响 JWT 本身）。
     *
     * @param patName  PAT 扩展名
     * @param username 归属用户
     */
    Mono<Void> logout(String patName, String username);

    /**
     * 当前登录用户的资料与权限。只读，<b>不签发新令牌</b>。
     */
    Mono<ProfileVo> profile(String username);
}
