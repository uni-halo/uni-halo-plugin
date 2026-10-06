package cn.ialley.unihalo.scheme;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 应用版本（应用升级），对应 uni-upgrade-center 的 opendb-app-versions。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "AppVersion", plural = "appVersions", singular = "appVersion")
public class AppVersion extends AbstractExtension {

    private AppVersionSpec spec;

    @Data
    public static class AppVersionSpec {

        /**
         * 应用标识（关联 AppInfo.appid）
         */
        private String appid;

        /**
         * 应用名称
         */
        private String name;

        /**
         * 更新标题
         */
        private String title;

        /**
         * 更新内容
         */
        private String contents;

        /**
         * 更新平台：Android / iOS / Harmony
         */
        private List<String> platform;

        /**
         * 安装包类型：native_app / wgt
         */
        private String type;

        /**
         * 版本号（应用版本名称），须大于当前线上发行版本；公开接口 checkVersion 以此比较
         */
        private String version;

        /**
         * 应用版本号（整数），须大于该应用已发布的最大值
         */
        private Integer versionCode;

        /**
         * wgt 所需最低原生 App 版本
         */
        private String minUniVersion;

        /**
         * 安装包下载/跳转链接；iOS 为 AppStore 链接。store 形态下作为全部商店
         * 跳转失败时的回落下载地址，external 形态下作为旧版客户端兜底下载地址
         */
        private String url;

        /**
         * 下载方式：direct 直链下载（缺省）/ store 商店分发 / external 外部链接跳转。
         * wgt 包恒为 direct；iOS 平台恒走 AppStore（url），不受此值影响
         */
        private String downloadType;

        /**
         * 应用商店分发渠道（downloadType=store 时生效；按 priority 降序尝试跳转）
         */
        private List<StoreChannel> storeList;

        /**
         * 外部链接（downloadType=external 时生效）：网盘/落地页地址，
         * 客户端跳系统浏览器打开，不做应用内下载
         */
        private String externalUrl;

        /**
         * 外部链接的按钮文案（downloadType=external 时生效），缺省「前往下载」
         */
        private String externalName;

        /**
         * 是否上线发行（同 appid+platform+type 同时仅一个 true）
         */
        private Boolean stablePublish;

        /**
         * 是否静默更新（仅 wgt）
         */
        private Boolean isSilently;

        /**
         * 是否强制更新
         */
        private Boolean isMandatory;
    }

    /**
     * 应用商店分发渠道，字段与客户端 store_list 解析结构一一对应。
     */
    @Data
    public static class StoreChannel {

        /**
         * 是否启用（未启用的渠道仅存库不下发）
         */
        private Boolean enable;

        /**
         * 渠道标识：huawei / xiaomi / oppo / vivo / tencent / 自定义
         */
        private String id;

        /**
         * 渠道显示名
         */
        private String name;

        /**
         * 商店跳转 scheme
         */
        private String scheme;

        /**
         * 优先级，数值大先尝试
         */
        private Integer priority;
    }
}