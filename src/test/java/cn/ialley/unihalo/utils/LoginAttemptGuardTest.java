package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 登录失败限流（内存固定窗口）测试。
 *
 * <p>注：窗口固定 15 分钟且无时钟注入，过期重置路径无法在单测内覆盖，
 * 仅覆盖计数、阈值、归一化与清零语义。
 */
@DisplayName("登录失败限流")
class LoginAttemptGuardTest {

    private final LoginAttemptGuard guard = new LoginAttemptGuard();

    @Test
    @DisplayName("无失败记录 → 用户名与 IP 均未锁定")
    void noFailuresMeansNoLock() {
        assertThat(guard.usernameLockRemaining("alice")).isNull();
        assertThat(guard.ipLockRemaining("10.0.0.1")).isNull();
    }

    @Test
    @DisplayName("同一用户名第 5 次失败 → 触发锁定（剩余时间 ≤ 15 分钟窗口）")
    void usernameLocksAtFifthFailure() {
        for (int i = 0; i < 4; i++) {
            guard.recordFailure("alice", "10.0.0.1");
        }
        assertThat(guard.usernameLockRemaining("alice")).isNull();
        guard.recordFailure("alice", "10.0.0.1");
        Duration remaining = guard.usernameLockRemaining("alice");
        assertThat(remaining).isNotNull();
        assertThat(remaining).isPositive();
        assertThat(remaining).isLessThanOrEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("用户名大小写归一 → 用 ALICE/alice 变体无法绕过计数")
    void usernameCounterIsCaseInsensitive() {
        for (int i = 0; i < 5; i++) {
            guard.recordFailure("Alice", "10.0.0.1");
        }
        assertThat(guard.usernameLockRemaining("ALICE")).isNotNull();
        assertThat(guard.usernameLockRemaining("alice")).isNotNull();
    }

    @Test
    @DisplayName("同一 IP 第 20 次失败（跨用户扫号）→ IP 维度锁定，其他 IP 不受影响")
    void ipLocksAtTwentiethFailure() {
        for (int i = 0; i < 19; i++) {
            guard.recordFailure("user" + i, "10.0.0.9");
        }
        assertThat(guard.ipLockRemaining("10.0.0.9")).isNull();
        guard.recordFailure("user19", "10.0.0.9");
        assertThat(guard.ipLockRemaining("10.0.0.9")).isNotNull();
        assertThat(guard.ipLockRemaining("10.0.0.10")).isNull();
    }

    @Test
    @DisplayName("登录成功 resetUsername 只清用户名计数；IP 计数刻意不清零（防穿插重置）")
    void resetUsernameClearsUsernameButNotIp() {
        for (int i = 0; i < 20; i++) {
            guard.recordFailure("bob", "10.0.0.9");
        }
        assertThat(guard.usernameLockRemaining("bob")).isNotNull();
        guard.resetUsername("bob");
        assertThat(guard.usernameLockRemaining("bob")).isNull();
        assertThat(guard.ipLockRemaining("10.0.0.9")).isNotNull();
    }

    @Test
    @DisplayName("null / 空白输入 → 安全忽略，不计数不抛错")
    void nullAndBlankInputsAreIgnored() {
        guard.recordFailure(null, null);
        guard.recordFailure("  ", "  ");
        guard.recordFailure(null, "10.0.0.1");
        guard.recordFailure("alice", null);
        assertThat(guard.usernameLockRemaining("alice")).isNull();
        assertThat(guard.ipLockRemaining(null)).isNull();
        assertThat(guard.ipLockRemaining("")).isNull();
    }
}
