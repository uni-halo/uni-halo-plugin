package cn.ialley.unihalo.scheme;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 恋爱信息（单例，固定 {@code metadata.name = "love-info"}）：纪念日 + 恋人信息，
 * 在「恋爱管理-恋爱信息」维护，app 端与主题模板各自消费。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "LoveInfo", plural = "loveInfos", singular = "loveInfo")
public class LoveInfo extends AbstractExtension {

    private LoveInfoSpec spec;

    @Data
    public static class LoveInfoSpec {

        /** 纪念日标题（如「这是我们一起走过的」，留空由模板/app 端回落默认文案） */
        private String loveDateTitle;

        /** 恋爱纪念日（yyyy-MM-dd），用于计算恋爱天数 */
        private String loveDate;

        /** 男生昵称 */
        private String boyNickname;

        /** 男生头像 */
        private String boyAvatar;

        /** 女生昵称 */
        private String girlNickname;

        /** 女生头像 */
        private String girlAvatar;
    }
}
