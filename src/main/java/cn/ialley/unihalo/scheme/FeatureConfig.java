package cn.ialley.unihalo.scheme;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 功能设置（单例，metadata.name 固定为 feature-config），承载小程序通用内容与外观配置。
 * 区块：{@link Profile} 应用资料、{@link Pages} 首页/图库/关于页视觉、{@link Assets}
 * 兜底资源、{@link Preferences} 站点级展示偏好默认（L0）、{@link Love} 恋爱模块开关、
 * {@link Maintenance} 维护模式排期。
 *
 * 本模型是控制台「功能设置」页的写端事实源；小程序端经 {@code getConfigs} 读取
 * （{@code featureConfig} 键整体下发本模型 spec，密码字段脱敏）。
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "FeatureConfig", plural = "featureConfigs", singular = "featureConfig")
public class FeatureConfig extends AbstractExtension {

    private Spec spec;

    @Data
    public static class Spec {
        private Profile profile;
        private Pages pages;
        private Assets assets;
        private Preferences preferences;
        private Love love;
        /** 友链信息：站长小程序展示信息（featureConfig.linkInfo 下发），
         * 供小程序端「申请信息」弹窗展示 */
        private LinkInfo linkInfo;
        /** 维护模式 */
        private Maintenance maintenance;
    }

    /** 应用资料：应用信息（名称/图标）+ 博主/社交信息；
     * 页脚版权显示于【关于】页面页脚 */
    @Data
    public static class Profile {
        /** 应用信息 */
        private AppInfo appInfo;
        private Blogger blogger;
        private Social social;
        /** 页脚版权（显示于【关于】页面页脚） */
        private Copyright copyrightConfig;
    }

    /** 应用信息（应用名称/图标） */
    @Data
    public static class AppInfo {
        private String name;
        private String logo;
    }

    /** 博主信息（昵称/头像/邮箱/简介/主页/介绍） */
    @Data
    public static class Blogger {
        private String nickname;
        private String avatar;
        private String email;
        private String description;
        /** 主页 */
        private String website;
        /** 介绍（富文本 HTML，app 端联系博主页 mp-html 渲染） */
        private String intro;
    }

    /** 社交信息（动态列表） */
    @Data
    public static class Social {
        /** 社交项列表（app 端联系博主页按序渲染；数组顺序 = 展示顺序） */
        private List<SocialItem> items;
    }

    /** 社交项（app 端联系博主页展示/复制；名称/内容/颜色/背景色/排序/显隐，
     * 图标由 app 端按 color/bgColor 色块渲染） */
    @Data
    public static class SocialItem {
        /** 名称（如「企鹅号」「微信号」） */
        private String name;
        /** 内容（账号/地址/链接，点击复制） */
        private String content;
        /** 图标颜色（16 进制，支持透明） */
        private String color;
        /** 背景色（16 进制，支持透明） */
        private String bgColor;
        /** 排序（越大越靠前，与 Banner.priority 同语义） */
        private Integer priority;
        /** 是否展示 */
        private Boolean visible;
    }

    /** 页脚版权（显示于【关于】页面） */
    @Data
    public static class Copyright {
        private Boolean enabled;
        private String content;
    }

    /** 免责声明页（不再需要启用开关，仅维护内容） */
    @Data
    public static class DisclaimerPage {
        private String content;
    }

    /** 文章详情页内容与版权文案 */
    @Data
    public static class PostDetailPage {
        /** 是否显示评论列表 */
        private Boolean showComment;
        /** 是否开启评论（服务端经系统设置 comment.enable 注入） */
        private Boolean enableComment;
        /** 是否使用文章版权 */
        private Boolean copyrightEnabled;
        private String copyrightAuthor;
        private String copyrightDesc;
        private String copyrightViolation;
    }

    /** 瞬间页评论配置 */
    @Data
    public static class MomentPage {
        /** 是否显示评论列表 */
        private Boolean showCommentList;
        /** 是否开启评论 */
        private Boolean enableComment;
    }

