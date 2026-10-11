package cn.ialley.unihalo.scheme;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 猜画竞猜记录：用户对某幅竞猜画作的作答。
 *
 * <p>metadata.name 固定为 {@code drawguess-{drawName}-{username}}，
 * 同一用户对同一画作天然只有一条记录：猜错为更新记录（保留最后一次答案），
 * 猜中为将 correct 置 true（仅此一次，solvedAt 记录猜中时刻）。
 * 并发提交被 name 唯一性兜底，无需额外查重。
 *
 * <p>画作删除/隐藏不清理本记录（积分不回收，历史得分永久有效）。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "DrawGuess", plural = "drawguesses", singular = "drawguess")
public class DrawGuess extends AbstractExtension {

    private DrawGuessSpec spec;

    @Data
    public static class DrawGuessSpec {

        /**
         * 关联画作名（Draw 的 metadata.name）
         */
        private String drawName;

        /**
         * 猜者用户名
         */
        private String username;

        /**
         * 最近一次提交的答案
         */
        private String answer;

        /**
         * 是否猜中（每用户每画至多一次 true）
         */
        private Boolean correct;

        /**
         * 最近一次答案是否接近（归一化编辑距离 ≤1，不泄露谜底）
         */
        private Boolean close;

        /**
         * 猜中时刻（RFC3339，猜中榜排序依据）
         */
        private String solvedAt;
    }
}
