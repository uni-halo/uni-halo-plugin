package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.vo.DrawVo;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;

/**
 * 绘画作品业务服务：公开浏览、我的绘画管理（owner 隔离）、发布审核判定与审核动作。
 * 查询/审核结果一律返回 {@link DrawVo}（owner 实时解析 User）。
 *
 * @author 小莫唐尼
 */
public interface DrawService {

    /**
     * 公开列表（仅已通过且未隐藏，创建时间倒序；响应脱敏不含谜底）
     */
    Mono<ListResult<DrawVo>> listPublic(int page, int size);

    /**
     * 公开列表（带分类：all=全部 / normal=普通作品 / puzzle=竞猜作品）
     */
    Mono<ListResult<DrawVo>> listPublic(int page, int size, String type);

    /**
     * 公开详情（仅已通过且未隐藏，未通过/待审核/已隐藏一律 404）
     */
    Mono<DrawVo> getPublic(String name);

    /**
     * 我的绘画列表（强制 ownerName=当前用户，含全部审核状态；status 非空时按状态筛选）
     */
    Mono<ListResult<DrawVo>> listMine(String username, String status, int page, int size);

    /**
     * 我的绘画详情（非本人返回 404，不泄露存在性）
     */
    Mono<DrawVo> getMine(String name, String username);

    /**
     * 发布绘画：强制覆盖 ownerName/status，按 featureConfig.pages.draw.auditEnabled 判定是否进入待审核
     */
    Mono<DrawVo> create(Draw input, User author);

    /**
     * 删除我的绘画（先校验 owner，仅删记录保留附件）
     */
    Mono<Void> deleteMine(String name, String username);

    /**
     * 切换我的绘画显隐（先校验 owner；隐藏仅影响公开展示）
     */
    Mono<DrawVo> setMineHidden(String name, String username, boolean hidden);

    /**
     * 控制台列表（全状态，可按 status/keyword 筛选；sort=createdAt/reviewedAt/revealedAt 指定排序，
     * 缺省待审核优先 + 提交时间倒序；type=normal/puzzle 按普通/竞猜筛选，
     * hidden=true/false 按显隐筛选）
     */
    Mono<ListResult<DrawVo>> list(String status, String keyword, String sort, String type,
            String hidden, int page, int size);

    /**
     * 控制台详情（全状态可见）
     */
    Mono<DrawVo> getManage(String name);

    /**
     * 控制台删除（仅删记录保留附件）
     */
    Mono<Void> deleteManage(String name);

    /**
     * 控制台切换显隐（隐藏仅影响公开展示）
     */
    Mono<DrawVo> setHidden(String name, boolean hidden);

    /**
     * 审核通过（审核结果通知作者）
     */
    Mono<DrawVo> approve(String name, String reviewer, String note);

    /**
     * 审核拒绝（reason 必填，审核结果通知作者）
     */
    Mono<DrawVo> reject(String name, String reviewer, String reason);
}
