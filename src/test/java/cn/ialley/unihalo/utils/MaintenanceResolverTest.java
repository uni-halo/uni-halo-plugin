package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.ialley.unihalo.utils.MaintenanceResolver.MaintenanceStatus;

/**
 * 维护模式状态判定（纯函数）测试。
 */
@DisplayName("维护模式状态判定")
class MaintenanceResolverTest {

    private static final Instant NOW = Instant.parse("2026-09-24T00:00:00Z");

    @Test
    @DisplayName("开关未开启（null / false）→ 未维护 NONE")
    void disabledOrNonNullFalseMeansNone() {
        assertThat(MaintenanceResolver.resolve(null, null, null, NOW)).isEqualTo(MaintenanceStatus.NONE);
        assertThat(MaintenanceResolver.resolve(false, null, null, NOW)).isEqualTo(MaintenanceStatus.NONE);
    }

    @Test
    @DisplayName("endTime 已到（含等号）→ 维护自动结束 NONE（输出端判定，配置不回写）")
    void endTimeReachedMeansAutoFinished() {
        assertThat(MaintenanceResolver.resolve(true, null,
                "2026-09-24T00:00:00Z", NOW)).isEqualTo(MaintenanceStatus.NONE);
        assertThat(MaintenanceResolver.resolve(true, null,
                "2026-09-23T00:00:00Z", NOW)).isEqualTo(MaintenanceStatus.NONE);
    }

    @Test
    @DisplayName("startTime 在未来 → 维护预告 SCHEDULED")
    void futureStartTimeMeansScheduled() {
        assertThat(MaintenanceResolver.resolve(true,
                "2026-09-25T00:00:00Z", "2026-09-26T00:00:00Z", NOW))
                .isEqualTo(MaintenanceStatus.SCHEDULED);
    }

    @Test
    @DisplayName("已开始且未结束 → 维护中 ACTIVE（开始时刻含等号）")
    void startedWithoutEndMeansActive() {
        assertThat(MaintenanceResolver.resolve(true,
                "2026-09-23T00:00:00Z", null, NOW)).isEqualTo(MaintenanceStatus.ACTIVE);
        assertThat(MaintenanceResolver.resolve(true,
                "2026-09-24T00:00:00Z", null, NOW)).isEqualTo(MaintenanceStatus.ACTIVE);
    }

    @Test
    @DisplayName("时间字符串空 / 非法 → 按未设置处理（返回 null，不影响 ACTIVE 判定）")
    void blankOrInvalidTimeTreatedAsUnset() {
        assertThat(MaintenanceResolver.parseTime(null)).isNull();
        assertThat(MaintenanceResolver.parseTime("  ")).isNull();
        assertThat(MaintenanceResolver.parseTime("not-a-time")).isNull();
        // 非法时间不抛错，公开输出不因脏数据失败
        assertThat(MaintenanceResolver.resolve(true, "bad", "also-bad", NOW))
                .isEqualTo(MaintenanceStatus.ACTIVE);
    }
}
