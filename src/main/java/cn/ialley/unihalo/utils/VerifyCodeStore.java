package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 邮箱验证码内存存储（自建验证闭环，轻量内存缓存，零第三方依赖）。
 *
 * <p>缓存为 ConcurrentHashMap（key = {@code username + ":" + email}），10 分钟过期
 * （写入时记录过期时间戳，读取时惰性清理），容量上限 1000（超出先清理过期项，
 * 仍满则淘汰最旧条目——洪泛下攻击者条目互相挤占，正常用户不受影响）。
 *
 * <p>验证码一次性（校验成功即移除，防重放）；每条目最多 5 次尝试（含错误码），
 * 超限即作废，防 6 位码爆破。同一用户同一邮箱重复发码直接覆盖旧码（旧码自然作废），
 * 换邮箱则 key 不同、互不影响。
 *
 * @author 小莫唐尼
 */
@Component
public class VerifyCodeStore {

    public static final int CODE_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_CACHE_SIZE = 1000;

    private record Entry(String code, long expireAt, AtomicInteger attempts) {
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final Supplier<Long> clock;
    private final SecureRandom random = new SecureRandom();

    public VerifyCodeStore() {
        this(System::currentTimeMillis);
    }

    /**
     * 包级可见构造：测试注入可控时钟（验证过期逻辑）。
     */
    VerifyCodeStore(Supplier<Long> clock) {
        this.clock = clock;
    }

    /**
     * 注册场景专用：生成验证码（key 与登录用户无关，仅按邮箱维度，前缀 {@code reg:}）。
     * 注册时用户尚不存在，不能按 username 建键；邮箱统一小写避免大小写绕过。
     */
    public Mono<String> issueForRegistration(String email) {
        return issue(REG_KEY, normalizeEmail(email));
    }

    /** 注册场景专用：校验并消费验证码（一次性语义与通用方法一致）。 */
    public Mono<Boolean> verifyAndConsumeForRegistration(String email, String code) {
        return verifyAndConsume(REG_KEY, normalizeEmail(email), code);
    }

    private static final String REG_KEY = "reg";

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * 生成 6 位数字验证码并存入（key = username + ":" + email，重复发码覆盖旧码）。
     *
     * @return 生成的验证码明文（仅供发信，不落日志/不落库）
     */
    public Mono<String> issue(String username, String email) {
        return Mono.fromSupplier(() -> {
            evictExpired();
            String code = String.format("%06d", random.nextInt(1_000_000));
            String key = keyOf(username, email);
            cache.put(key, new Entry(code, clock.get() + CODE_TTL_MINUTES * 60_000L,
                    new AtomicInteger(0)));
            return code;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 校验验证码：存在、未过期、未超尝试上限且匹配。成功即作废（一次性）；
     * 失败累计尝试次数（超限作废）。
     */
    public Mono<Boolean> verifyAndConsume(String username, String email, String code) {
        return Mono.fromCallable(() -> {
            if (code == null || code.isBlank()) {
                return false;
            }
            String key = keyOf(username, email);
            Entry entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            if (entry.expireAt() < clock.get()) {
                cache.remove(key);
                return false;
            }
            if (entry.attempts().get() >= MAX_ATTEMPTS) {
                cache.remove(key);
                return false;
            }
            boolean matched = constantTimeEquals(entry.code(), code.trim());
            if (matched) {
                cache.remove(key);
                return true;
            }
            entry.attempts().incrementAndGet();
            return false;
        });
    }

    /** 作废指定条目（如验证成功后的清理兜底）。 */
    public void invalidate(String username, String email) {
        cache.remove(keyOf(username, email));
    }

    private static String keyOf(String username, String email) {
        return username + ":" + email;
    }

    /** 超出容量上限时先清理过期项（惰性）；仍超限则淘汰最旧条目。 */
    private void evictExpired() {
        if (cache.size() < MAX_CACHE_SIZE) {
            return;
        }
        long now = clock.get();
        cache.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
        while (cache.size() >= MAX_CACHE_SIZE) {
            String oldestKey = null;
            long oldest = Long.MAX_VALUE;
            for (var entry : cache.entrySet()) {
                if (entry.getValue().expireAt() < oldest) {
                    oldest = entry.getValue().expireAt();
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey == null) {
                break;
            }
            cache.remove(oldestKey);
        }
    }

    /**
     * 定长字符串定常比较（消除时序侧信道）。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
