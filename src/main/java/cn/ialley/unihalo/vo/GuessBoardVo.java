package cn.ialley.unihalo.vo;

import java.util.List;

import cn.ialley.unihalo.scheme.Draw;
import lombok.Data;
import run.halo.app.extension.MetadataOperator;

/**
 * 竞猜信息合一视图（公开）：榜单 + 参与统计，未揭晓不含谜底，已揭晓附谜底。
 *
 * @author 小莫唐尼
 */
@Data
public class GuessBoardVo {

    /**
     * 是否已揭晓
     */
    private Boolean revealed;

    /**
     * 谜底（仅已揭晓时非空）
     */
    private String answer;

    /**
     * 提示语（出题人填写，公开）
     */
    private String puzzleHint;

    /**
     * 自动揭晓时间（RFC3339，null = 永不自动揭晓）
     */
    private String autoRevealAt;

    /**
     * 参与人数（提交过作答的去重用户数）
     */
    private Long guessCount;

    /**
     * 猜中人数
     */
    private Long solvedCount;

    /**
     * 猜中榜（按猜中时间正序）
     */
    private List<Item> board;

    @Data
    public static class Item {

        private String username;

        private String displayName;

        private String avatar;

        /**
         * 猜中时刻（RFC3339）
         */
        private String solvedAt;
    }

    public static Item itemOf(String username, String displayName, String avatar, String solvedAt) {
        var item = new Item();
        item.setUsername(username);
        item.setDisplayName(displayName);
        item.setAvatar(avatar);
        item.setSolvedAt(solvedAt);
        return item;
    }

    public static GuessBoardVo of(Draw draw, long guessCount, long solvedCount, List<Item> board) {
        var vo = new GuessBoardVo();
        var spec = draw.getSpec();
        boolean revealed = spec != null && Draw.PUZZLE_STATUS_REVEALED.equals(spec.getPuzzleStatus());
        vo.setRevealed(revealed);
        vo.setAnswer(revealed && spec != null ? spec.getPuzzleAnswer() : null);
        vo.setPuzzleHint(spec == null ? null : spec.getPuzzleHint());
        vo.setAutoRevealAt(spec == null ? null : spec.getAutoRevealAt());
        vo.setGuessCount(guessCount);
        vo.setSolvedCount(solvedCount);
        vo.setBoard(board);
        return vo;
    }
}
