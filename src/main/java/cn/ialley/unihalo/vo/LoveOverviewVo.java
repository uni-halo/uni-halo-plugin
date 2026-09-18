package cn.ialley.unihalo.vo;

import java.util.List;

import lombok.Data;

/**
 * 首页模块摘要（各模块最新 3 条）。
 *
 * 锁定模块对应的列表恒为空列表（不是 null）：Finder 在锁定时不查库，
 * 因此这里连"有几条"都不泄露。
 *
 * @author 小莫唐尼
 */
@Data
public class LoveOverviewVo {

    /** 最新故事（≤3，锁定模块为空） */
    private List<LoveStoryVo> stories = List.of();

    /** 最新相册（≤3，锁定模块为空） */
    private List<LoveAlbumVo> albums = List.of();

    /** 最新清单条目（≤3，锁定模块为空） */
    private List<LoveDailyItemVo> items = List.of();

    /** 是否三个模块全空（模板据此渲染空状态，省掉三次判空） */
    public boolean isEmpty() {
        return stories.isEmpty() && albums.isEmpty() && items.isEmpty();
    }
}
