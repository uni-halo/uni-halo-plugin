package cn.ialley.unihalo.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 恋爱日记主题页的日期派生工具。
 *
 * <h3>为什么派生放在服务端</h3>
 * <p>设计报告 §5.3 明确要求故事时间轴（日大字 / 年·月 / 星期）由<b>服务端计算</b>，
 * 不引入 dayjs 之类的运行期依赖；同时倒计时「第 N 天」也要有 SSR 初值，
 * 否则无 JS / 首屏会空白。于是统一在这里把库里的字符串日期解析成模板可直接输出的片段。</p>
 *
 * <h3>容错</h3>
 * <p>站长手填的日期可能不规范（{@code 2024/5/1}、带时间、空串）。本类一律
 * <b>解析失败即返回 null</b>，由模板按「没有日期」渲染 —— 绝不因为一个脏日期让整页 500。</p>
 *
 * @author 小莫唐尼
 */
public final class LoveDates {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;

    private LoveDates() {
    }

    /**
     * 宽松解析日期字符串：只取前 10 个字符，允许 {@code /} 与 {@code .} 作分隔符。
     *
     * @return 解析结果；失败返回 {@code null}
     */
    public static LocalDate parse(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        text = text.replace('/', '-').replace('.', '-');
        if (text.length() > 10) {
            text = text.substring(0, 10);
        }
        String[] parts = text.split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int day = Integer.parseInt(parts[2].trim());
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 恋爱天数：纪念日当天算第 1 天（与 app 端 {@code daysSince()} 一致）。
     *
     * @return 天数；日期为空或非法时返回 {@code null}（模板显示占位）
     */
    public static Integer daysSince(String value) {
        LocalDate start = parse(value);
        if (start == null) {
            return null;
        }
        return (int) (start.until(LocalDate.now()).getDays() + 1);
    }

    /** 时间轴「日大字」，如 {@code 12}；失败返回 {@code null}。 */
    public static String dayOfMonth(String value) {
        LocalDate date = parse(value);
        return date == null ? null : String.format("%02d", date.getDayOfMonth());
    }

    /** 时间轴「年·月」，如 {@code 2026·03}；失败返回 {@code null}。 */
    public static String yearMonth(String value) {
        LocalDate date = parse(value);
        return date == null ? null : date.getYear() + "·" + String.format("%02d", date.getMonthValue());
    }

    /** 时间轴「星期」，如 {@code 星期四}；失败返回 {@code null}。 */
    public static String weekday(String value) {
        LocalDate date = parse(value);
        return date == null ? null : date.getDayOfWeek().getDisplayName(TextStyle.FULL, ZH);
    }

    /** 规范化成 {@code yyyy-MM-dd}（已是该格式时原样返回前 10 位）；失败返回原始文本。 */
    public static String isoDate(String value) {
        LocalDate date = parse(value);
        return date == null ? value : ISO_DATE.format(date);
    }

    /** 附件创建时间 → {@code yyyy-MM-dd}（取服务器时区）；为 null 时返回 {@code null}。 */
    public static String isoDate(Instant instant) {
        return instant == null ? null
                : ISO_DATE.format(instant.atZone(ZoneId.systemDefault()).toLocalDate());
    }
}
