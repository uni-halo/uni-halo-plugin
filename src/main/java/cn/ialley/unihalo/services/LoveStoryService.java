package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.LoveStory;
import java.util.List;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 恋爱故事服务（多条目）
 *
 * @author 小莫唐尼
 */
public interface LoveStoryService {

    /**
     * 分页列表；keyword 模糊匹配标题/内容，排序 spec.priority 倒序 + 创建时间倒序。
     * 管理端使用，保留删除中对象。
     */
    Mono<ListResult<LoveStory>> list(String keyword, int page, int size);

    /**
     * 公开列表（keyword 恒空，排除删除中对象）。公开读路径专用。
     */
    Mono<ListResult<LoveStory>> listPublic(int page, int size);

    Mono<LoveStory> getByName(String name);

    /**
     * 新建（title 必填）。
     */
    Mono<LoveStory> create(LoveStory story);

    /**
     * 更新（title 必填）。
     */
    Mono<LoveStory> update(LoveStory story);

    Mono<Void> delete(String name);

    /**
     * 批量手动排序：names 为按新顺序排列的 name 列表，
     * 依次写 spec.priority 降序（第一位最大）。
     */
    Mono<Void> sort(List<String> names);
}