    /** 页面与排版：全站页面标题、首页/博主页/文章详情页/免责声明页视觉与功能 */
    @Data
    public static class Pages {
        /** 全站页面标题（页面设置-页面标题 tab 统一维护） */
        private PageTitles titles;
        private HomePage home;
        /** 瞬间页评论配置 */
        private MomentPage moment;
        /** 博主页（资料卡视觉 + 常用功能布局，配置并入「博主页」tab） */
        private BloggerPage blogger;
        /** 我的页面功能入口（常用功能/其他功能两组，配置并入「博主页」tab，
         * 随 {@code featureConfig.pages.mine} 下发） */
        private MinePage mine;
        /** 免责声明页（不再需要启用开关，仅内容） */
        private DisclaimerPage disclaimer;
        /** 文章详情页内容与版权文案 */
        private PostDetailPage postDetail;
        /** 用户协议页（页面设置-用户协议 tab 维护，
         * 随 {@code featureConfig.pages.userAgreement} 下发，app 端注册页/协议页渲染） */
        private UserAgreementPage userAgreement;
        /** 隐私政策页（页面设置-隐私政策 tab 维护，
         * 随 {@code featureConfig.pages.privacyPolicy} 下发，app 端注册页/协议页渲染） */
        private PrivacyPolicyPage privacyPolicy;
    }

    /** 用户协议页（注册流程与独立协议页共用内容）。内容为富文本 HTML
     * （RichTextEditorField 编辑）；enabled=false 或留空 = 站点未启用/未配置，
     * app 端注册页回退为静态提示文案，协议页展示空态。 */
    @Data
    public static class UserAgreementPage {
        /** 是否启用用户协议页面（注册页勾选行/协议入口显隐） */
        private Boolean enabled;
        /** 用户协议内容（富文本 HTML） */
        private String content;
    }

    /** 隐私政策页（注册流程与独立协议页共用内容）。内容为富文本 HTML
     * （RichTextEditorField 编辑）；enabled=false 或留空 = 站点未启用/未配置，
     * app 端注册页回退为静态提示文案，协议页展示空态。 */
    @Data
    public static class PrivacyPolicyPage {
        /** 是否启用隐私政策页面（注册页勾选行/协议入口显隐） */
        private Boolean enabled;
        /** 隐私政策内容（富文本 HTML） */
        private String content;
    }

    /** 全站页面标题（app 端经 pages.titles 读取，传入各页面 uh-navbar default-title，留空回退内置默认） */
    @Data
    public static class PageTitles {
        // ===== tabbar 页 =====
        /** 首页 */
        private String home;
        /** 图库页 */
        private String gallery;
        /** 分类页 */
        private String category;
        /** 瞬间页 */
        private String moments;
        /** 博主页 */
        private String blogger;

        // ===== 博客页 =====
        /** 文章列表页 */
        private String articles;
        /** 文章归档页 */
        private String archives;
        /** 文章详情页导航栏默认标题（滚动后仍显示文章题目） */
        private String postDetail;
        /** 分类文章列表页 */
        private String categoryArticles;
        /** 标签列表页 */
        private String tags;
        /** 标签文章列表页 */
        private String tagArticles;
        /** 搜索页 */
        private String search;
        /** 我的收藏页 */
        private String favorites;
        /** 友情链接页 */
        private String friendLinks;
        /** 公告中心页 */
        private String notice;
        /** 公告详情页 */
        private String noticeDetail;
        /** 投票中心页 */
        private String votes;
        /** 投票详情页 */
        private String voteDetail;
        /** 联系博主页 */
        private String contact;
        /** 偏好设置页 */
        private String setting;
        /** 关于项目页 */
        private String aboutProject;
        /** 免责声明页 */
        private String disclaimer;
        /** 数据看板页 */
        private String dataVisual;

        // ===== 认证页 =====
        /** 登录页 */
        private String login;
        /** 注册页 */
        private String register;
        /** 用户协议页 */
        private String userAgreement;
        /** 隐私政策页 */
        private String privacyPolicy;
    }

