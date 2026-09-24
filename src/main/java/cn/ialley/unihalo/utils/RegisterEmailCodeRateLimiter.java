package cn.ialley.unihalo.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;

/**
 * 注册验证码发送限流器（固定窗口计数，内存态，重启即清零）。
 *
 * <p>三桶叠加，任一桶满即拒绝：全站总量（防分布式滥用的最后防线）、单 IP、
 * 单邮箱（防轰炸）。配额读自设置页「安全控制 → 注册邮箱验证码」，缓存 30 秒；
 * 配额为 0 表示该桶不限制。
 */
@Component
public class RegisterEmailCodeRateLimiter {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private static final int DEFAULT_GLOBAL = 30;

    private static final int DEFAULT_IP = 10;

    private static final int DEFAULT_EMAIL = 3;

    private static final long WINDOW_1MIN_MILLIS = Duration.ofMinutes(1).toMillis();

    private static final long WINDOW_10MIN_MILLIS = Duration.ofMinutes(10).toMillis();

    private static final int MAX_KEYS = 1024;

    private record Limit(int max, long windowMillis) {
    }

    private record Limits(Limit global, Limit ip, Limit email) {
    }

    private static final Limits DEFAULT_LIMITS = new Limits(
            new Limit(DEFAULT_GLOBAL, WINDOW_1MIN_MILLIS),
            new Limit(DEFAULT_IP, WINDOW_1MIN_MILLIS),
            new Limit(DEFAULT_EMAIL, WINDOW_10MIN_MILLIS));

    /** key → [windowStart, count] */
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    private final ReactiveSettingFetcher settingFetcher;

    private volatile Limits cached;

    private volatile long cachedAt;

    public RegisterEmailCodeRateLimiter(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    /**
     * 为一次发送占用配额；任一桶超限则整体失败并回滚已占用的配额。
     *
     * @param ip    客户端 IP（socket 源地址，不可伪造）
     * @param email 目标邮箱（小写归一）
     */
    public Mono<Void> check(String ip, String email) {
        return limits().map(limits -> {
            reserveAll(ip, email, limits);
            return true;
        }).then();
    }

    private void reserveAll(String ip, String email, Limits limits) {
        sweepIfCrowded();
        var reserved = new ArrayList<String>(3);
        try {
            if (limits.global().max() > 0) {
                reserve("g", limits.global(), reserved);
            }
            if (limits.ip().max() > 0) {
                reserve("ip:" + (ip == null ? "unknown" : ip), limits.ip(), reserved);
            }
            if (limits.email().max() > 0) {
                reserve("em:" + (email == null ? "unknown" : email), limits.email(), reserved);
            }
        } catch (QuotaExceededException e) {
            reserved.forEach(this::release);
            throw e;
        }
    }

    private void reserve(String key, Limit limit, List<String> reserved) {
        long now = System.currentTimeMillis();
        var slot = counters.compute(key, (k, v) ->
                v == null || now - v[0] >= limit.windowMillis()
                        ? new long[] {now, 1}
                        : new long[] {v[0], v[1] + 1});
        if (slot[1] > limit.max()) {
            release(key);
            throw new QuotaExceededException();
        }
        reserved.add(key);
    }

    private void release(String key) {
        counters.computeIfPresent(key, (k, v) -> new long[] {v[0], v[1] - 1});
    }

    /** 键过多时惰性清理已过窗口的计数，防止 Map 无界增长 */
    private void sweepIfCrowded() {
        if (counters.size() <= MAX_KEYS) {
            return;
        }
        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW_10MIN_MILLIS);
    }

    private Mono<Limits> limits() {
        var snapshot = cached;
        if (snapshot != null && System.currentTimeMillis() - cachedAt < CACHE_TTL.toMillis()) {
            return Mono.just(snapshot);
        }
        return SettingGroupResolver
                .group(settingFetcher, "safetyConfig", "registerEmailCodeConfig")
                .map(this::parse)
                .defaultIfEmpty(DEFAULT_LIMITS)
                .doOnNext(limits -> {
                    cached = limits;
                    cachedAt = System.currentTimeMillis();
                });
    }

    private Limits parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            return DEFAULT_LIMITS;
        }
        return new Limits(
                new Limit(intValue(node, "globalPerMinute", DEFAULT_GLOBAL), WINDOW_1MIN_MILLIS),
                new Limit(intValue(node, "ipPerMinute", DEFAULT_IP), WINDOW_1MIN_MILLIS),
                new Limit(intValue(node, "emailPerTenMinutes", DEFAULT_EMAIL),
                        WINDOW_10MIN_MILLIS));
    }

    private int intValue(JsonNode node, String field, int defaultValue) {
        var value = node.get(field);
        if (value == null || !value.isNumber() || value.intValue() < 0) {
            return defaultValue;
        }
        return value.intValue();
    }

    public static class QuotaExceededException extends RuntimeException {
    }
}
