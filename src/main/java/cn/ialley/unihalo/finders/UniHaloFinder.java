package cn.ialley.unihalo.finders;

import cn.ialley.unihalo.vo.LoveAlbumVo;
import cn.ialley.unihalo.vo.LoveConfigVo;
import cn.ialley.unihalo.vo.LoveDailyItemVo;
import cn.ialley.unihalo.vo.LoveModuleAccess;
import cn.ialley.unihalo.vo.LovePageVo;
import cn.ialley.unihalo.vo.LoveStoryVo;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 恋爱日记主题页 Finder（模板只读数据源，版本化契约，破坏兼容须升主版本）。
 *
 * 全部方法只读、不抛异常、空结果返回空 {@code ListResult}；锁判定在本层完成，
 * 模块锁定时返回空结果，绝不把业务字段交给模板；返回展示用脱敏 VO，不含敏感字段。
 *
 * 返回裸 {@code ListResult}（对齐 Halo 官方 Finder）：Finder 不碰 HTTP，拼 URL 是
 * {@code LoveDiaryRouter} 的职责，它把结果包成 {@code UrlContextListResult} 放进 model，
 * 模板侧 {@code prevUrl}/{@code nextUrl} 等用法照旧可用。
 *
 * @author 小莫唐尼
 */
public interface UniHaloFinder {

    /**
     * 恋爱页配置（纪念日/恋人信息/模块入口列表）。
     *
     * 模块入口只包含已注册路由且 {@code enabled=true} 的模块，按
     * {@code priority} 降序；因此模板可以直接遍历，不需要再判"这个链接能不能点"。
     */
    Mono<LoveConfigVo> loveConfig();

    /**
     * 指定模块的访问状态（供锁定页渲染页内解锁表单）。
     *
     * @param module 模块 key 或路由 key（{@code loveDiary}/{@code ourStory}/
     *               {@code lovePhoto}/{@code loveDaily}，或 {@code home}/{@code stories}/
     *               {@code albums}/{@code daily}）
     */
    Mono<LoveModuleAccess> loveModuleAccess(String module);

    /**
     * 首页模型 = 配置 + 摘要 + 路由表 + 访问状态。
     *
     * 恋爱日记入口本身设了密码时，返回的对象里 {@code config}/{@code overview}
     * 为 null，只有 {@code access.locked=true} 与 {@code routes}。
     */
    Mono<LovePageVo> loveHome();

    /**
     * 故事列表（模块锁定时返回空 {@code ListResult}，不查库）。
     */
    Mono<ListResult<LoveStoryVo>> listLoveStories(Integer page, Integer size);

    /**
     * 相册列表（模块锁定时返回空；相册自身设密码时 {@code locked=true} 且
     * {@code photos} 为空，{@code cover} 仍返回 —— 与 app 端一致）。
     */
    Mono<ListResult<LoveAlbumVo>> listLoveAlbums(Integer page, Integer size);

    /**
     * 清单列表（模块锁定时返回空；{@code status} 为空表示不筛选）。
     *
     * @param status {@code wait} / {@code doing} / {@code complete}，或空串
     */
    Mono<ListResult<LoveDailyItemVo>> listLoveItems(String status, Integer page, Integer size);
}
