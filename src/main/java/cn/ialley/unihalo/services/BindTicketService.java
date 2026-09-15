package cn.ialley.unihalo.services;

import java.time.Instant;

import reactor.core.publisher.Mono;

/**
 * 微信扫码绑定票据（BindTicket）服务。
 *
 * <p>PC 端 UC 用户（浏览器环境，无微信授权能力）通过扫码绑定微信：
 * 服务端先签发一次性票据并锁定 UC 用户名，二维码内容为
 * {@code uh-bindwx-{ticket}}（前缀约定见 Constants）；
 * 小程序端扫码确认后携带 {@code wx.login()} 的 code 调 confirm，
 * 服务端校验票据并把该微信身份绑定到票据创建时锁定的用户。</p>
 *
 * <p>安全约束：</p>
 * <ol>
 *   <li><b>单次消费</b>：confirm 成功即标记已用，重放返回失败；</li>
 *   <li><b>时效</b>：5 分钟过期，过期后只能重新生成；</li>
 *   <li><b>用户名锁定</b>：绑定目标在创建票据时已确定，confirm 不接受客户端指定，
 *       扫码者无法把微信绑到别的账号。</li>
 * </ol>
 *
 * @author 小莫唐尼
 */
public interface BindTicketService {

    /** 二维码内容状态。 */
    enum Status {
        /** 等待扫码确认 */
        PENDING,
        /** 已确认绑定 */
        CONFIRMED,
        /** 已过期（未在有效期内确认） */
        EXPIRED
    }

    /**
     * 轮询返回的状态视图（不暴露绑定用户名——轮询方就是创建者本人，
     * 但状态接口仍按最小披露原则只给状态）。
     */
    record TicketStatus(String ticket, Status status) {
    }

    /**
     * confirm 的消费结果：成功时携带创建票据时锁定的用户名。
     */
    record ConsumeResult(boolean success, String username, String reason) {

        public static ConsumeResult ok(String username) {
            return new ConsumeResult(true, username, null);
        }

        public static ConsumeResult fail(String reason) {
            return new ConsumeResult(false, null, reason);
        }
    }

    /**
     * 为指定 UC 用户签发绑定票据。
     *
     * @param username 创建票据时锁定的 Halo 用户名（metadata.name）
     * @return 票据号与过期时间
     */
    Mono<IssuedTicket> issue(String username);

    /**
     * 票据签发结果。
     */
    record IssuedTicket(String ticket, Instant expiresAt) {
    }

    /**
     * 查询票据当前状态（轮询用，不改变状态）。
     */
    Mono<TicketStatus> status(String ticket);

    /**
     * 消费票据（confirm 时调用，原子单次）。
     *
     * <p>成功返回 {@code success=true} 并携带锁定用户名；票据不存在、已过期、
     * 已消费均返回失败（reason 面向小程序端展示）。</p>
     */
    Mono<ConsumeResult> consume(String ticket);
}
