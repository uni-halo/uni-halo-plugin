package cn.ialley.unihalo.services;

import reactor.core.publisher.Mono;
import cn.ialley.unihalo.services.BindTicketService;
import cn.ialley.unihalo.vo.LoginResult;
import cn.ialley.unihalo.vo.ProfileVo;
import cn.ialley.unihalo.vo.RegisterForm;
import cn.ialley.unihalo.vo.TokenCheckVo;
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
     * 微信一键注册：与 {@link #loginByWechat} 同源同语义，已绑定则直接登录，
     * 未绑定则自动建号（建号走 {@code signUp}，注册开关关闭时 fail closed 拒绝）。
     *
     * @param code wx.login() 得到的临时登录凭证（一次性）
     */
    Mono<LoginResult> registerByWechat(String code);

    /**
     * 微信补邮箱注册（第二段）：站点开启「注册必须验证邮箱」后，微信一键注册会以
     * {@code WECHAT_EMAIL_REQUIRED} 拒绝并下发 HMAC 票据（30 分钟），客户端补齐
     * 邮箱与验证码后凭票据调用本方法完成注册并登录。
     *
     * <p>服务端会用 {@code code} 重新换取微信身份并与票据完整比对（openid 必须一致，
     * 票据含 unionid 时还须与本次接口返回一致），防止票据被截获后
     * 由其他微信冒名注册；邮箱验证码由客户端在注册前经 Halo 匿名端点发往新邮箱，
     * {@code signUp} 内部校验验证码有效性及与邮箱一致。
     *
     * @param ticket    一键注册被拦时下发的注册票据
     * @param email     用户填写的邮箱
     * @param emailCode 发往该邮箱的验证码
     * @param code      重新获取的 wx.login() 临时登录凭证（一次性，用于二次校验身份）
     */
    Mono<LoginResult> registerByWechatEmail(String ticket, String email, String emailCode,
            String code);

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
     * 查询票据归属用户名，仅供「小程序端登录态预检」比对使用，<b>禁止</b>回显给客户端
     * （status 接口匿名可调，返回归属用户名等于凭一张二维码照片探测站内用户名）。
     *
     * @param ticket 票据号；票据不存在或已过期未消费时为空 Mono
     */
    Mono<String> bindTicketOwner(String ticket);

    /**
     * 小程序端扫码（第一阶段）：换出微信身份并登记到票据，<b>不建立绑定</b>。
     *
     * <p>真正的绑定要等 PC 端 {@link #approveBindTicket} 确认。拆成两阶段的理由见
     * {@link BindTicketService}：票据必然暴露在二维码里，扫码即绑定等于把
     * 「拿到二维码」等价于「可以绑走这个账号」。
     *
     * <p>登录态校验放在这里、且<b>在消费票据之前</b>：手机端登录着 B 却扫了 A 的码时，
     * 直接拒绝并保留票据（用户退出登录后同一个码还能用），而不是白废一个二维码。
     *
     * @param ticket   票据号
     * @param code     wx.login() 得到的临时登录凭证（换取微信身份）
     * @param visitor  小程序端当前登录的 Halo 用户名；未登录/匿名传 null
     */
    Mono<Void> scanBindTicket(String ticket, String code, String visitor);

    /**
     * PC 端确认绑定（第二阶段）：把扫码时暂存的微信身份绑定到票据锁定的账号。
     *
     * <p>确认者必须是票据创建者本人（服务端按 {@code username} 与票据归属比对），
     * 防止他人代确认。成功后发站内通知并落 {@code CONFIRMED} 终态。
     *
     * @param ticket   票据号
     * @param username 当前登录的 UC 用户（票据归属者）
     */
    Mono<Void> approveBindTicket(String ticket, String username);

    /**
     * PC 端拒绝本次扫码（用户看到扫码提示后点「取消」）。
     *
     * @param ticket   票据号
     * @param username 当前登录的 UC 用户（票据归属者）
     */
    Mono<Void> rejectBindTicket(String ticket, String username);

    /**
     * 吊销当前令牌（置 revoked，不影响 JWT 本身）。
     *
     * @param patName  PAT 扩展名
     * @param username 归属用户
     */
    Mono<Void> logout(String patName, String username);

    /**
     * 探测当前令牌是否仍有效（App 端启动 / 回前台时调用）。
     *
     * 能进入本方法说明认证链已通过 JWT 验签与 PAT 存在性校验——无效令牌
     * （验签失败 / exp 过期 / 扩展被互踢删除）在过滤器层就不成立，请求会以
     * 匿名身份到达端点，由端点侧 {@code currentUser()} 拒绝并收敛为 401。
     * 此处再做三道显式兜底（fail closed），防止官方认证链语义变化导致漏判：
     * PAT 扩展缺失、归属人不符、已吊销（revoked）或已过 {@code spec.expiresAt}
     * 均按 401 拒绝。
     *
     * @param username 当前登录用户（匿名请求在端点侧已被拒绝，不会进入）
     * @param patName  PAT 扩展名；为空表示非令牌登录（如浏览器会话 Cookie），
     *                 认证链放行即有效，无令牌元数据可回传
     */
    Mono<TokenCheckVo> tokenCheck(String username, String patName);

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
