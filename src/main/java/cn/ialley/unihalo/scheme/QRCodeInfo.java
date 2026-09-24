package cn.ialley.unihalo.scheme;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 二维码信息扩展模型：按 key 或文章 ID 关联二维码图片地址。
 *
 * @author lywq
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "QRCodeInfo", plural = "qRCodeInfos", singular = "qRCodeInfo")
public class QRCodeInfo extends AbstractExtension {

    private String key;
    private String postId;
    private String imageUrl;

}