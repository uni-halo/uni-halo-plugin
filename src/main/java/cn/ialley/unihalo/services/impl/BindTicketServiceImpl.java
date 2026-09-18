package cn.ialley.unihalo.services.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.services.BindTicketService;

/**
 * 绑定票据内存实现。
 *
 * 票据生命周期极短（5 分钟）且站点级并发极低（同时扫绑的用户个位数），
 * 内存 {@link ConcurrentHashMap} 足够，无需落扩展存储；进程重启丢失的票据
 * 用户重新点一次按钮即可，无恢复价值。
 *
 * 容量上限参照 {@code CaptchaManager}（100）：超出先清理过期项，
 * 仍满则拒绝签发——防异常刷票撑爆内存。
 *
 * @author 小莫唐尼
 */
@Service
public class BindTicketServiceImpl implements BindTicketService {

    static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_TICKETS = 100;
    private static final int TICKET_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    /** 单条票据：锁定用户名、过期时间与消费标记。消费后保留到自然过期，
     * 让 UC 轮询能看到 CONFIRMED（否则只会看到 EXPIRED，无法区分成败）。 */
    private record Ticket(String username, Instant expiresAt, boolean consumed) {
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
            tickets.put(ticket, new Ticket(username, expiresAt, false));
            return new IssuedTicket(ticket, expiresAt);
        });
    }

    @Override
    public Mono<TicketStatus> status(String ticket) {
        return Mono.fromSupplier(() -> {
            var t = tickets.get(ticket);
            if (t == null) {
                // 不存在 = 从未签发或已随过期清理；统一按过期处理，弹窗引导刷新即可。
                return new TicketStatus(ticket, Status.EXPIRED);
            }
            if (t.consumed()) {
                return new TicketStatus(ticket, Status.CONFIRMED);
            }
            if (Instant.now().isAfter(t.expiresAt())) {
                return new TicketStatus(ticket, Status.EXPIRED);
            }
            return new TicketStatus(ticket, Status.PENDING);
        });
    }

    @Override
    public Mono<ConsumeResult> consume(String ticket) {
        return Mono.fromSupplier(() -> {
            // compute 原子完成「校验 + 标记消费」：并发两个 confirm 只有一个成功。
            var result = new ConsumeResult[] {null};
            tickets.compute(ticket, (k, t) -> {
                if (t == null) {
                    result[0] = ConsumeResult.fail("绑定请求已失效，请重新生成二维码");
                    return null;
                }
                if (t.consumed()) {
                    result[0] = ConsumeResult.fail("该二维码已使用过，请重新生成");
                    return t;
                }
                if (Instant.now().isAfter(t.expiresAt())) {
                    result[0] = ConsumeResult.fail("二维码已过期，请重新生成");
                    return null;
                }
                result[0] = ConsumeResult.ok(t.username());
                return new Ticket(t.username(), t.expiresAt(), true);
            });
            return result[0];
        });
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
