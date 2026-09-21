package cn.ialley.unihalo.services;

import reactor.core.publisher.Mono;
import cn.ialley.unihalo.services.BindTicketService;
import cn.ialley.unihalo.vo.LoginResult;
import cn.ialley.unihalo.vo.ProfileVo;
import cn.ialley.unihalo.vo.RegisterForm;
import cn.ialley.unihalo.vo.WechatBindingVo;

/**
 * 移动端登录服务。
 *
 * 所有登录方式最终都收敛为「签发一枚 Halo 原生 PAT」，因此移动端可直连
 * Halo 原生 API，插件无需为每一项能力再包一层代理接口。
 *
 * 密码登录与微信登录是两条互相独立的能力：各自有开关，互不为前提。
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
     * 账号密码注册并登录（注册即登录）：中转 Halo 注册（{@code UserService.signUp}），
     * 成功后按登录同一出口签发 PAT，返回结构与 {@link #loginByPassword} 完全一致。
     *
     * 注册开关与策略沿用 Halo 系统设置（允许注册 / 默认角色 / 注册协议 / 注册邮箱验证），
     * 均由 {@code signUp} 内部 fail closed 校验；失败计入限流（与登录共用闸门）。
     *
     * @param form     注册表单
     * @param clientIp 来源 IP（TCP 源地址），用于限流；未知时传 null
     */
    Mono<LoginResult> registerByPassword(RegisterForm form, String clientIp);

    /**
     * 微信一键注册并登录（注册页按钮，兼做「注册 + 登录」）。
     *
     * 与 {@link #loginByWechat} 同源同语义：wx.login code 换身份，已绑定则直接登录，
     * 未绑定则自动建号（建号走 {@code signUp}，注册开关关闭时 fail closed 拒绝）。
     * 复用同一实现避免双路径漂移。
     *
     * @param code wx.login() 得到的临时登录凭证（一次性）
     */
    Mono<LoginResult> registerByWechat(String code);

    /**
     * 已登录用户绑定微信（老账号主动关联，绑定后可用微信一键登录进这个账号）。
     *
     * @param username 当前登录用户
     * @param code     wx.login() 得到的临时登录凭证
     */
    Mono<Void> bindWechat(String username, String code);

    /**
     * 为 UC 用户签发扫码绑定票据（UC 侧「扫码绑定微信」入口）。
     *
     * 二维码内容为 {@code Constants.QR_BIND_WECHAT_PREFIX + ticket}，
     * 小程序端按前缀识别并引导到确认页。
     *
     * @param username 创建票据时锁定的 UC 用户名
     */
    Mono<BindTicketService.IssuedTicket> createBindTicket(String username);

    /**
     * 查询扫码绑定票据状态（UC 侧轮询）。
     *
     * @param ticket 票据号
     */
    Mono<BindTicketService.TicketStatus> bindTicketStatus(String ticket);

    /**
     * 小程序端确认绑定：消费票据，把扫码微信身份绑定到票据创建时锁定的用户。
     *
     * @param ticket 票据号
     * @param code   wx.login() 得到的临时登录凭证（换取微信身份）
     */
    Mono<Void> confirmBindTicket(String ticket, String code);

    /**
     * 吊销当前令牌（置 revoked，不影响 JWT 本身）。
     *
     * @param patName  PAT 扩展名
     * @param username 归属用户
     */
    Mono<Void> logout(String patName, String username);

    /**
     * 当前登录用户的资料与权限。只读，不签发新令牌。
     */
    Mono<ProfileVo> profile(String username);

    /**
     * 查询指定用户的微信绑定状态（UC 侧「我的绑定」展示用；
     * 与 Console 管理端 wechat-users 接口不同，这里只需登录自身身份）。
     *
     * @param username 当前登录用户
     */
    Mono<WechatBindingVo> myWechatBinding(String username);

    /**
     * 解除当前登录用户的微信绑定（移动端「我的信息」页用，幂等：未绑定时同样成功）。
     * 仅删除 {@link UserConnection}，不删 Halo 用户，解绑后可重新绑定或改用密码登录。
     *
     * @param username 当前登录用户
     */
    Mono<Void> unbindMyWechat(String username);

    /**
     * 首次设置密码（免旧密码）：仅「从未自主设置过密码」的用户可调用，
     * 成功后打 {@code unihalo.ialley.cn/password-set-by-user} 注解，此后改密走原生旧密码流程。
     *
     * @param username    当前登录用户
     * @param newPassword 新密码明文
     */
    Mono<Void> setInitialPassword(String username, String newPassword);
}
