package cn.ialley.unihalo.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.constants.Constants;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.security.PersonalAccessToken;

/**
 * 登录令牌（PAT）过期清理。
 *
 * <p>每次登录都会新建一条 PAT，高频登录会持续产生扩展记录。令牌的「失效」由 JWT 的
 * {@code exp} 保证（Halo 的 {@code PatAuthenticationManager} 不校验 spec.expiresAt），
 * 清理只是回收存储，因此低频扫描即可。</p>
 *
 * <p><b>只回收本插件签发的令牌</b>：创建时打了
 * {@code unihalo.ialley.cn/managed-by=uni-halo} 标签，用户在「个人中心 → 个人令牌」
 * 手动创建的令牌不带该标签，永远不会被删除。</p>
 *
 * <p>回收条件（需同时满足归属标签）：</p>
 * <ul>
 *   <li>已过期：{@code spec.expiresAt} 早于当前时间；</li>
 *   <li>已吊销且超过保留期：{@code spec.revokesAt + 7 天} 早于当前时间（保留一段时间
 *       便于追溯，且避免与正在进行的请求竞态）。</li>
 * </ul>
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatCleanupService implements InitializingBean, DisposableBean {

    private static final Duration INITIAL_DELAY = Duration.ofMinutes(5);
    private static final Duration SCAN_INTERVAL = Duration.ofHours(6);
    private static final Duration REVOKED_RETENTION = Duration.ofDays(7);

    private final ReactiveExtensionClient client;

    private volatile Disposable subscription;

    @Override
    public void afterPropertiesSet() {
        this.subscription = Flux.interval(INITIAL_DELAY, SCAN_INTERVAL)
                .onBackpressureDrop()
                .concatMap(tick -> cleanup()
                        .onErrorResume(e -> {
                            log.warn("【UniHalo】登录令牌清理失败，下次扫描重试", e);
                            return Mono.just(0L);
                        }))
                .subscribe();
        log.info("【UniHalo】登录令牌过期清理已启动（首次 5 分钟后，之后每 6 小时一次）");
    }

    @Override
    public void destroy() {
        var subscription = this.subscription;
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    /**
     * 清理一次，返回删除数量。手动触发与定时扫描共用同一入口。
     */
    public Mono<Long> cleanup() {
        var now = Instant.now();
        return client.list(PersonalAccessToken.class, pat -> shouldDelete(pat, now), null)
                .flatMap(client::delete)
                .count()
                .doOnNext(count -> {
                    if (count > 0) {
                        log.info("【UniHalo】已清理 {} 个过期/已吊销的登录令牌", count);
                    }
                });
    }

    /**
     * 是否应当回收。抽成静态方法便于单测覆盖各分支。
     */
    public static boolean shouldDelete(PersonalAccessToken pat, Instant now) {
        if (pat == null || pat.getSpec() == null || !ownedByPlugin(pat)) {
            return false;
        }
        var spec = pat.getSpec();
        var expiresAt = spec.getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(now)) {
            return true;
        }
        if (spec.isRevoked() && spec.getRevokesAt() != null) {
            return spec.getRevokesAt().plus(REVOKED_RETENTION).isBefore(now);
        }
        return false;
    }

    private static boolean ownedByPlugin(PersonalAccessToken pat) {
        Map<String, String> labels = pat.getMetadata() == null ? null : pat.getMetadata().getLabels();
        if (labels == null) {
            return false;
        }
        return Constants.PAT_MANAGED_BY_VALUE.equals(labels.get(Constants.PAT_MANAGED_BY_LABEL));
    }
}
