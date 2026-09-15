package cn.ialley.unihalo.vo;

import java.util.List;

import cn.ialley.unihalo.scheme.LoveDailyItem;
import cn.ialley.unihalo.utils.LoveDates;
import lombok.Data;

/**
 * 恋爱清单条目公开视图（脱敏）。
 *
 * <p>{@link #status} 取值固定为 {@code wait}（未开始）/ {@code doing}（进行中）/
 * {@code complete}（已完成）—— 文案映射放在模板，不在 VO 里做本地化。</p>
 *
 * <p><b>锁定态下本对象根本不会被构造</b>：模块锁定时 Finder 直接返回空
 * {@code ListResult}（total=0）。</p>
 *
 * @author 小莫唐尼
 */
@Data
public class LoveDailyItemVo {

    private String name;

    private String title;

    /** 计划内容（Halo 富文本 HTML，模板 th:utext） */
    private String content;

    /** 状态：wait / doing / complete */
    private String status;

    /** 计划时间（yyyy-MM-dd，可空） */
    private String planDate;

    /** 完成时间（yyyy-MM-dd；status=complete 时才有） */
    private String completeDate;

    /** 完成感想（可空） */
    private String completeRemark;

    /** 回忆图片列表（Halo 附件 URL） */
    private List<String> images = List.of();

    /** 排序（越大越前） */
    private Integer priority;

    public static LoveDailyItemVo from(LoveDailyItem item) {
        LoveDailyItemVo vo = new LoveDailyItemVo();
        if (item == null) {
            return vo;
        }
        if (item.getMetadata() != null) {
            vo.setName(item.getMetadata().getName());
        }
        LoveDailyItem.LoveDailyItemSpec spec = item.getSpec();
        if (spec != null) {
            vo.setTitle(spec.getTitle());
            vo.setContent(spec.getContent());
            vo.setStatus(spec.getStatus());
            vo.setPlanDate(LoveDates.isoDate(spec.getPlanDate()));
            vo.setCompleteDate(LoveDates.isoDate(spec.getCompleteDate()));
            vo.setCompleteRemark(spec.getCompleteRemark());
            vo.setImages(spec.getImages() == null ? List.of() : spec.getImages());
            vo.setPriority(spec.getPriority());
        }
        return vo;
    }
}
