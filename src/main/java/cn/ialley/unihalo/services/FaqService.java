package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.Faq;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 常见问题（FAQ）服务。
 *
 * @author 小莫唐尼
 */
public interface FaqService {

    /**
     * 控制台分页列表；keyword 模糊匹配标题/摘要，category 非空时按分类过滤，
     * visible 非空时按显隐过滤。
     */
    Mono<ListResult<Faq>> list(String keyword, String category, Boolean visible, int page, int size);

    /**
     * 按 name 查询；不存在时抛 {@link cn.ialley.unihalo.exception.NotFoundException}。
     */
    Mono<Faq> getByName(String name);

    /**
     * 按 name 查询公开可见条目（仅 visible=true，隐藏条目按不存在处理）；
     * 不存在或已隐藏时抛 {@link cn.ialley.unihalo.exception.NotFoundException}。
     */
    Mono<Faq> getPublicByName(String name);

    /**
     * 新建（title 必填）。
     */
    Mono<Faq> create(Faq faq);

    /**
     * 更新。
     */
    Mono<Faq> update(Faq faq);

    /**
     * 删除。
     */
    Mono<Void> delete(String name);

    /**
     * 公开有序列表（仅 visible=true；size<=0 返回全量，供 app 端一次取全部）；
     * keyword 模糊匹配标题/摘要/分类，category 非空时按分类过滤。
     */
    Mono<ListResult<Faq>> listPublic(String keyword, String category, int page, int size);
}
