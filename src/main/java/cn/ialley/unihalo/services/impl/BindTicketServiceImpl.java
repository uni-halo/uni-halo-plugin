package cn.ialley.unihalo.services.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.services.BindTicketService;
import cn.ialley.unihalo.utils.WechatIdentityMasker;

/**
 * 绑定票据内存实现（两阶段确认）。
 *
 * 票据生命周期极短（3 分钟）且站点级并发极低（同时扫绑的用户个位数），
 * 内存 {@link ConcurrentHashMap} 足够，无需落扩展存储；进程重启丢失的票据
 * 用户重新点一次按钮即可，无恢复价值。
 *
 * 容量上限参照 {@code CaptchaManager}（100）：超出先清理过期项，
 * 仍满则拒绝签发——防异常刷票撑爆内存。
 *
 * <p>状态机：{@code PENDING →（scan）SCANNED →（approve）处理中 → CONFIRMED / FAILED}，
 * 未确认且超时为 {@code EXPIRED}。所有状态迁移都走 {@link Map#compute} 原子完成，
 * 并发两个请求只有一个能推进状态。
 *
 * <p>安全边界：票据只暂存微信标识到确认完成（或过期清理）为止，
 * 落终态时立即清空 {@code scannedIdentity}，不长期驻留内存。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
public class BindTicketServiceImpl implements BindTicketService {

    /**
     * 票据有效期。两阶段确认后 PC 端还要点一次按钮，但 3 分钟足够走完
     * 「生成二维码 → 掏手机 → 扫 → 确认」；再长只会放大票据泄露窗口。
     */
    static final Duration TTL = Duration.ofMinutes(3);
    private static final int MAX_TICKETS = 100;
    private static final int TICKET_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    /**
     * 单条票据：锁定用户名、过期时间、消费标记、终态结果与扫码方标识。
     * 消费后保留到自然过期，让 UC 轮询能看到终态（否则只会看到 EXPIRED，无法区分成败）。
     */
    private record Ticket(String username, Instant expiresAt, boolean consumed,
            Status outcome, String reason, String scannedIdentity, String hint) {
    }

    @Override
    public Mono<IssuedTicket> issue(String username) {
        return Mono.fromSupplier(() -> {
            evictExpired();
            if (tickets.size() >= MAX_TICKETS) {
                throw new IllegalStateException("绑定请求过于频繁，请稍后再试");
            }
            var ticket = newTicket();
            var expiresAt = Instant.now().plus(TTL);
            tickets.put(ticket, new Ticket(username, expiresAt, false, null, null, null, null));
            return new IssuedTicket(ticket, expiresAt);
        });
    }

    @Override
    public Mono<TicketStatus> status(String ticket) {
        return Mono.fromSupplier(() -> {
            var t = tickets.get(ticket);
            if (t == null) {
                // 不存在 = 从未签发或已随过期清理；统一按过期处理，弹窗引导刷新即可。
                return new TicketStatus(ticket, Status.EXPIRED, null, null);
            }
            if (!t.consumed() && Instant.now().isAfter(t.expiresAt())) {
                return new TicketStatus(ticket, Status.EXPIRED, null, null);
            }
            // 已消费但还没落终态：绑定仍在处理中，继续轮询等待，不能提前报成功
            if (t.outcome() == null) {
                return new TicketStatus(ticket, Status.PENDING, null, null);
            }
            return new TicketStatus(ticket, t.outcome(), t.reason(), t.hint());
        });
    }

    @Override
    public Mono<String> owner(String ticket) {
        return Mono.fromSupplier(() -> {
            var t = tickets.get(ticket);
            if (t == null || (!t.consumed() && Instant.now().isAfter(t.expiresAt()))) {
                return null;
            }
            return t.username();
        });
    }

    @Override
    public Mono<ScanResult> scan(String ticket, String identity) {
        return Mono.fromSupplier(() -> {
            var result = new ScanResult[] {null};
            tickets.compute(ticket, (k, t) -> {
                if (t == null) {
                    result[0] = ScanResult.fail("绑定请求已失效，请重新生成二维码");
                    return null;
                }
                if (t.consumed() || t.outcome() != null) {
                    result[0] = ScanResult.fail("该二维码已被使用过，请重新生成");
                    return t;
                }
                if (Instant.now().isAfter(t.expiresAt())) {
                    result[0] = ScanResult.fail("二维码已过期，请重新生成");
                    return null;
                }
                var hint = WechatIdentityMasker.mask(identity);
                result[0] = ScanResult.ok(hint);
                return new Ticket(t.username(), t.expiresAt(), false, Status.SCANNED,
                        null, identity, hint);
            });
            return result[0];
        });
    }

    @Override
    public Mono<ApproveResult> approve(String ticket, String username) {
        return Mono.fromSupplier(() -> {
            var result = new ApproveResult[] {null};
            tickets.compute(ticket, (k, t) -> {
                if (t == null) {
                    result[0] = ApproveResult.fail("绑定请求已失效，请重新生成二维码");
                    return null;
                }
                if (!t.username().equals(username)) {
                    // 票据归属与确认者不一致：视为越权尝试，留痕但不回显归属账号
                    log.warn("【UniHalo】扫码绑定确认越权：票据不属于请求者，已拒绝");
                    result[0] = ApproveResult.fail("二维码已失效，请重新生成");
                    return t;
                }
                if (t.consumed() || t.outcome() == Status.CONFIRMED
                        || t.outcome() == Status.FAILED) {
                    result[0] = ApproveResult.fail("该二维码已被使用过，请重新生成");
                    return t;
                }
                if (Instant.now().isAfter(t.expiresAt())) {
                    result[0] = ApproveResult.fail("二维码已过期，请重新生成");
                    return null;
                }
                if (t.outcome() != Status.SCANNED) {
                    result[0] = ApproveResult.fail("尚未检测到扫码，请先在微信中确认");
                    return t;
                }
                result[0] = ApproveResult.ok(t.scannedIdentity());
                // 进入「处理中」：终态由 confirm / fail 落定
                return new Ticket(t.username(), t.expiresAt(), true, null, null, null,
                        t.hint());
            });
            return result[0];
        });
    }

    @Override
    public Mono<Boolean> reject(String ticket, String username, String reason) {
        return Mono.fromSupplier(() -> {
            var rejected = new boolean[] {false};
            tickets.computeIfPresent(ticket, (k, t) -> {
                if (!t.username().equals(username) || t.consumed()
                        || t.outcome() != Status.SCANNED) {
                    return t;
                }
                rejected[0] = true;
                // 拒绝即终态：清空暂存的微信标识，不再保有
                return new Ticket(t.username(), t.expiresAt(), true, Status.FAILED,
                        reason, null, null);
            });
            return rejected[0];
        });
    }

    @Override
    public Mono<Void> confirm(String ticket) {
        return settle(ticket, Status.CONFIRMED, null);
    }

    @Override
    public Mono<Void> fail(String ticket, String reason) {
        return settle(ticket, Status.FAILED, reason);
    }

    private Mono<Void> settle(String ticket, Status outcome, String reason) {
        return Mono.fromRunnable(() -> tickets.computeIfPresent(ticket, (k, t) ->
                // 终态一律清空 scannedIdentity：确认流程结束，不再需要暂存微信标识
                new Ticket(t.username(), t.expiresAt(), true, outcome, reason, null, null)))
                .then();
    }

    private String newTicket() {
        var bytes = new byte[TICKET_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void evictExpired() {
        var now = Instant.now();
        tickets.values().removeIf(t -> now.isAfter(t.expiresAt()));
    }
}
