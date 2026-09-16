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
    public static final String END_POINT_API_BASE_PATH = "plugins/uni-halo";

    /**
     * 应用升级（公开 checkVersion）接口基础路径
     */
    public static final String UPGRADE_API_BASE_PATH = "plugins/uni-halo/upgrade";

    /**
     * 应用管理（console）接口基础路径
     */
    public static final String APP_INFO_API_BASE_PATH = "plugins/uni-halo/apps";

    /**
     * 应用升级管理（console）接口基础路径
     */
    public static final String APP_VERSION_API_BASE_PATH = "plugins/uni-halo/app-versions";

    /**
     * 恋爱相册（console）接口基础路径
     */
    public static final String LOVE_ALBUM_API_BASE_PATH = "plugins/uni-halo/love-albums";

    /**
     * 恋爱清单（console）接口基础路径
     */
    public static final String LOVE_DAILY_API_BASE_PATH = "plugins/uni-halo/love-daily-items";

    /**
     * 恋爱故事（console）接口基础路径
     */
    public static final String LOVE_STORY_API_BASE_PATH = "plugins/uni-halo/love-stories";

    /**
     * 通知公告（console）接口基础路径
     */
    public static final String NOTICE_API_BASE_PATH = "plugins/uni-halo/notices";

    /**
     * 公告类型（console）接口基础路径
     */
    public static final String NOTICE_TYPE_API_BASE_PATH = "plugins/uni-halo/notice-types";

    /**
     * 轮播图（console/公开）接口基础路径
     */
    public static final String BANNER_API_BASE_PATH = "plugins/uni-halo/banners";

    /**
     * 审核模式（console/公开）接口基础路径
     */
    public static final String AUDIT_DATA_API_BASE_PATH = "plugins/uni-halo/audit-data";

    /**
     * 验证码（公开）接口基础路径
     */
    public static final String CAPTCHA_API_BASE_PATH = "plugins/uni-halo/captcha/generate";

    /**
     * 移动端登录认证（公开）接口基础路径
     */
    public static final String AUTH_API_BASE_PATH = "plugins/uni-halo/auth";

    /**
     * 微信绑定关系（console）接口基础路径
     */
    public static final String WECHAT_USER_API_BASE_PATH = "plugins/uni-halo/wechat-users";

    /**
     * 微信绑定使用的 registrationId（UserConnection.spec.registrationId）
     */
    public static final String WECHAT_REGISTRATION_ID = "wechat-miniprogram";

    /**
     * Halo 匿名身份的用户名（AnonymousAuthenticationToken#getName）。
     * 写操作端点据此显式拒绝匿名调用（fail-closed），不做无害空转。
     */
    public static final String ANONYMOUS_USER = "anonymousUser";

    /**
     * 微信自动注册用户名的默认前缀。最终用户名 = 前缀 + 两位序号，如 {@code unihalo01}。
     */
    public static final String DEFAULT_WECHAT_USERNAME_PREFIX = "unihalo";

    /**
     * 用户名前缀合法性与 {@code ValidationUtils.NAME_REGEX} 一致（小写字母数字 + 连字符）。
     * 前缀非法时回落到 {@link #DEFAULT_WECHAT_USERNAME_PREFIX}，避免注册直接失败。
     */
    public static final String USERNAME_PREFIX_REGEX = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$";

    /**
     * Halo 用户名最短长度（{@code SignUpData.username} 的 {@code @Size(min = 4)}）。
     */
    public static final int USERNAME_MIN_LENGTH = 4;

    /**
     * Halo 用户名最长长度（{@code SignUpData.username} 的 {@code @Size(max = 63)}）。
     */
    public static final int USERNAME_MAX_LENGTH = 63;

    /**
     * 登录令牌归属标签键：值固定为 {@link #PAT_MANAGED_BY_VALUE}。
     * 用于区分本插件签发的令牌与用户在「个人中心 → 个人令牌」手动创建的令牌，
     * 过期清理只回收带该标签的令牌，绝不碰用户自己创建的。
     */
    public static final String PAT_MANAGED_BY_LABEL = "unihalo.ialley.cn/managed-by";

    /**
     * 登录令牌归属标签值（插件名）
     */
    public static final String PAT_MANAGED_BY_VALUE = "uni-halo";

    /**
     * 绑定类二维码内容前缀（{@code uh-bindwx-{ticket}}）。
     * 小程序端按前缀识别业务类型并分发处理；后续其他扫码业务
     * （如扫码登录）使用各自前缀并列。
     */
    public static final String QR_BIND_WECHAT_PREFIX = "uh-bindwx-";

    /**
     * 友情链接-小程序链接（console/公开）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_API_BASE_PATH =
        "plugins/uni-halo/mini-program-links";

    /**
     * 友情链接-小程序链接申请单（console/公开）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_SUBMISSION_API_BASE_PATH =
        "plugins/uni-halo/mini-program-link-submissions";

    /**
     * 友情链接-分组（console）接口基础路径
     */
    public static final String MINI_PROGRAM_LINK_GROUP_API_BASE_PATH =
        "plugins/uni-halo/mini-program-link-groups";

    /**
     * 审核模式单例名称（metadata.name 固定值）
     */
    public static final String AUDIT_DATA_CONFIG_SINGLETON_NAME = "audit-data-config";

    /**
     * 功能设置（FeatureConfig）单例名称（metadata.name 固定值）
     */
    public static final String FEATURE_CONFIG_SINGLETON_NAME = "feature-config";

    /**
     * 功能设置（console）接口基础路径
     */
    public static final String FEATURE_CONFIG_API_BASE_PATH =
        "plugins/uni-halo/feature-config";

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

    // ==================== 恋爱日记主题模板（前台页面） ====================

    /**
     * 插件模板名前缀：Halo 模板片段引用与主题覆盖均以此为前缀，
     * 形如 {@code plugin:uni-halo:fragments/love-icons}。
     */
    public static final String THEME_TEMPLATE_PREFIX = "plugin:uni-halo:";

    /**
     * {@code _templateId} 前缀：恋爱日记四页的模板 ID 均以此开头，
     * HeadProcessor 据此判断是否注入资源（前缀匹配，fail-closed）。
     */
    public static final String LOVE_TEMPLATE_ID_PREFIX = "plugin:uni-halo:love";

    /**
     * 恋爱日记四页的模板 ID（{@code _templateId} 取值，属主题集成契约，不可随意变更）。
     */
    public static final String LOVE_TEMPLATE_ID_HOME = "plugin:uni-halo:love";

    public static final String LOVE_TEMPLATE_ID_STORIES = "plugin:uni-halo:love-stories";

    public static final String LOVE_TEMPLATE_ID_ALBUMS = "plugin:uni-halo:love-albums";

    public static final String LOVE_TEMPLATE_ID_DAILY = "plugin:uni-halo:love-daily";

    /**
     * 插件静态资源前缀（由 {@code uni-halo-reverse-proxy.yaml} 的
     * {@code /static/** → directory: static} 提供，与 Halo 主机同源，不走 CDN）。
     */
    public static final String PLUGIN_STATIC_PREFIX = "/plugins/uni-halo/assets/static";

    /**
     * 恋爱日记前端资产目录（相对 {@link #PLUGIN_STATIC_PREFIX}）。
     */
    public static final String LOVE_STATIC_DIR = "/love-diary";

    /**
     * 设置域组名（setting.yaml 的 Tab「主题展示」）。
     */
    public static final String SETTING_DOMAIN_THEME_CONFIG = "themeConfig";

    /**
     * 恋爱日记主题模板设置组名（themeConfig.loveDiaryTheme）。
     */
    public static final String SETTING_MODULE_LOVE_DIARY_THEME = "loveDiaryTheme";

    /**
     * 模块入口 key（与 FeatureConfig.Love 的字段名一致，app 端契约）。
     */
    public static final String LOVE_MODULE_DIARY = "loveDiary";

    public static final String LOVE_MODULE_OUR_STORY = "ourStory";

    public static final String LOVE_MODULE_PHOTO = "lovePhoto";

    public static final String LOVE_MODULE_DAILY = "loveDaily";

    /**
     * 列表页分页路径段（Halo 官方范式：{@code /page/{page}}，非 {@code ?page=N}）。
     */
    public static final String PAGE_SEGMENT = "page";

    /**
     * 默认分页大小（与 Halo {@code ModelConst.DEFAULT_PAGE_SIZE} 对齐）。
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

}