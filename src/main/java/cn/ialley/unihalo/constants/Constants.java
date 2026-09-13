package cn.ialley.unihalo.constants;

/**
 * 插件使用到的常量定义
 *
 * @author 小莫唐尼
 */
public class Constants {
    /**
     * 接口版本
     */
    public static final String PLUGIN_API_VERSION = "v1alpha1";

    /**
     * endpoint 中的接口基础路径
     */
    public static final String END_POINT_API_BASE_PATH = "plugins/uni-halo-plugin";

    /**
     * 应用升级（公开 checkVersion）接口基础路径
     */
    public static final String UPGRADE_API_BASE_PATH = "plugins/uni-halo-plugin/upgrade";

    /**
     * 应用管理（console）接口基础路径
     */
    public static final String APP_INFO_API_BASE_PATH = "plugins/uni-halo-plugin/apps";

    /**
     * 应用升级管理（console）接口基础路径
     */
    public static final String APP_VERSION_API_BASE_PATH = "plugins/uni-halo-plugin/app-versions";

    /**
     * 恋爱相册（console）接口基础路径
     */
    public static final String LOVE_ALBUM_API_BASE_PATH = "plugins/uni-halo-plugin/love-albums";

    /**
     * 恋爱清单（console）接口基础路径
     */
    public static final String LOVE_DAILY_API_BASE_PATH = "plugins/uni-halo-plugin/love-daily-items";

    /**
     * 恋爱故事（console）接口基础路径
     */
    public static final String LOVE_STORY_API_BASE_PATH = "plugins/uni-halo-plugin/love-stories";

    /**
     * 通知公告（console）接口基础路径
     */
    public static final String NOTICE_API_BASE_PATH = "plugins/uni-halo-plugin/notices";

    /**
     * 公告类型（console）接口基础路径
     */
    public static final String NOTICE_TYPE_API_BASE_PATH = "plugins/uni-halo-plugin/notice-types";

    /**
     * 轮播图（console/公开）接口基础路径
     */
    public static final String BANNER_API_BASE_PATH = "plugins/uni-halo-plugin/banners";

    /**
     * 审核模式（console/公开）接口基础路径
     */
    public static final String AUDIT_DATA_API_BASE_PATH = "plugins/uni-halo-plugin/audit-data";

    /**
     * 验证码（公开）接口基础路径
     */
    public static final String CAPTCHA_API_BASE_PATH = "plugins/uni-halo-plugin/captcha/generate";

    /**
     * 友情链接-小程序链接（console/公开）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_API_BASE_PATH =
        "plugins/uni-halo-plugin/mini-program-links";

    /**
     * 友情链接-小程序链接申请单（console/公开）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_SUBMISSION_API_BASE_PATH =
        "plugins/uni-halo-plugin/mini-program-link-submissions";

    /**
     * 友情链接-分组（console）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_GROUP_API_BASE_PATH =
        "plugins/uni-halo-plugin/mini-program-link-groups";

    /**
     * 审核模式单例名称（metadata.name 固定值）
     */
    public static final String AUDIT_DATA_CONFIG_SINGLETON_NAME = "audit-data-config";

    /**
     * 通用配置（GeneralConfig）单例名称（metadata.name 固定值）
     */
    public static final String GENERAL_CONFIG_SINGLETON_NAME = "general-config";

    /**
     * 通用配置（console）接口基础路径
     */
    public static final String GENERAL_CONFIG_API_BASE_PATH =
        "plugins/uni-halo-plugin/general-config";

    /**
     * 基础的域名地址
     */
    public static final String BASIC_DOMAIN_NAME = "unihalo.ialley.cn";

    /**
     * 基础的域名接口地址
     */
    public static final String BASIC_DOMAIN_API_NAME = "api." + BASIC_DOMAIN_NAME;

    /**
     * 自定义接口分组名称（给后台管理提供的接口）
     */
    public static final String CONSOLE_CUSTOM_API_GROUP_NAME =
        "console." + BASIC_DOMAIN_API_NAME + "/" + PLUGIN_API_VERSION;

    /**
     * 自定义接口分组名称（给用户中心提供的接口）
     */
    public static final String UC_CUSTOM_API_GROUP_NAME =
        "uc." + BASIC_DOMAIN_API_NAME + "/" + PLUGIN_API_VERSION;

    /**
     * 自定义接口分组名称
     */
    public static final String PUBLIC_CUSTOM_API_GROUP_NAME =
        BASIC_DOMAIN_API_NAME + "/" + PLUGIN_API_VERSION;

}