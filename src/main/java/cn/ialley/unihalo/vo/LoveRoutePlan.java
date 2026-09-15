package cn.ialley.unihalo.vo;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

/**
 * 恋爱日记路由解析结果。
 *
 * <p>{@link #paths} 为 key → 已解析的完整路径，<b>只含可安全注册的路由</b>；
 * {@link #conflicts} 为 key → 冲突原因（该条不注册，fail-closed）；
 * {@link #skipped} 为未配置（留空）而未注册的 key。</p>
 *
 * <p>key 取值固定为 {@code home} / {@code stories} / {@code albums} / {@code daily}，
 * 与设置项 {@code routes.*} 及模板 model 中的 {@code routes} 一致。</p>
 *
 * @author 小莫唐尼
 */
@Data
public class LoveRoutePlan {

    /** key → 完整路径（可注册） */
    private Map<String, String> paths = new LinkedHashMap<>();

    /** key → 冲突原因（不可注册） */
    private Map<String, String> conflicts = new LinkedHashMap<>();

    /** 未配置（留空 = 主动不注册）的 key */
    private java.util.List<String> skipped = new java.util.ArrayList<>();

    /** 冲突检测实际比对的来源说明（写入日志与 Console，便于排查） */
    private String checkedSources = "";

    /**
     * 是否无任何可注册路由（此时除 Head 注入外不应注册任何 RouterFunction）。
     */
    public boolean isEmpty() {
        return paths.isEmpty();
    }

    /**
     * 该 key 对应的列表页是否需要注册 {@code /page/{page}}。
     * 首页与相册详情不参与分页；故事/相册/清单列表页参与。
     */
    public static boolean isListPage(String key) {
        return "stories".equals(key) || "albums".equals(key) || "daily".equals(key);
    }
}
