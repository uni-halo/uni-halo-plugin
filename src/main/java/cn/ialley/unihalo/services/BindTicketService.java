package cn.ialley.unihalo.services;

import java.time.Instant;

import reactor.core.publisher.Mono;

/**
 * 微信扫码绑定票据（BindTicket）服务：两阶段确认模型。
 *
 * <p>PC 端 UC 用户（浏览器无微信授权能力）扫码绑定微信：服务端签发一次性票据并锁定
 * UC 用户名（二维码内容 {@code uh-bindwx-{ticket}}），小程序端扫码后<b>只登记微信身份</b>，
 * 真正的绑定要等 PC 端在弹窗里<b>确认</b>之后才执行。
 *
 * <p>为什么要两阶段（而不是扫码即绑定）：
 * <ul>
 * <li><b>票据泄露不等于账号接管</b>：ticket 必然出现在二维码里（屏幕共享、截图外传、
 *     肩窥都能拿到），confirm 又是匿名接口。一阶段模型下，任何人拿到 ticket 就能用
 *     自己的微信绑到受害者账号。两阶段把危害收敛为「必须受害者本人再点一次确认」。</li>
 * <li><b>防止扫码者绑错账号</b>：手机号登录着 B、却扫了 A 的码时，一阶段模型会静默
 *     把微信绑给 A。两阶段让 A 的持有者看到「有人扫码（尾号 xxxx）」并自行决定。</li>
 * </ul>
 *
 * <p>状态机：{@code PENDING →（scan）SCANNED →（approve）处理中 → CONFIRMED / FAILED}，
 * 未确认且超时为 {@code EXPIRED}，PC 端主动拒绝为 {@code FAILED}（带 reason）。
 *
 * @author 小莫唐尼
 */
public interface BindTicketService {

    /** 二维码内容状态。 */
    enum Status {
        /** 已生成，等待扫码 */
        PENDING,
        /**
         * 已被扫码，等待 PC 端确认（此阶段尚未绑定，可确认也可拒绝）。
         *
         * <p>轮询方（UC 弹窗）据此展示扫码方标识尾号与确认/拒绝按钮。
         */
        SCANNED,
        /** PC 端已确认且绑定成功 */
        CONFIRMED,
        /** 绑定失败或被 PC 端拒绝（{@code reason} 面向用户） */
        FAILED,
        /** 已过期（未在有效期内确认） */
        EXPIRED
    }

    /**
     * 轮询返回的状态视图（不暴露绑定用户名——轮询方就是创建者本人，
     * 但状态接口仍按最小披露原则只给状态）。
     *
     * @param reason 失败原因文案（仅 {@link Status#FAILED} 有值，其余为 null）
     * @param hint   扫码方微信标识的<b>脱敏</b>尾号（仅 {@link Status#SCANNED} 有值），
     *               给 PC 端一个「是谁在扫」的核对依据，不给完整 openid
     */
    record TicketStatus(String ticket, Status status, String reason, String hint) {
    }

    /** scan 的结果：成功时携带脱敏尾号供展示。 */
    record ScanResult(boolean success, String reason, String hint) {

        public static ScanResult ok(String hint) {
            return new ScanResult(true, null, hint);
        }

        public static ScanResult fail(String reason) {
            return new ScanResult(false, reason, null);
        }
    }

    /** approve 的结果：成功时携带扫码方暂存下来的微信标识（用于真正建立绑定）。 */
    record ApproveResult(boolean success, String reason, String identity) {

        public static ApproveResult ok(String identity) {
            return new ApproveResult(true, null, identity);
        }

        public static ApproveResult fail(String reason) {
            return new ApproveResult(false, null, null);
        }

        public static ApproveResult fail(String reason, String identity) {
            return new ApproveResult(false, reason, identity);
        }
    }

    /**
     * 为指定 UC 用户签发绑定票据。
     *
     * @param username 创建票据时锁定的 Halo 用户名（metadata.name）
     * @return 票据号与过期时间
     */
    Mono<IssuedTicket> issue(String username);

    /** 票据签发结果。 */
    record IssuedTicket(String ticket, Instant expiresAt) {
    }

    /** 查询票据当前状态（轮询用，不改变状态）。 */
    Mono<TicketStatus> status(String ticket);

    /**
     * 取票据锁定的用户名（只读，不改变状态、不消费）。
     *
     * <p>用途：confirm 前先比对「扫码者登录身份」与「票据归属」，不一致直接拒绝
     * —— 这一步必须发生在消费之前，否则误操作会把票据白白作废。
     */
    Mono<String> owner(String ticket);

    /**
     * 登记扫码（第一阶段）：暂存扫码方的微信标识，票据进入 {@link Status#SCANNED}。
     *
     * <p><b>不建立绑定关系</b>。票据不存在 / 已过期 / 已被扫 / 已终态均返回失败。
     *
     * @param identity 由 code2Session 换得的微信标识（openid 或 unionid）
     */
    Mono<ScanResult> scan(String ticket, String identity);

    /**
     * 确认绑定（第二阶段）：校验归属与已扫码状态后，交回暂存的微信标识。
     *
     * <p>成功只说明「可以绑定了」，终态仍由 {@link #confirm} / {@link #fail} 落定；
     * 票据在此被消费（拒绝重复确认）。
     *
     * @param username 必须是票据创建时锁定的用户（防止他人代确认）
     */
    Mono<ApproveResult> approve(String ticket, String username);

    /**
     * PC 端拒绝本次扫码：校验归属后落 {@link Status#FAILED}。
     *
     * @return 是否成功落定（票据不存在 / 已终态 / 非本人为 false）
     */
    Mono<Boolean> reject(String ticket, String username, String reason);

    /** 落「绑定成功」终态。 */
    Mono<Void> confirm(String ticket);

    /** 落「绑定失败」终态并记下原因文案。 */
    Mono<Void> fail(String ticket, String reason);
}
