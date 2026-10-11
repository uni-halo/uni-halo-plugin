package cn.ialley.unihalo.scheme;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 绘画作品：App 端用户在画布上创作并发布的图片作品，带审核状态。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "Draw", plural = "draws", singular = "draw")
public class Draw extends AbstractExtension {

    /** 竞猜状态：竞猜中 */
    public static final String PUZZLE_STATUS_GUESSING = "GUESSING";
    /** 竞猜状态：已揭晓 */
    public static final String PUZZLE_STATUS_REVEALED = "REVEALED";
    /** 自动揭晓人标识（系统惰性触发） */
    public static final String PUZZLE_REVEALER_SYSTEM = "system";

    private DrawSpec spec;

    @Data
    public static class DrawSpec {

        /**
         * 画作标题（必填，≤50 字）
         */
        private String title;

        /**
         * 画作简介（选填，≤200 字）
         */
        private String description;

        /**
         * 画作图片永久链接（必填，附件上传后的 permalink）
         */
        private String cover;

        /**
         * 关联附件名（删除留档，便于后续清理）
         */
        private String attachmentName;

        /**
         * 作者用户名（唯一标识，服务端强制覆盖，客户端传值无效；
         * owner 隔离与索引依据，昵称/头像由响应层实时解析）
         */
        private String ownerName;

        /**
         * 是否隐藏（管理端/作者可切换；隐藏后不在公开列表与详情中展示，
         * 不影响审核状态与作者本人查看）
         */
        private Boolean hidden;

        /**
         * 审核状态：PENDING（待审核）/ APPROVED（已通过）/ REJECTED（已拒绝）
         */
        private String status;

        /**
         * 审核说明（拒绝时为原因，展示给作者）
         */
        private String reason;

        /**
         * 审核人用户名
         */
        private String reviewedBy;

        /**
         * 审核时间（RFC3339）
         */
        private String reviewedAt;

        /**
         * 猜画谜底（≤20 字；仅出题人与管理端可见，公开响应层强制脱敏置空；
         * 非空即视为竞猜作品）
         */
        private String puzzleAnswer;

        /**
         * 猜画提示语（选填，公开可见）
         */
        private String puzzleHint;

        /**
         * 自动揭晓时间（RFC3339，选填；null = 永不自动揭晓，仅手动揭晓）
         */
        private String autoRevealAt;

        /**
         * 竞猜状态：GUESSING（竞猜中）/ REVEALED（已揭晓）；
         * 无谜底字段时无意义。揭晓由出题人/管理员手动或到自动揭晓时间惰性触发
         */
        private String puzzleStatus;

        /**
         * 揭晓时间（RFC3339）
         */
        private String revealedAt;

        /**
         * 揭晓人（手动 = 出题人/管理员用户名，自动 = system）
         */
        private String revealedBy;
    }

    /**
     * 作者信息（响应层实时解析 User 组装，非存储字段；语义同官方评论 OwnerInfo）。
     */
    @Data
    public static class DrawOwner {

        /**
         * 作者用户名
         */
        private String username;

        /**
         * 作者昵称（用户不存在时回退用户名）
         */
        private String displayName;

        /**
         * 作者头像链接
         */
        private String avatar;
    }
}
