package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.vo.GuessBoardVo;
import cn.ialley.unihalo.vo.GuessRankItem;
import cn.ialley.unihalo.vo.GuessResultVo;
import cn.ialley.unihalo.vo.PuzzleInfoVo;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 猜画竞猜业务服务：作答、竞猜信息、积分排行榜、竞猜管理与揭晓。
 *
 * @author 小莫唐尼
 */
public interface DrawGuessService {

    /**
     * 惰性揭晓：竞猜中且已到自动揭晓时间则翻转为 REVEALED（幂等，乐观锁失败静默忽略，
     * 下次读取再触发）。所有竞猜读路径与作答入口必须先调用。
     */
    Mono<Draw> revealIfDue(Draw draw);

    /**
     * 审核通过时的自动揭晓顺延：揭晓时间已过或剩余不足最小竞猜窗口时，
     * 顺延为「当前时间 + 顺延时长」（供 DrawServiceImpl.approve 调用，返回原对象或顺延后的对象）
     */
    Mono<Draw> postponeAutoRevealOnApprove(Draw draw);

    /**
     * 提交作答（仅登录用户；出题人本人、非公开/非竞猜中画作、已猜中者拒绝）。
     * 猜错更新记录保留最后一次答案，猜中置 correct 并记录 solvedAt（每用户每画一次）。
     */
    Mono<GuessResultVo> guess(String drawName, String username, String answer);

    /**
     * 竞猜信息合一（匿名可访问）：榜单 + 统计，未揭晓不含谜底；入口先执行惰性揭晓检查
     */
    Mono<GuessBoardVo> board(String drawName, String viewerUsername);

    /**
     * 猜画积分排行榜（匿名可访问）：按累计猜中画作数降序，top 限制返回条数
     */
    Mono<List<GuessRankItem>> rank(int top);

    /**
     * 竞猜管理信息（仅出题人）：谜底原文、竞猜记录
     */
    Mono<PuzzleInfoVo> myPuzzle(String drawName, String username);

    /**
     * 竞猜管理信息（管理端，无 owner 限制）
     */
    Mono<PuzzleInfoVo> managePuzzle(String drawName);

    /**
     * 手动揭晓（仅出题人，幂等：已揭晓直接返回）
     */
    Mono<Void> revealByOwner(String drawName, String username);

    /**
     * 手动揭晓（管理端，幂等）
     */
    Mono<Void> revealByManager(String drawName, String reviewer);
}
