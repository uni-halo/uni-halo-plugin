package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 登录失败限流的行为验证。
 *
 * <p>锁定窗口的过期（15 分钟）不做时间旅行测试，只验证阈值与重置语义 ——
 * 窗口判定是 {@code start + WINDOW < now} 的一行比较，靠等待去测不划算。</p>
 */
class LoginAttemptGuardTest {

    private final LoginAttemptGuard guard = new LoginAttemptGuard();

    @Test
    @DisplayName("没有失败记录时不锁定")
    void notLockedBeforeAnyFailure() {
        assertThat(guard.usernameLockRemaining("alice")).isNull();
        assertThat(guard.ipLockRemaining("10.0.0.1")).isNull();
    }

    @Test
    @DisplayName("用户名连续失败 4 次仍未锁定，第 5 次才锁")
    void locksUsernameOnFifthFailure() {
        for (int i = 0; i < 4; i++) {
            guard.recordFailure("alice", "10.0.0.1");
        }
        assertThat(guard.usernameLockRemaining("alice")).isNull();

        guard.recordFailure("alice", "10.0.0.1");
        assertThat(guard.usernameLockRemaining("alice")).isNotNull();
    }

    @Test
    @DisplayName("同一 IP 累计失败 20 次后锁定该 IP")
    void locksIpAfterManyFailures() {
        for (int i = 0; i < 19; i++) {
            guard.recordFailure("user" + i, "10.0.0.1");
        }
        assertThat(guard.ipLockRemaining("10.0.0.1")).isNull();

        guard.recordFailure("user19", "10.0.0.1");
        assertThat(guard.ipLockRemaining("10.0.0.1")).isNotNull();
    }

    @Test
    @DisplayName("登录成功清零用户名计数，并可立即再次失败 4 次而不被锁")
    void resetUsernameClearsCounter() {
        for (int i = 0; i < 4; i++) {
            guard.recordFailure("alice", "10.0.0.1");
        }
        guard.resetUsername("alice");
        assertThat(guard.usernameLockRemaining("alice")).isNull();

        for (int i = 0; i < 4; i++) {
            guard.recordFailure("alice", "10.0.0.1");
        }
        assertThat(guard.usernameLockRemaining("alice")).isNull();
    }

    @Test
    @DisplayName("计数按用户名隔离，一个账号被锁不影响其他账号")
    void countersAreIsolatedPerUsername() {
        for (int i = 0; i < 5; i++) {
            guard.recordFailure("alice", null);
        }
        assertThat(guard.usernameLockRemaining("alice")).isNotNull();
        assertThat(guard.usernameLockRemaining("bob")).isNull();
    }

    @Test
    @DisplayName("用户名大小写归一化，无法用大小写差异绕过计数")
    void usernameIsCaseInsensitive() {
        guard.recordFailure("Alice", null);
        guard.recordFailure("ALICE", null);
        guard.recordFailure("alice", null);
        guard.recordFailure("aLiCe", null);
        guard.recordFailure("alice", null);

        assertThat(guard.usernameLockRemaining("alice")).isNotNull();
        assertThat(guard.usernameLockRemaining("ALICE")).isNotNull();
    }

    @Test
    @DisplayName("null 或空用户名/IP 不计数也不抛异常")
    void toleratesNullAndBlankKeys() {
        guard.recordFailure(null, null);
        guard.recordFailure("", "  ");
        guard.resetUsername(null);

        assertThat(guard.usernameLockRemaining(null)).isNull();
        assertThat(guard.usernameLockRemaining("")).isNull();
        assertThat(guard.ipLockRemaining(null)).isNull();
        assertThat(guard.ipLockRemaining("  ")).isNull();
    }
}
