package cn.ialley.unihalo.vo;

import java.util.List;

import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.scheme.DrawGuess;
import lombok.Data;

/**
 * 竞猜管理信息视图（仅出题人与管理端可见，含谜底原文与竞猜记录）。
 *
 * @author 小莫唐尼
 */
@Data
public class PuzzleInfoVo {

    private String drawName;

    private String puzzleAnswer;

    private String puzzleHint;

    private String autoRevealAt;

    private String puzzleStatus;

    private String revealedAt;

    private String revealedBy;

    /**
     * 参与人数（提交过作答的去重用户数）
     */
    private long guessCount;

    /**
     * 猜中人数
     */
    private long solvedCount;

    /**
     * 竞猜记录（每位猜者最近一次答案与对错，按猜中/作答时间排序）
     */
    private List<Record> records;

    @Data
    public static class Record {

        private String username;

        private String displayName;

        private String avatar;

        /**
         * 猜者最近一次提交的答案
         */
        private String answer;

        private Boolean correct;

        /**
         * 猜中时刻（RFC3339，未猜中为空）
         */
        private String solvedAt;

        /**
         * 最近作答时间（RFC3339，取记录创建时间）
         */
        private String answeredAt;
    }

    public static Record recordOf(DrawGuess guess, String displayName, String avatar) {
        var spec = guess.getSpec();
        var record = new Record();
        record.setUsername(spec == null ? null : spec.getUsername());
        record.setDisplayName(displayName);
        record.setAvatar(avatar);
        record.setAnswer(spec == null ? null : spec.getAnswer());
        record.setCorrect(spec != null && Boolean.TRUE.equals(spec.getCorrect()));
        record.setSolvedAt(spec == null ? null : spec.getSolvedAt());
        record.setAnsweredAt(guess.getMetadata() == null || guess.getMetadata().getCreationTimestamp() == null
                ? null : guess.getMetadata().getCreationTimestamp().toString());
        return record;
    }

    public static PuzzleInfoVo of(Draw draw, List<Record> records, long guessCount, long solvedCount) {
        var spec = draw.getSpec();
        var vo = new PuzzleInfoVo();
        vo.setDrawName(draw.getMetadata() == null ? null
                : draw.getMetadata().getName());
        vo.setPuzzleAnswer(spec == null ? null : spec.getPuzzleAnswer());
        vo.setPuzzleHint(spec == null ? null : spec.getPuzzleHint());
        vo.setAutoRevealAt(spec == null ? null : spec.getAutoRevealAt());
        vo.setPuzzleStatus(spec == null ? null : spec.getPuzzleStatus());
        vo.setRevealedAt(spec == null ? null : spec.getRevealedAt());
        vo.setRevealedBy(spec == null ? null : spec.getRevealedBy());
        vo.setRecords(records);
        vo.setGuessCount(guessCount);
        vo.setSolvedCount(solvedCount);
        return vo;
    }
}
