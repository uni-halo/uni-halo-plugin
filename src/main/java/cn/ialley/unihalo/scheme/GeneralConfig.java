package cn.ialley.unihalo.scheme;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import static cn.ialley.unihalo.constants.Constants.BASIC_DOMAIN_NAME;
import static cn.ialley.unihalo.constants.Constants.PLUGIN_API_VERSION;

/**
 * 通用配置（单例，metadata.name 固定为 general-config）。
 *
 * <p>承载小程序通用内容与外观配置，分为以下区块：</p>
 * <ul>
 *   <li>{@link Profile} profile：应用资料——应用信息（名称/图标）、博主信息、
 *       社交信息、页脚版权、免责声明、关于项目与文章详情版权文案；</li>
 *   <li>{@link Pages} pages：首页（含轮播图渲染参数）、图库、关于页的视觉配置；</li>
 *   <li>{@link Assets} assets：全局默认图片/封面/头像/加载占位等兜底资源；</li>
 *   <li>{@link Preferences} preferences：站点级展示偏好默认（L0，经 getConfigs 顶层
 *       {@code preferences} 下发，与客户端 layout.home/cardType/isAvatarRadius 对齐）；</li>
 *   <li>{@link Love} love：恋爱模块——总开关、恋爱页图片与恋爱故事/相册/清单模块入口开关；</li>
 *   <li>{@link Maintenance} maintenance：维护模式——维护页标题/富文本说明
 *       与排期窗口，状态由服务端按时间窗口计算。</li>
 * </ul>
 *
 * <p>本模型是控制台「通用配置」页的写端事实源；小程序端仍通过 {@code getConfigs}
 * 读取（由服务端输出合成层把本模型映射回旧 shape，见 UniHaloServiceImpl）。</p>
 *
 * @author 小莫唐尼
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = BASIC_DOMAIN_NAME, version = PLUGIN_API_VERSION,
        kind = "GeneralConfig", plural = "generalConfigs", singular = "generalConfig")
public class GeneralConfig extends AbstractExtension {

    private Spec spec;

    @Data
    public static class Spec {
        private Profile profile;
        private Pages pages;
        private Assets assets;
        private Preferences preferences;
        private Love love;
        /** 友链信息：站长小程序展示信息，经 getConfigs 覆盖
         * pluginConfig.linksSubmitPlugin 对应键下发，供小程序端「申请信息」弹窗展示 */
        private LinkInfo linkInfo;
        /** 维护模式 */
        private Maintenance maintenance;
        /** 审核模式：开启后关闭小程序部分数据展示，小程序提交审核时建议开启；
         * 公开 getConfigs 由装配器重建回 {@code auditConfig.auditModeEnabled} 形态下发 */
        private AuditMode auditMode;
    }

    /** 审核模式开关（默认关闭） */
    @Data
    public static class AuditMode {
        /** 是否开启审核模式 */
        private Boolean enabled;
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

    /** 博主信息（原 authorConfig.blogger） */
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

    /** 免责声明（不再需要启用开关，仅维护内容） */
    @Data
    public static class Disclaimer {
        private String content;
    }

    /** 文章详情内容与版权文案 */
    @Data
    public static class PostDetail {
        /** 是否显示评论相关（优先级高于系统评论开关） */
        private Boolean showComment;
        /** 是否使用文章版权 */
        private Boolean copyrightEnabled;
        private String copyrightAuthor;
        private String copyrightDesc;
        private String copyrightViolation;
    }

    /** 页面与排版：首页/图库/分类页/瞬间页/关于页/文章详情页/免责声明页视觉，
     * 以及其余功能页面标题（恋爱日记/联系博主/我的收藏/友情链接/文章归档/投票中心/
     * 数据看板/偏好设置/关于项目/公告中心/搜索页面，均经 getConfigs 下发
     * pageConfig.xxxConfig，页面标题留空时客户端回退内置标题） */
    @Data
    public static class Pages {
        private Home homeConfig;
        private Gallery galleryConfig;
        private About aboutConfig;
        /** 分类页（分类页标题，客户端 pageConfig.categoryConfig） */
        private CategoryPage categoryConfig;
        /** 瞬间页（瞬间页标题，客户端 pageConfig.momentConfig） */
        private MomentPage momentConfig;
        /** 我的页面功能入口（常用功能/其他功能两组，配置并入「关于页」tab，
         * 经 getConfigs 下发 pageConfig.myPageConfig） */
        private MyPage myPageConfig;
        /** 免责声明页（不再需要启用开关，仅内容） */
        private Disclaimer disclaimers;
        /** 文章详情页内容与版权文案 */
        private PostDetail postDetailConfig;
        /** 恋爱日记页（页面标题，客户端 pageConfig.loveDiaryConfig） */
        private LoveDiaryPage loveDiaryConfig;
        /** 联系博主页（页面标题，客户端 pageConfig.contactConfig） */
        private ContactPage contactConfig;
        /** 我的收藏页（页面标题，客户端 pageConfig.favoritesConfig） */
        private FavoritesPage favoritesConfig;
        /** 友情链接页（页面标题，客户端 pageConfig.friendLinksConfig） */
        private FriendLinksPage friendLinksConfig;
        /** 文章归档页（页面标题，客户端 pageConfig.archivesConfig） */
        private ArchivesPage archivesConfig;
        /** 投票中心页（页面标题，客户端 pageConfig.voteConfig） */
        private VotePage voteConfig;
        /** 数据看板页（页面标题，客户端 pageConfig.dataVisualConfig） */
        private DataVisualPage dataVisualConfig;
        /** 偏好设置页（页面标题，客户端 pageConfig.settingConfig） */
        private SettingPage settingConfig;
        /** 关于项目页（页面标题，客户端 pageConfig.aboutProjectConfig） */
        private AboutProjectPage aboutProjectConfig;
        /** 公告中心页（页面标题，客户端 pageConfig.noticeConfig） */
        private NoticePage noticeConfig;
        /** 搜索页面（页面标题，客户端 pageConfig.searchConfig） */
        private SearchPage searchConfig;
    }

    /** 首页（轮播渲染参数由 app 端默认开启、快捷导航逐项可配置） */
    @Data
    public static class Home {
        /** 首页标题（客户端默认「首页」） */
        private String pageTitle;
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

    /** 我的页面功能入口（常用功能/其他功能两组，条目复用快捷导航项结构，
     * app 端 about 页按组渲染） */
    @Data
    public static class MyPage {
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

    /** 图库页（瀑布流由 app 端默认） */
    @Data
    public static class Gallery {
        private String pageTitle;
    }

    /** 分类页（客户端 pageConfig.categoryConfig） */
    @Data
    public static class CategoryPage {
        /** 分类页标题 */
        private String pageTitle;
    }

    /** 瞬间页（客户端 pageConfig.momentConfig） */
    @Data
    public static class MomentPage {
        /** 瞬间页标题 */
        private String pageTitle;
    }

    /** 关于页 */
    @Data
    public static class About {
        private String pageTitle;
        /** 资料卡背景图 */
        private String bgImageUrl;
        /** 资料卡波浪图 */
        private String waveImageUrl;
    }

    /** 恋爱日记页（客户端 pageConfig.loveDiaryConfig） */
    @Data
    public static class LoveDiaryPage {
        /** 页面标题 */
        private String pageTitle;
        /** 恋爱页背景图（客户端内置回退） */
        private String bgImageUrl;
    }

    /** 联系博主页（客户端 pageConfig.contactConfig） */
    @Data
    public static class ContactPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 我的收藏页（客户端 pageConfig.favoritesConfig） */
    @Data
    public static class FavoritesPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 友情链接页（客户端 pageConfig.friendLinksConfig） */
    @Data
    public static class FriendLinksPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 文章归档页（客户端 pageConfig.archivesConfig） */
    @Data
    public static class ArchivesPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 投票中心页（客户端 pageConfig.voteConfig） */
    @Data
    public static class VotePage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 数据看板页（客户端 pageConfig.dataVisualConfig） */
    @Data
    public static class DataVisualPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 偏好设置页（客户端 pageConfig.settingConfig） */
    @Data
    public static class SettingPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 关于项目页（客户端 pageConfig.aboutProjectConfig） */
    @Data
    public static class AboutProjectPage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 公告中心页（客户端 pageConfig.noticeConfig） */
    @Data
    public static class NoticePage {
        /** 页面标题 */
        private String pageTitle;
    }

    /** 搜索页面（客户端 pageConfig.searchConfig） */
    @Data
    public static class SearchPage {
        /** 页面标题 */
        private String pageTitle;
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
     * getConfigs 顶层 {@code preferences} 原样下发本结构，客户端 collectSiteDefaults
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
        private String articleCardType;
        /** 文章归档页列表布局（客户端 layout.archives.listLayout）：single / double */
        private String archivesListLayout;
        /** 文章归档页卡片样式（客户端 layout.archives.cardType） */
        private String archivesCardType;
        /** 评论头像是否圆角（客户端 isAvatarRadius） */
        private Boolean avatarRadius;
    }

    /** 恋爱模块（getConfigs 输出由装配器映射回旧顶层 loveConfig shape；
     * 入口展示由模块入口开关与 navList 统一管理） */
    @Data
    public static class Love {
        /** 恋爱日记入口开关（恋爱页本身，即 app 端 love 页；
         * 设置密码后进入恋爱页前需先验证密码） */
        private ModuleSwitch loveDiary;
        /** 恋爱故事模块入口开关（数据在「恋爱管理-恋爱故事」维护） */
        private ModuleSwitch ourStory;
        /** 恋爱相册模块入口开关（数据在「恋爱管理-恋爱相册」维护） */
        private ModuleSwitch lovePhoto;
        /** 恋爱清单模块入口开关（数据在「恋爱管理-恋爱清单」维护） */
        private ModuleSwitch loveDaily;
        /** 恋爱信息（纪念日 + 恋人信息，配置在通用配置-恋爱设置-恋爱信息 tab） */
        private LoveInfo loveInfo;
        /** 恋爱页入口列表（固定 3 项，key 对应模块；
         * 仅 title/subTitle 可编辑 + priority 排序 + visible 开关，不可增删；
         * 经 getConfigs 下发 loveConfig.navList） */
        private List<LoveNavItem> navList;
    }

    /** 恋爱信息（纪念日 + 恋人信息） */
    @Data
    public static class LoveInfo {
        /** 纪念日标题，默认「这是我们一起走过的」 */
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

    /** 恋爱页入口项（固定 3 项，key 对应 ourStory/lovePhoto/loveDaily 模块，
     * app 端按 key 映射 uhlove-icon 图标与跳转路径） */
    @Data
    public static class LoveNavItem {
        /** 固定：stories/album/list（对应 ourStory/lovePhoto/loveDaily 模块） */
        private String key;
        /** 可编辑名称 */
        private String title;
        /** 可编辑副标题 */
        private String subTitle;
        /** 排序字段（越大越靠前，与 CategoryItem/Banner.priority 同语义） */
        private Integer priority;
        /** 是否展示（不可删除，仅禁用/启用开关；与模块开关 enabled 均 true 才展示） */
        private Boolean visible;
    }

    /**
     * 恋爱模块入口开关（enabled 是否在恋爱页展示入口；passwordEnabled/password/
     * passwordHash/passwordRemoved 为入口密码，与恋爱相册密码同一套 BCrypt 语义：
     * 管理端设置后，小程序端访问对应模块数据前需先经 {@code POST /love-modules/unlock}
     * 验证密码换取 HMAC token）。
     */
    @Data
    public static class ModuleSwitch {
        /** 是否在恋爱页展示该模块入口 */
        private Boolean enabled;
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
     * <ul>
     *   <li>{@link MiniInfo} 小程序信息：小程序端「申请信息」弹窗（uh-links-mini-info）展示；</li>
     *   <li>{@link SiteInfo} 站点信息：本站站点名片，字段对齐 Halo 官方友链提交 API
     *       （plugin-links {@code link-applications} 请求体：displayName/url/logo/description/backlink/feedUrls）。</li>
     * </ul>
     * getConfigs 输出经装配器直接下发 {@code pluginConfig.linkInfo}。
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
