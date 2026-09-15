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
 * 恋爱日记主题页 Finder（模板只读数据源）。
 *
 * <h3>契约（版本化，破坏兼容须升主版本）</h3>
 * <ul>
 *   <li>Finder 名固定为 {@code uniHaloFinder}（沿用已有空实现，未改名）；</li>
 *   <li>全部方法<b>只读</b>、<b>不抛异常</b>、空结果返回空 {@code ListResult}；</li>
 *   <li><b>锁判定在本层完成</b>：模块锁定时返回空结果，绝不把业务字段交给模板；</li>
 *   <li>返回<b>展示用脱敏 VO</b>，不含 {@code passwordHash} 等敏感字段。</li>
 * </ul>
 *
 * <h3>为什么返回 {@code ListResult} 而不是 {@code UrlContextListResult}</h3>
 * <p>Halo 官方 Finder（如 {@code PostFinder}）一律返回裸 {@code ListResult} ——
 * Finder 不碰 HTTP，拿不到当前请求路径，而 {@code UrlContextListResult} 的
 * {@code prevUrl}/{@code nextUrl} 必须由「知道路径的那一层」用
 * {@code PageUrlUtils} 拼出来。<br>
 * 本插件里「知道路径的那一层」= {@code LoveDiaryRouter} 的 route handler，
 * 它把 {@code ListResult} 包成 {@code UrlContextListResult} 后放进 model。<br>
 * <b>模板可见契约不变</b>：{@code ${stories.prevUrl}} / {@code ${stories.nextUrl}} /
 * {@code ${stories.hasNext()}} / {@code ${stories.getTotalPages()}} 照旧可用。</p>
 *
 * @author 小莫唐尼
 */
public interface UniHaloFinder {

    /**
     * 恋爱页配置（纪念日/恋人信息/模块入口列表）。
     *
     * <p>模块入口只包含<b>已注册路由</b>且 {@code enabled=true} 的模块，按
     * {@code priority} 降序；因此模板可以直接遍历，不需要再判"这个链接能不能点"。</p>
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
     * <p>恋爱日记入口本身设了密码时，返回的对象里 {@code config}/{@code overview}
     * 为 null，只有 {@code access.locked=true} 与 {@code routes}。</p>
     */
    Mono<LovePageVo> loveHome();

    /**
     * 故事列表（模块锁定时返回空 {@code ListResult}，<b>不查库</b>）。
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