    /** 首页（快捷导航逐项可配置，轮播渲染参数由 app 端默认开启） */
    @Data
    public static class HomePage {
        /** 是否显示快捷导航 */
        private Boolean useQuickNavigation;
        /** 快捷导航项列表（每项可配置名称/排序/显示隐藏，排序=数组顺序） */
        private List<QuickNavigationItem> quickNavigation;
        /** 是否显示分类（精品文章分类） */
        private Boolean useCategory;
        /** 首页分类栏展示的分类引用（固定 3 个，数据在「分类管理」维护） */
        private List<CategoryItem> categories;
    }

    /** 快捷导航项（字段与客户端 uh-home-quick-nav 对齐） */
    @Data
    public static class QuickNavigationItem {
        private String key;
        private String title;
        /** 副标题（对标 app 端 rightText，如「全部文章」，可空） */
        private String subTitle;
        /** 图标颜色（十六进制色值，如 #03A9F4） */
        private String color;
        /** 背景色（rgba 半透明值） */
        private String bgColor;
        /** 图标字体前缀（如 uhemoji2-icon） */
        private String iconPrefix;
        /** 图标名（如 -mask） */
        private String icon;
        /** 跳转路径（小程序页面路径） */
        private String path;
        /** 是否显示 */
        private Boolean visible;
    }

    /** 博主页（资料卡视觉与常用功能布局） */
    @Data
    public static class BloggerPage {
        /** 资料卡背景图 */
        private String bgImageUrl;
        /** 资料卡波浪图 */
        private String waveImageUrl;
        /** 常用功能显示方式（grid=宫格 / list=列表，app 端博主页常用功能布局；缺省网格） */
        private String commonFeaturesMode;
    }

    /** 我的页面功能入口（常用功能/其他功能两组，条目复用快捷导航项结构，
     * app 端博主页按组渲染） */
    @Data
    public static class MinePage {
        /** 常用功能 */
        private List<QuickNavigationItem> commonFeatures;
        /** 其他功能 */
        private List<QuickNavigationItem> otherFeatures;
    }

    /** 首页分类栏选中引用（固定 3 个；name = Category.metadata.name，快照含名称/封面/排序/文章数，
     * 数组顺序 = 展示排序；app 端直接按快照渲染，不发请求） */
    @Data
    public static class CategoryItem {
        private String name;
        /** 分类名称（展示用冗余快照） */
        private String displayName;
        /** 分类封面图（展示用冗余快照，选中时保存） */
        private String cover;
        /** 分类排序权重（Halo Category.spec.priority 冗余快照，越大越靠前） */
        private Integer priority;
        /** 分类文章数（Halo Category.status.postCount 冗余快照，缺失默认 0） */
        private Integer postCount;
    }

    /** 恋爱日记页（页面标题 + 恋爱页背景图，随 featureConfig.love.diaryPage 下发） */
    @Data
    public static class LoveDiaryPage {
        /** 页面标题 */
        private String pageTitle;
        /** 恋爱页背景图（客户端内置回退） */
        private String bgImageUrl;
    }

    /** 资源与兜底：加载占位图片（仅 loading/error 两图，客户端内置回退兜底） */
    @Data
    public static class Assets {
        /** 加载中的图片 */
        private String loadingGifUrl;
        /** 加载失败图片 */
        private String loadingErrUrl;
    }

