package cn.ialley.unihalo.utils;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 短期令牌内存缓存（单值 + TTL），过期由定时任务兜底清理。
 *
 * @author lywq
 */
@Component
@EnableScheduling
public class TokenManager {

    private String token;
    private long expiryTime;

    public synchronized void setToken(String token, long ttlInSeconds) {
        this.token = token;
        this.expiryTime = System.currentTimeMillis() + ttlInSeconds * 1000;
    }

    public synchronized String getToken() {
        if (isTokenExpired()) {
            return null;
        }
        return token;
    }

    private synchronized boolean isTokenExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public synchronized void checkTokenExpiry() {
        if (isTokenExpired()) {
            this.token = null;
        }
    }
}