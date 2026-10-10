package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.Draw;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListResult;

/**
 * 绘画作品业务服务：公开浏览、我的绘画管理（owner 隔离）、发布审核判定与审核动作。
 *
 * @author 小莫唐尼
 */
public interface DrawService {

    /**
     * 公开列表（仅已通过，创建时间倒序）
     */
    Mono<ListResult<Draw>> listPublic(int page, int size);

    /**
     * 公开详情（仅已通过，未通过/待审核一律 404）
     */
    Mono<Draw> getPublic(String name);

    /**
     * 我的绘画列表（强制 owner=当前用户，含全部审核状态）
     */
    Mono<ListResult<Draw>> listMine(String username, int page, int size);

    /**
     * 我的绘画详情（非本人返回 404，不泄露存在性）
     */
    Mono<Draw> getMine(String name, String username);

    /**
     * 发布绘画：强制覆盖 owner/status，按 drawConfig.auditEnabled 判定是否进入待审核
     */
    Mono<Draw> create(Draw input, User author);

    /**
     * 删除我的绘画（先校验 owner，仅删记录保留附件）
     */
    Mono<Void> deleteMine(String name, String username);

    /**
     * 控制台列表（全状态，可按 status/keyword 筛选，待审核优先排序）
     */
    Mono<ListResult<Draw>> list(String status, String keyword, int page, int size);

    /**
     * 控制台详情（全状态可见）
     */
    Mono<Draw> getManage(String name);

    /**
     * 审核通过（审核结果通知作者）
     */
    Mono<Draw> approve(String name, String reviewer, String note);

    /**
     * 审核拒绝（reason 必填，审核结果通知作者）
     */
    Mono<Draw> reject(String name, String reviewer, String reason);
}
