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
         * 作者用户名（服务端强制覆盖，客户端传值无效）
         */
        private String owner;

        /**
         * 作者昵称快照（列表展示用，避免逐条查 User）
         */
        private String ownerDisplayName;

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
    }
}
