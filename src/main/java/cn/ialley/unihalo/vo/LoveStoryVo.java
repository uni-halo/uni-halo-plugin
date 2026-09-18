package cn.ialley.unihalo.vo;

import java.util.List;

import cn.ialley.unihalo.scheme.LoveStory;
import cn.ialley.unihalo.utils.LoveDates;
import lombok.Data;

/**
 * 恋爱故事公开视图（脱敏）。
 *
 * {@link #content} 为站长在 Halo 富文本编辑器写的 HTML（可信来源），模板用
 * {@code th:utext} 渲染并套 {@code prose uh-love-content} 容器（{@code .prose} 交给
 * 主题排版，{@code .uh-love-content} 是插件补位锚点）；与 app 端一致，不额外做前端净化。
 * 锁定态下本对象根本不会被构造：模块锁定时 Finder 直接返回空 {@code ListResult}。
 *
 * @author 小莫唐尼
 */
@Data
public class LoveStoryVo {

    private String name;

    private String title;

    /** 故事正文（Halo 富文本 HTML，模板 th:utext） */
    private String content;

    /** 故事时间（yyyy-MM-dd，可空） */
    private String date;

    /** 时间轴日大字（如 {@code 12}，服务端派生；日期缺失为 null） */
    private String day;

    /** 时间轴年·月（如 {@code 2026·03}，服务端派生） */
    private String yearMonth;

    /** 时间轴星期（如 {@code 星期四}，服务端派生；不引入 dayjs） */
    private String weekday;

    /** 故事地点（可空） */
    private String location;

    /** 图片列表（Halo 附件 URL） */
    private List<String> images = List.of();

    /** 排序（越大越前） */
    private Integer priority;

    public static LoveStoryVo from(LoveStory story) {
        LoveStoryVo vo = new LoveStoryVo();
        if (story == null) {
            return vo;
        }
        if (story.getMetadata() != null) {
            vo.setName(story.getMetadata().getName());
        }
        LoveStory.LoveStorySpec spec = story.getSpec();
        if (spec != null) {
            vo.setTitle(spec.getTitle());
            vo.setContent(spec.getContent());
            vo.setDate(LoveDates.isoDate(spec.getDate()));
            vo.setDay(LoveDates.dayOfMonth(spec.getDate()));
            vo.setYearMonth(LoveDates.yearMonth(spec.getDate()));
            vo.setWeekday(LoveDates.weekday(spec.getDate()));
            vo.setLocation(spec.getLocation());
            vo.setImages(spec.getImages() == null ? List.of() : spec.getImages());
            vo.setPriority(spec.getPriority());
        }
        return vo;
    }
}
