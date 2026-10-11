package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 猜画积分排行榜条目：score = 累计猜中画作数（猜对一幅 +1，画作删除/隐藏不回收）。
 *
 * @author 小莫唐尼
 */
@Data
public class GuessRankItem {

    /**
     * 名次（1 起）
     */
    private int rank;

    private String username;

    private String displayName;

    private String avatar;

    /**
     * 累计积分（猜中画作数）
     */
    private long score;
}