    /**
     * 站点级展示偏好默认（L0；客户端本地偏好可覆盖）。
     * 随 {@code featureConfig.preferences} 下发，客户端 collectSiteDefaults
     * 按字段名映射到本地 layout.home/articles/archives。
     */
    @Data
    public static class Preferences {
        /** 首页列表布局（客户端 layout.home.listLayout）：single 单列 / double 双列 */
        private String homeListLayout;
        /** 首页卡片样式（客户端 layout.home.cardType）：image_top/image_right/
         * image_bottom/image_left */
        private String homeCardType;
        /** 文章列表页列表布局（客户端 layout.articles.listLayout）：single / double */
        private String articlesListLayout;
        /** 文章列表页卡片样式（客户端 layout.articles.cardType） */
        private String articlesCardType;
        /** 文章归档页列表布局（客户端 layout.archives.listLayout）：single / double */
        private String archivesListLayout;
        /** 文章归档页卡片样式（客户端 layout.archives.cardType） */
        private String archivesCardType;
        /** 分类笔记页列表布局（客户端 layout.categoryArticles.listLayout）：single / double */
        private String categoryArticlesListLayout;
        /** 分类笔记页卡片样式（客户端 layout.categoryArticles.cardType） */
        private String categoryArticlesCardType;
        /** 标签笔记页列表布局（客户端 layout.tagArticles.listLayout）：single / double */
        private String tagArticlesListLayout;
        /** 标签笔记页卡片样式（客户端 layout.tagArticles.cardType） */
        private String tagArticlesCardType;
        /** 头像外观（客户端 preferences.avatarShape）：square 方形（默认）/ circle 圆形；
         * 应用于文章卡片（上文下图）与瞬间卡片的用户头像 */
        private String avatarShape;
        /** 友情链接页展示偏好（小程序打开模式等） */
        private LinkPage linkPage;
    }

    /** 友情链接页展示偏好 */
    @Data
    public static class LinkPage {
        /** 小程序打开模式：fullscreen 全屏（默认，navigateToMiniProgram）/ halfScreen 半屏（openEmbeddedMiniProgram） */
        private String miniProgramOpenMode;
    }

    /** 恋爱模块（恋爱日记入口密码在「页面入口」tab 配置、无 enabled 开关
     * （入口显隐由页面设置-快捷导航/关于页功能入口注册表控制）；
     * 三模块入口（故事/相册/清单）在「模块入口」tab 配置，自身即 app 端
     * 入口列表数据（title/subTitle/颜色/图标背景色/path/priority）） */
    @Data
    public static class Love {
        /** 恋爱日记入口密码（恋爱页本身，即 app 端 love 页；
         * 设置密码后进入恋爱页前需先验证密码；无 enabled 开关） */
        private ModuleSwitch loveDiary;
        /** 恋爱故事模块入口（数据在「恋爱管理-恋爱故事」维护） */
        private ModuleSwitch ourStory;
        /** 恋爱相册模块入口（数据在「恋爱管理-恋爱相册」维护） */
        private ModuleSwitch lovePhoto;
        /** 恋爱清单模块入口（数据在「恋爱管理-恋爱清单」维护） */
        private ModuleSwitch loveDaily;
        /** 恋爱日记页面设置（页面标题 + 恋爱页背景图，配置在功能设置-恋爱设置-页面设置
         * tab；随 {@code featureConfig.love.diaryPage} 下发） */
        private LoveDiaryPage diaryPage;
    }

    /**
     * 恋爱模块入口（三模块共用；loveDiary 仅使用密码相关字段）：
     * enabled 是否在恋爱页展示入口；passwordEnabled/password/passwordHash/
     * passwordRemoved 为入口密码，与恋爱相册密码同一套 BCrypt 语义：管理端设置后，
     * 小程序端访问对应模块数据前需先经 {@code POST /love-modules/unlock}
     * 验证密码换取 HMAC token。
     * 三模块（ourStory/lovePhoto/loveDaily）另承载 app 端入口列表数据：
     * title/subTitle/titleColor/subTitleColor/iconBgColor/path/priority
     * （app 端直接按模块 key 渲染入口，无需本地硬编码）。
     */
    @Data
    public static class ModuleSwitch {
        /** 是否在恋爱页展示该模块入口（loveDiary 不使用：入口显隐由快捷导航/功能入口注册表控制） */
        private Boolean enabled;
        /** 入口名称（app 端入口列表标题） */
        private String title;
        /** 入口副标题（app 端入口列表副标题） */
        private String subTitle;
        /** 标题颜色（hex8 #rrggbbaa） */
        private String titleColor;
        /** 副标题颜色（hex8 #rrggbbaa） */
        private String subTitleColor;
        /** 图标背景色（hex8 #rrggbbaa） */
        private String iconBgColor;
        /** 图标字体前缀（如 uhlove-icon，app 端渲染入口图标） */
        private String iconPrefix;
        /** 图标名（app 端渲染入口图标） */
        private String icon;
        /** app 端跳转路径 */
        private String path;
        /** 排序字段（越大越靠前，与 CategoryItem/Banner.priority 同语义） */
        private Integer priority;
        /** 是否已设置密码（视图字段：控制台 GET 返回，由后端按 passwordHash 计算，保存时忽略） */
        private Boolean passwordEnabled;
        /** 新密码（仅管理端写请求携带；非空 = 重设并启用；GET 一律置空不回显） */
        private String password;
        /** 密码 BCrypt 哈希（仅内部存储；控制台 GET 与公开输出一律脱敏置空） */
        private String passwordHash;
        /** 是否清除密码（仅管理端写请求携带；true = 清除该入口密码并关闭验证） */
        private Boolean passwordRemoved;
    }

