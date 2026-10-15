package cn.ialley.unihalo.utils;

import java.util.Optional;

/**
 * 端点查询参数解析工具。
 *
 * @author 小莫唐尼
 */
public final class QueryParams {

    private QueryParams() {
    }

    /**
     * 整数解析（缺省、空值或非法格式回退默认值）。
     */
    public static int parseInt(Optional<String> value, int fallback) {
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.get().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
