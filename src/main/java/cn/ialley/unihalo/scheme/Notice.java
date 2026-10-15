package cn.ialley.unihalo.scheme;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 通知公告（多条）。
 *
 * 状态：draft 草稿 / published 已发布（唯一对 app 端可见）/ offline 已下线。
 * content 为富文本 HTML；summary 摘要手填优先、为空时服务端保存自动剥离生成；
 * typeName 引用 {@link NoticeType} 的 metadata.name；publishTime 在
 * 状态变更为 published 时由服务端自动记录。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "Notice", plural = "notices", singular = "notice")
public class Notice extends AbstractExtension {

    private NoticeSpec spec;

    @Data
    public static class NoticeSpec {

        /**
         * 标题（必填）
         */
        private String title;

        /**
         * 富文本正文（编辑器输出的 HTML）
         */
        private String content;

        /**
         * 摘要（手填可选；为空时保存自动从 content 剥离生成，截断 200 字）
         */
        private String summary;

        /**
         * 封面图（Halo 附件 URL）
         */
        private String cover;

        /**
         * 链接地址（按 linkType 语义使用：external/wechat-mp 为完整 URL，
         * internal 为 app 内页面路径；miniapp 类型不使用此字段）
         */
        private String link;

        /**
         * 链接类型：none 无（默认，不展示跳转入口）/ external 外部链接 /
         * miniapp 小程序链接 / internal 内部页面 / wechat-mp 微信公众号文章；
         * 为空时按 link 非空回退为 external（兼容历史数据）
         */
        private String linkType;

        /**
         * 小程序 appId（仅 linkType=miniapp）
         */
        private String linkAppId;

        /**
         * 小程序内页面路径（仅 linkType=miniapp，可空=首页）
         */
        private String linkPath;

        /**
         * 公告类型（引用 NoticeType 的 metadata.name，为空=不分类）
         */
        private String typeName;

        /**
         * 状态：draft / published / offline，默认 draft
         */
        private String status;

        /**
         * 排序，越大越靠前，默认 0
         */
        private Integer priority;

        /**
         * 发布时间（状态变更为 published 时服务端自动记录，ISO-8601）
         */
        private String publishTime;
    }
}
