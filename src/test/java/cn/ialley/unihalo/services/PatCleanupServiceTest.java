package cn.ialley.unihalo.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cn.ialley.unihalo.constants.Constants;
import run.halo.app.extension.Metadata;
import run.halo.app.security.PersonalAccessToken;

/**
 * 登录令牌清理策略单元测试。
 *
 * <p>核心保障：只回收带归属标签的令牌，用户手动创建的令牌永不被删。</p>
 *
 * @author 小莫唐尼
 */
class PatCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    @Test
    @DisplayName("插件签发的过期令牌应被回收")
    void shouldDeleteExpiredOwnedToken() {
        var pat = ownedPat();
        pat.getSpec().setExpiresAt(NOW.minus(Duration.ofDays(1)));

        assertThat(PatCleanupService.shouldDelete(pat, NOW)).isTrue();
    }

    @Test
    @DisplayName("未过期的令牌不应被回收")
    void shouldKeepValidToken() {
        var pat = ownedPat();
        pat.getSpec().setExpiresAt(NOW.plus(Duration.ofDays(1)));

        assertThat(PatCleanupService.shouldDelete(pat, NOW)).isFalse();
    }

    @Test
    @DisplayName("用户手动创建的过期令牌不应被回收")
    void shouldKeepUserCreatedToken() {
        var pat = new PersonalAccessToken();
        pat.setMetadata(new Metadata());
        pat.getMetadata().setName("pat-user-created");
        pat.setSpec(new PersonalAccessToken.Spec());
        pat.getSpec().setExpiresAt(NOW.minus(Duration.ofDays(30)));

        assertThat(PatCleanupService.shouldDelete(pat, NOW)).isFalse();
    }

    @Test
    @DisplayName("已吊销且超过保留期的令牌应被回收")
    void shouldDeleteRevokedAfterRetention() {
        var pat = ownedPat();
        pat.getSpec().setRevoked(true);
        pat.getSpec().setRevokesAt(NOW.minus(Duration.ofDays(8)));

        assertThat(PatCleanupService.shouldDelete(pat, NOW)).isTrue();
    }

    @Test
    @DisplayName("刚吊销的令牌在保留期内不应被回收")
    void shouldKeepRecentlyRevokedToken() {
        var pat = ownedPat();
        pat.getSpec().setRevoked(true);
        pat.getSpec().setRevokesAt(NOW.minus(Duration.ofDays(1)));

        assertThat(PatCleanupService.shouldDelete(pat, NOW)).isFalse();
    }

    private static PersonalAccessToken ownedPat() {
        var pat = new PersonalAccessToken();
        pat.setMetadata(new Metadata());
        pat.getMetadata().setName("pat-test-abc123");
        pat.getMetadata().setLabels(Map.of(
                Constants.PAT_MANAGED_BY_LABEL, Constants.PAT_MANAGED_BY_VALUE));
        pat.setSpec(new PersonalAccessToken.Spec());
        return pat;
    }
}
