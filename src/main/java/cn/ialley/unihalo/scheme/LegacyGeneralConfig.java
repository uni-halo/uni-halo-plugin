package cn.ialley.unihalo.scheme;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * TODO-TEMPORARY-MIGRATION：旧「通用配置」Scheme（改名前的 kind=GeneralConfig /
 * plural=generalConfigs / 单例名 general-config）。
 *
 * <p>仅用于把存量单例的 spec 复制为新 {@link FeatureConfig}（feature-config），
 * 迁移完成经用户确认后，本类与迁移代码将一并删除；勿在新业务代码中引用。</p>
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "GeneralConfig", plural = "generalConfigs", singular = "generalConfig")
public class LegacyGeneralConfig extends AbstractExtension {

    /**
     * 旧 spec 原样承载（不解析字段，迁移时整树复制，避免字段演进导致丢数据）。
     */
    private JsonNode spec;
}
