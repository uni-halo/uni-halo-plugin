package cn.ialley.unihalo.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 登录失败限流（内存固定窗口计数）。
 *
 * <p>密码登录在补齐限流之前是一条「裸校验」接口，可被无限次尝试。这里按两个维度计数：</p>
 * <ul>
 *   <li><b>用户名</b>：{@value #MAX_USERNAME_FAILURES} 次失败即锁定一个窗口 —— 拦定向爆破；</li>
 *   <li><b>来源 IP</b>：{@value #MAX_IP_FAILURES} 次失败即锁定一个窗口 —— 拦换着用户名扫号。</li>
 * </ul>
 *
 * <p>锁定采用固定窗口：窗口（{@value #WINDOW_MINUTES} 分钟）结束后计数自然作废，无需人工解锁，
 * 也不会因为一次误输就把账号永久锁死。</p>
 *
 * <p><b>已知边界</b>：计数在内存中，多实例部署时各算各的；Halo 通常单实例，可接受。
 * IP 取 TCP 源地址（{@code remoteAddress}），反代后是反代 IP —— 这是刻意的取舍：
 * {@code X-Forwarded-For} 可由客户端伪造，用它做限流等于没做。</p>
 *
 * @author 小莫唐尼
 */
@Component
public class LoginAttemptGuard {

    private static final int WINDOW_MINUTES = 15;
    private static final Duration WINDOW = Duration.ofMinutes(WINDOW_MINUTES);
    private static final int MAX_USERNAME_FAILURES = 5;
    private static final int MAX_IP_FAILURES = 20;

    /** 条目数超过此值时顺带清理已过期窗口，避免长期运行下 Map 只增不减。 */
    private static final int PURGE_THRESHOLD = 4096;

    private static final String USERNAME_PREFIX = "u:";
    private static final String IP_PREFIX = "i:";

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /** 用户名维度的剩余锁定时间；未锁定返回 null。 */
    public Duration usernameLockRemaining(String username) {
        var key = usernameKey(username);
        return key == null ? null : lockRemaining(key, MAX_USERNAME_FAILURES);
    }

    /** 来源 IP 维度的剩余锁定时间；未锁定返回 null。 */
    public Duration ipLockRemaining(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        return lockRemaining(IP_PREFIX + clientIp.trim(), MAX_IP_FAILURES);
    }

    /** 记录一次失败，同时累加用户名与 IP 两个维度。 */
    public void recordFailure(String username, String clientIp) {
        var key = usernameKey(username);
        if (key != null) {
            increment(key);
        }
        if (clientIp != null && !clientIp.isBlank()) {
            increment(IP_PREFIX + clientIp.trim());
        }
        purgeExpiredIfNeeded();
    }

    /** 登录成功后清零用户名计数。IP 计数刻意不清零：否则扫号者只要穿插一次成功登录即可重置。 */
    public void resetUsername(String username) {
        var key = usernameKey(username);
        if (key != null) {
            counters.remove(key);
        }
    }

    private static String usernameKey(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        // Halo 用户名本身即小写（NAME_REGEX），归一化避免用大小写差异绕过计数。
        return USERNAME_PREFIX + username.trim().toLowerCase(Locale.ROOT);
    }

    private Duration lockRemaining(String key, int threshold) {
        var counter = counters.get(key);
        if (counter == null) {
            return null;
        }
        var now = Instant.now();
        if (isExpired(counter, now)) {
            counters.remove(key, counter);
            return null;
        }
        if (counter.count() < threshold) {
            return null;
        }
        return Duration.between(now, counter.start().plus(WINDOW));
    }

    private void increment(String key) {
        var now = Instant.now();
        counters.compute(key, (k, existing) -> existing == null || isExpired(existing, now)
                ? new Counter(now, 1)
                : new Counter(existing.start(), existing.count() + 1));
    }

    private void purgeExpiredIfNeeded() {
        if (counters.size() <= PURGE_THRESHOLD) {
            return;
        }
        var now = Instant.now();
        counters.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private static boolean isExpired(Counter counter, Instant now) {
        return counter.start().plus(WINDOW).isBefore(now);
    }

    private record Counter(Instant start, int count) {
    }
}
