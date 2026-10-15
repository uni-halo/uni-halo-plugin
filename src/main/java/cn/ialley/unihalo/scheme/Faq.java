package cn.ialley.unihalo.scheme;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 常见问题（FAQ，多条，折叠问答模型）。
 *
 * 折叠列表展开仅展示 {@code description} 摘要，富文本 {@code content}
 * 在详情弹窗中展示；{@code links} 为相关链接按钮数组（外部链接/内部页面/客服按钮），
 * 客服按钮无需 url（小程序端 open-type=contact）。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "Faq", plural = "faqs", singular = "faq")
public class Faq extends AbstractExtension {

    private FaqSpec spec;

    @Data
    public static class FaqSpec {

        /**
         * 问题标题（必填）
         */
        private String title;

        /**
         * 摘要描述（折叠展开时显示的一句话摘要）
         */
        private String description;

        /**
         * 分类（软分组，app 端按此分组渲染，仅一个分类时不显示分组标题）
         */
        private String category;

        /**
         * 详细内容（富文本 HTML，详情弹窗展示）
         */
        private String content;

        /**
         * 相关链接（详情弹窗底部按钮，可空）
         */
        private List<FaqLink> links;

        /**
         * 是否显示（false = 仅控制台可见）
         */
        private Boolean visible;

        /**
         * 排序权重，越小越靠前，默认 0
         */
        private Integer priority;
    }

    @Data
    public static class FaqLink {

        /**
         * 按钮标题
         */
        private String title;

        /**
         * 链接类型：external 外部链接 / internal 内部页面 / contact 客服按钮
         */
        private String type;

        /**
         * 链接地址（外部链接 url / 内部页面路径；客服按钮无需填写）
         */
        private String url;
    }
}
