package cn.ialley.unihalo.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import cn.ialley.unihalo.scheme.AppVersion;

/**
 * 升级检测结果（checkVersion），字段与 uni-upgrade-center-app 的
 * （字段采用 snake_case 命名，与移动端既有解析逻辑兼容）。
 *
 * @author 小莫唐尼
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpgradeResult {

    /**
     * &gt;0 有更新 / 0 无更新 / &lt;0 错误
     */
    private int code;

    private String message;

    private String appid;

    private String name;

    private String title;

    private String contents;

    /**
     * 安装包下载地址（iOS 为 AppStore 链接）
     */
    private String url;

    /**
     * 适用平台：Android / iOS / Harmony
     */
    private List<String> platform;

    /**
     * 新版本号
     */
    private String version;

    /**
     * 安装包类型：native_app / wgt
     */
    private String type;

    @JsonProperty("is_mandatory")
    private Boolean isMandatory;

    @JsonProperty("is_silently")
    private Boolean isSilently;

    @JsonProperty("min_uni_version")
    private String minUniVersion;

    @JsonProperty("stable_publish")
    private Boolean stablePublish;

    /**
     * 下载方式：direct 直链下载（缺省）/ store 商店分发 / external 外部链接跳转
     */
    @JsonProperty("download_type")
    private String downloadType;

    /**
     * 外部链接（download_type=external 时输出）：网盘/落地页地址，客户端跳系统浏览器
     */
    @JsonProperty("external_url")
    private String externalUrl;

    /**
     * 外部链接的按钮文案（download_type=external 时输出），缺省「前往下载」
     */
    @JsonProperty("external_name")
    private String externalName;

    /**
     * 应用商店列表（download_type=store 时输出，多渠道下载入口；
     * 客户端按 priority 降序尝试跳转，全部失败回落 url 下载）
     */
    @JsonProperty("store_list")
    private List<StoreListItem> storeList;

    @Data
    public static class StoreListItem {
        private Boolean enable;
        private String id;
        private String name;
        private String scheme;
        private Integer priority;
    }

    public static UpgradeResult error(int code, String message) {
        UpgradeResult result = new UpgradeResult();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static UpgradeResult of(int code, String message, AppVersion appVersion) {
        UpgradeResult result = new UpgradeResult();
        result.setCode(code);
        result.setMessage(message);
        result.setAppid(appVersion.getSpec().getAppid());
        result.setName(appVersion.getSpec().getName());
        result.setTitle(appVersion.getSpec().getTitle());
        result.setContents(appVersion.getSpec().getContents());
        result.setUrl(appVersion.getSpec().getUrl());
        result.setPlatform(appVersion.getSpec().getPlatform());
        result.setVersion(appVersion.getSpec().getVersion());
        result.setType(appVersion.getSpec().getType());
        result.setIsMandatory(appVersion.getSpec().getIsMandatory());
        result.setIsSilently(appVersion.getSpec().getIsSilently());
        result.setMinUniVersion(appVersion.getSpec().getMinUniVersion());
        result.setStablePublish(appVersion.getSpec().getStablePublish());
        result.setDownloadType(appVersion.getSpec().getDownloadType());
        result.setStoreList(appVersion.getSpec().getStoreList() == null ? null
                : appVersion.getSpec().getStoreList().stream().map(channel -> {
                    StoreListItem item = new StoreListItem();
                    item.setEnable(channel.getEnable());
                    item.setId(channel.getId());
                    item.setName(channel.getName());
                    item.setScheme(channel.getScheme());
                    item.setPriority(channel.getPriority());
                    return item;
                }).toList());
        result.setExternalUrl(appVersion.getSpec().getExternalUrl());
        result.setExternalName(appVersion.getSpec().getExternalName());
        return result;
    }
}