package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 注册验证码发送限流器测试（三桶：全站 / 单 IP / 单邮箱）。
 *
 * <p>默认配额（未配置 safetyConfig 时）：全站 30/分钟、IP 10/分钟、邮箱 3/10 分钟。
 * 配额缓存 30 秒，每个用例独立实例避免缓存串扰。
 */
@DisplayName("注册验证码发送限流")
@ExtendWith(MockitoExtension.class)
class RegisterEmailCodeRateLimiterTest {

    @Mock
    private ReactiveSettingFetcher settingFetcher;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("同一邮箱 10 分钟内第 4 次（默认 3 次上限）→ 拒绝发送")
    void emailBucketRejectsFourthSendWithinWindow() {
        var limiter = new RegisterEmailCodeRateLimiter(settingFetcher);
        for (int i = 0; i < 3; i++) {
            limiter.check("10.0.0.1", "user@example.com").block();
        }
        assertThatThrownBy(() -> limiter.check("10.0.0.1", "user@example.com").block())
                .isInstanceOf(RegisterEmailCodeRateLimiter.QuotaExceededException.class);
    }

    @Test
    @DisplayName("邮箱桶按地址隔离 → A 邮箱耗尽配额不影响 B 邮箱")
    void emailBucketIsPerAddress() {
        var limiter = new RegisterEmailCodeRateLimiter(settingFetcher);
        for (int i = 0; i < 3; i++) {
            limiter.check("10.0.0.1", "user@example.com").block();
        }
        assertThatCode(() -> limiter.check("10.0.0.1", "other@example.com").block())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("同一 IP 1 分钟内第 11 次（默认 10 次上限，换邮箱无效）→ 拒绝；换 IP 不受影响")
    void ipBucketRejectsEleventhSend() {
        var limiter = new RegisterEmailCodeRateLimiter(settingFetcher);
        for (int i = 0; i < 10; i++) {
            limiter.check("10.0.0.1", "user" + i + "@example.com").block();
        }
        assertThatThrownBy(() -> limiter.check("10.0.0.1", "user10@example.com").block())
                .isInstanceOf(RegisterEmailCodeRateLimiter.QuotaExceededException.class);
        assertThatCode(() -> limiter.check("10.0.0.2", "user10@example.com").block())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("超限失败时全局与 IP 占用须回滚 → 不泄漏失败尝试消耗的配额")
    void exceededAttemptRollsBackGlobalAndIpQuota() {
        // 全站 2/分钟、邮箱 1/10 分钟：第二次同邮箱超限时，全局与 IP 占用须回滚
        var limiter = new RegisterEmailCodeRateLimiter(
                stubbedFetcher(safetyNode(2, 10, 1)));
        limiter.check("10.0.0.1", "a@example.com").block();
        assertThatThrownBy(() -> limiter.check("10.0.0.1", "a@example.com").block())
                .isInstanceOf(RegisterEmailCodeRateLimiter.QuotaExceededException.class);
        // 若全局/IP 未回滚，全局已计 2 + 本次 = 3 > 2，此处必然失败
        assertThatCode(() -> limiter.check("10.0.0.2", "b@example.com").block())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("配额配置为 0 → 该桶不限制（0 = 关闭该维度）")
    void zeroQuotaDisablesBucket() {
        var limiter = new RegisterEmailCodeRateLimiter(
                stubbedFetcher(safetyNode(30, 10, 0)));
        for (int i = 0; i < 5; i++) {
            limiter.check("10.0.0.1", "user@example.com").block();
        }
        assertThatCode(() -> limiter.check("10.0.0.1", "user@example.com").block())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("配置非法（负数 / 非数字）→ 回退默认配额，不因脏配置失效")
    void invalidConfigValuesFallBackToDefaults() {
        ObjectNode safety = mapper.createObjectNode();
        ObjectNode code = safety.putObject("registerEmailCodeConfig");
        code.put("emailPerTenMinutes", -1);
        code.put("globalPerMinute", "not-a-number");
        var limiter = new RegisterEmailCodeRateLimiter(stubbedFetcher(safety));
        for (int i = 0; i < 3; i++) {
            limiter.check("10.0.0.1", "user@example.com").block();
        }
        assertThatThrownBy(() -> limiter.check("10.0.0.1", "user@example.com").block())
                .isInstanceOf(RegisterEmailCodeRateLimiter.QuotaExceededException.class);
    }

    /** 构造 safetyConfig.registerEmailCodeConfig 设置节点。 */
    private ObjectNode safetyNode(int global, int ip, int email) {
        ObjectNode safety = mapper.createObjectNode();
        ObjectNode code = safety.putObject("registerEmailCodeConfig");
        code.put("globalPerMinute", global);
        code.put("ipPerMinute", ip);
        code.put("emailPerTenMinutes", email);
        return safety;
    }

    /** 将 safetyConfig 节点 stub 进设置读取器。 */
    private ReactiveSettingFetcher stubbedFetcher(ObjectNode safety) {
        when(settingFetcher.getSettingValue("safetyConfig")).thenReturn(Mono.just(safety));
        return settingFetcher;
    }
}