    /**
     * 友链信息：
 *   - {@link MiniInfo} 小程序信息：小程序端「申请信息」弹窗（uh-links-mini-info）展示；
 *   - {@link SiteInfo} 站点信息：本站站点名片，字段对齐 Halo 官方友链提交 API
     *       （plugin-links {@code link-applications} 请求体：displayName/url/logo/description/backlink/feedUrls）。
     * 随 {@code featureConfig.linkInfo} 下发客户端。
     */
    @Data
    public static class LinkInfo {
        /** 是否开放公开提交申请（默认 true，关闭后公开提交接口返回「暂未开放提交申请」） */
        private Boolean submissionEnabled;
        /** 小程序信息（小程序名称/太阳码/跳转地址/描述/申请说明） */
        private MiniInfo miniInfo;
        /** 站点信息（本站站点名片，字段对齐 Halo 官方友链提交 API） */
        private SiteInfo siteInfo;
    }

    /** 小程序信息（app 端「申请信息」弹窗展示项） */
    @Data
    public static class MiniInfo {
        /** 小程序名称 */
        private String displayName;
        /** 太阳码/小程序码图片 */
        private String miniProgramCode;
        /** 小程序 AppID（wx 开头，必填） */
        private String appId;
        /** 跳转页面路径（非必填） */
        private String path;
        /** 跳转地址 */
        private String link;
        /** 小程序描述 */
        private String description;
        /** 申请说明 */
        private String applyRemark;
    }

    /** 站点信息（字段对齐 Halo 官方 plugin-links 友链提交 API：link-applications 请求体） */
    @Data
    public static class SiteInfo {
        /** 网站名称（官方 displayName） */
        private String displayName;
        /** 网站地址（官方 url，HTTP/HTTPS） */
        private String url;
        /** 网站 Logo 地址（官方 logo） */
        private String logo;
        /** 网站描述（官方 description） */
        private String description;
        /** 反链页面地址（官方 backlink） */
        private String backlink;
        /** RSS/Atom 订阅地址（官方 feedUrls；配置表单换行分隔，存储为数组） */
        private List<String> feedUrls;
    }

    /** 维护模式。维护页展示内容与排期窗口；实际状态（未维护/预告/维护中）
     * 由服务端按 {@code enabled + startTime/endTime} 与当前时间计算（见
     * {@code utils/MaintenanceResolver}），「到点自动结束」为输出端判定、配置不回写。 */
    @Data
    public static class Maintenance {
        /** 安排开关：开启后按时间窗口即时生效（startTime 为空/已过 = 立即进入维护中） */
        private Boolean enabled;
        /** 维护页标题（默认「站点维护中」，scheduled/active 共用，客户端自行组织文案） */
        private String title;
        /** 维护说明（纯文本，textarea 编辑；小程序端维护页标题下方直接展示，留空则展示默认文案） */
        private String notice;
        /** 维护详情（富文本 HTML，RichTextEditorField 编辑；小程序端「维护详情」弹窗展示，
         * 留空则不展示详情入口） */
        private String description;
        /** 维护开始时间（RFC3339 UTC 字符串，如 2026-09-05T02:00:00Z）；空 = 立即维护 */
        private String startTime;
        /** 预计恢复时间（RFC3339 UTC 字符串）；空 = 持续至手动关闭；到点自动结束 */
        private String endTime;
    }
}
