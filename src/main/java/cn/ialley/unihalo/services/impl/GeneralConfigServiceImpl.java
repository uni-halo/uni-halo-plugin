package cn.ialley.unihalo.services.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.GeneralConfig;
import cn.ialley.unihalo.scheme.GeneralConfig.About;
import cn.ialley.unihalo.scheme.GeneralConfig.AboutProjectPage;
import cn.ialley.unihalo.scheme.GeneralConfig.ArchivesPage;
import cn.ialley.unihalo.scheme.GeneralConfig.Assets;
import cn.ialley.unihalo.scheme.GeneralConfig.AuditMode;
import cn.ialley.unihalo.scheme.GeneralConfig.Blogger;
import cn.ialley.unihalo.scheme.GeneralConfig.CategoryPage;
import cn.ialley.unihalo.scheme.GeneralConfig.ContactPage;
import cn.ialley.unihalo.scheme.GeneralConfig.Copyright;
import cn.ialley.unihalo.scheme.GeneralConfig.DataVisualPage;
import cn.ialley.unihalo.scheme.GeneralConfig.Disclaimer;
import cn.ialley.unihalo.scheme.GeneralConfig.FavoritesPage;
import cn.ialley.unihalo.scheme.GeneralConfig.FriendLinksPage;
import cn.ialley.unihalo.scheme.GeneralConfig.Gallery;
import cn.ialley.unihalo.scheme.GeneralConfig.Home;
import cn.ialley.unihalo.scheme.GeneralConfig.LinkInfo;
import cn.ialley.unihalo.scheme.GeneralConfig.Love;
import cn.ialley.unihalo.scheme.GeneralConfig.LoveDiaryPage;
import cn.ialley.unihalo.scheme.GeneralConfig.LoveInfo;
import cn.ialley.unihalo.scheme.GeneralConfig.Maintenance;
import cn.ialley.unihalo.scheme.GeneralConfig.MomentPage;
import cn.ialley.unihalo.scheme.GeneralConfig.ModuleSwitch;
import cn.ialley.unihalo.scheme.GeneralConfig.MyPage;
import cn.ialley.unihalo.scheme.GeneralConfig.NoticePage;
import cn.ialley.unihalo.scheme.GeneralConfig.Pages;
import cn.ialley.unihalo.scheme.GeneralConfig.PostDetail;
import cn.ialley.unihalo.scheme.GeneralConfig.Profile;
import cn.ialley.unihalo.scheme.GeneralConfig.QuickNavigationItem;
import cn.ialley.unihalo.scheme.GeneralConfig.SearchPage;
import cn.ialley.unihalo.scheme.GeneralConfig.SettingPage;
import cn.ialley.unihalo.scheme.GeneralConfig.Social;
import cn.ialley.unihalo.scheme.GeneralConfig.SocialItem;
import cn.ialley.unihalo.scheme.GeneralConfig.Spec;
import cn.ialley.unihalo.scheme.GeneralConfig.VotePage;
import cn.ialley.unihalo.services.GeneralConfigService;
import cn.ialley.unihalo.utils.MaintenanceResolver;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 通用配置服务实现（单例）。
 *
 * <p>默认值对齐旧 setting.yaml 的 value 缺省；历史 ConfigMap 旧组
 * （basicConfig/pageConfig/authorConfig/imagesConfig）在单例尚未创建时合并进
 * 默认结构，作为升级期的一次性导入兜底。</p>
 *
 * @author 小莫唐尼
 */
@Service
@RequiredArgsConstructor
public class GeneralConfigServiceImpl implements GeneralConfigService {

    /**
     * 参与历史导入的旧设置组
     */
    private static final String[] LEGACY_GROUPS =
        {"basicConfig", "pageConfig", "authorConfig", "imagesConfig"};

    /**
     * 插件 Spring 上下文未注册 Jackson 3 ObjectMapper bean，故内部自行创建
     * （与 EmailService 同套路）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 恋爱模块入口密码编码器（与恋爱相册 {@code LoveAlbumServiceImpl} 同套路；
     * 声明处初始化，不进 Lombok 构造器）。
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final ReactiveExtensionClient client;
    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<GeneralConfig> get() {
        return client.fetch(GeneralConfig.class, Constants.GENERAL_CONFIG_SINGLETON_NAME)
                .switchIfEmpty(Mono.defer(this::defaultWithLegacy))
                // 恋爱模块入口密码一律脱敏（哈希不回显；passwordEnabled 由哈希派生）
                .map(this::maskLovePasswords);
    }

    @Override
    public Mono<GeneralConfig> save(GeneralConfig config) {
        GeneralConfig body = config == null ? new GeneralConfig() : config;
        // 写入前合并：默认值 → 历史 ConfigMap 旧值 → 请求体非空字段，防丢字段/防空写。
        // 先取现有单例（可能不存在）以保留恋爱模块入口密码哈希。
        return client.fetch(GeneralConfig.class, Constants.GENERAL_CONFIG_SINGLETON_NAME)
                .defaultIfEmpty(new GeneralConfig())
                .flatMap(existing -> legacyOverlay().map(overlay -> {
                    ObjectNode merged = merge(defaultSpecTree(), overlay);
                    if (body.getSpec() != null) {
                        merged = merge(merged,
                                (ObjectNode) objectMapper.valueToTree(body.getSpec()));
                    }
                    GeneralConfig target = new GeneralConfig();
                    target.setSpec(treeToValue(merged));
                    // 恋爱模块入口密码：新密码=重设并启用；passwordRemoved=清除；否则保持原哈希
                    applyLovePasswords(target.getSpec().getLove(),
                            existing.getSpec() != null ? existing.getSpec().getLove() : null);
                    validateMaintenance(target.getSpec().getMaintenance());
                    return target;
                }).flatMap(target -> {
                    if (existing.getMetadata() == null) {
                        Metadata metadata = new Metadata();
                        metadata.setName(Constants.GENERAL_CONFIG_SINGLETON_NAME);
                        metadata.setCreationTimestamp(Instant.now());
                        target.setMetadata(metadata);
                        return client.create(target);
                    }
                    existing.setSpec(target.getSpec());
                    return client.update(existing);
                }))
                // 响应同样脱敏（哈希/写请求字段不回显）
                .map(this::maskLovePasswords);
    }

    @Override
    public Mono<Boolean> verifyLoveModulePassword(String module, String password) {
        return fetchRaw().map(config -> {
            ModuleSwitch moduleSwitch = findLoveModule(config, module);
            if (moduleSwitch == null || isBlank(moduleSwitch.getPasswordHash())) {
                return false;
            }
            return passwordEncoder.matches(
                    password == null ? "" : password, moduleSwitch.getPasswordHash());
        }).defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> isLoveModuleLocked(String module) {
        return fetchRaw().map(config -> {
            ModuleSwitch moduleSwitch = findLoveModule(config, module);
            return moduleSwitch != null && !isBlank(moduleSwitch.getPasswordHash());
        }).defaultIfEmpty(false);
    }

    /**
     * 读取原始单例（未经脱敏，含恋爱模块密码哈希），供解锁/锁定判断使用。
     */
    private Mono<GeneralConfig> fetchRaw() {
        return client.fetch(GeneralConfig.class, Constants.GENERAL_CONFIG_SINGLETON_NAME)
                .switchIfEmpty(Mono.defer(this::defaultWithLegacy));
    }

    private static ModuleSwitch findLoveModule(GeneralConfig config, String module) {
        if (config == null || config.getSpec() == null || config.getSpec().getLove() == null) {
            return null;
        }
        Love love = config.getSpec().getLove();
        return switch (module) {
            case "loveDiary" -> love.getLoveDiary();
            case "ourStory" -> love.getOurStory();
            case "lovePhoto" -> love.getLovePhoto();
            case "loveDaily" -> love.getLoveDaily();
            default -> null;
        };
    }

    // ---------- 默认结构与历史导入 ----------

    /**
     * 默认结构（不落库）：默认值 + 历史 ConfigMap 旧值合并。
     */
    private Mono<GeneralConfig> defaultWithLegacy() {
        return legacyOverlay().map(overlay -> {
            GeneralConfig config = new GeneralConfig();
            Metadata metadata = new Metadata();
            metadata.setName(Constants.GENERAL_CONFIG_SINGLETON_NAME);
            config.setMetadata(metadata);
            config.setSpec(treeToValue(merge(defaultSpecTree(), overlay)));
            return config;
        });
    }

    /**
     * 读取历史旧设置组并整理成 spec 形态的覆盖树
     * （{@code profile: {...}} / {@code pages: {...}} / {@code assets: {...}}）。
     * 旧组不存在时返回空对象。
     */
    private Mono<ObjectNode> legacyOverlay() {
        return settingFetcher.getSettingValues()
                .defaultIfEmpty(Map.of())
                .map(values -> {
                    ObjectNode overlay = JsonNodeFactory.instance.objectNode();
                    JsonNode author = values.get("authorConfig");
                    JsonNode basic = values.get("basicConfig");
                    ObjectNode profile = JsonNodeFactory.instance.objectNode();
                    pick(author, profile, "blogger");
                    // 社交信息（动态列表）：旧 authorConfig.social 固定字段
                    // （enabled/qq/wechat/...）迁移为 items 列表（key=字段名、content=值）
                    JsonNode oldSocial = author != null ? author.get("social") : null;
                    if (oldSocial != null && oldSocial.isObject()) {
                        JsonNode items = migrateLegacySocialItems(oldSocial);
                        if (items != null && items.size() > 0) {
                            ObjectNode socialOut = JsonNodeFactory.instance.objectNode();
                            socialOut.set("items", items);
                            profile.set("social", socialOut);
                        }
                    }
                    // 页脚版权（旧 basicConfig.copyrightConfig → profile.copyrightConfig）
                    JsonNode basicCopyright = basic != null ? basic.get("copyrightConfig") : null;
                    if (basicCopyright != null && !basicCopyright.isNull()) {
                        profile.set("copyrightConfig", basicCopyright);
                    }
                    // showAboutSystem/disclaimers/postDetailConfig 不再属于 profile：
                    // 免责/文章详情位于页面设置，showAboutSystem 由开关入口统一管理，
                    // 此处仅保留博主/社交历史值
                    // 应用信息（名称/图标）：优先取「基本配置」baseConfig.appInfo，
                    // 回退旧 appConfig.appInfo
                    JsonNode appInfo = null;
                    JsonNode baseCfg = values.get("baseConfig");
                    if (baseCfg != null && baseCfg.isObject() && baseCfg.has("appInfo")) {
                        appInfo = baseCfg.get("appInfo");
                    } else {
                        JsonNode appCfg = values.get("appConfig");
                        if (appCfg != null && appCfg.isObject() && appCfg.has("appInfo")) {
                            appInfo = appCfg.get("appInfo");
                        }
                    }
                    if (appInfo != null && !appInfo.isNull()) {
                        profile.set("appInfo", appInfo);
                    }
                    if (profile.size() > 0) {
                        overlay.set("profile", profile);
                    }
                    JsonNode page = values.get("pageConfig");
                    ObjectNode pages = JsonNodeFactory.instance.objectNode();
                    pick(page, pages, "homeConfig", "galleryConfig");
                    // 关于页：旧 pageConfig.aboutConfig（标题/背景/波浪）；
                    // 页脚版权由应用资料 profile.copyrightConfig 承担（见上），
                    // 此处不再合并旧 basicConfig.copyrightConfig
                    ObjectNode aboutOut = JsonNodeFactory.instance.objectNode();
                    JsonNode aboutOld = page != null ? page.get("aboutConfig") : null;
                    pick(aboutOld, aboutOut, "pageTitle", "bgImageUrl", "waveImageUrl");
                    if (aboutOut.size() > 0) {
                        pages.set("aboutConfig", aboutOut);
                    }
                    // 免责声明/文章详情（basicConfig → 页面设置）
                    pick(basic, pages, "disclaimers", "postDetailConfig");
                    if (pages.size() > 0) {
                        overlay.set("pages", pages);
                    }
                    JsonNode images = values.get("imagesConfig");
                    if (images != null && images.isObject() && images.size() > 0) {
                        overlay.set("assets", images);
                    }
                    // 恋爱模块：旧 loveConfig 字段（模块开关）从 ConfigMap 导入
                    // spec.love，兼容 featureConfig.loveConfig 与旧顶层键 loveConfig
                    // 两种旧结构；仅显式挑选仍有效的字段（模块 enabled），
                    // 旧 iconUrl/waveImageUrl/heartImageUrl/loveEnabled/pageImages
                    // 等字段不再导入（背景图由 pages.loveDiaryConfig.bgImageUrl 承担）。
                    JsonNode love = values.get("loveConfig");
                    if (love == null || !love.isObject()) {
                        JsonNode feature = values.get("featureConfig");
                        if (feature != null && feature.isObject()) {
                            love = feature.get("loveConfig");
                        }
                    }
                    if (love != null && love.isObject() && love.size() > 0) {
                        ObjectNode loveOut = JsonNodeFactory.instance.objectNode();
                        // 入口展示由模块入口开关与 navList 统一管理，loveEnabled 不导入
                        for (String moduleKey : new String[]{"ourStory", "lovePhoto", "loveDaily"}) {
                            JsonNode module = love.get(moduleKey);
                            if (module != null && module.isObject()) {
                                ObjectNode moduleOut = JsonNodeFactory.instance.objectNode();
                                pick(module, moduleOut, "enabled");
                                if (moduleOut.size() > 0) {
                                    loveOut.set(moduleKey, moduleOut);
                                }
                            }
                        }
                        if (loveOut.size() > 0) {
                            overlay.set("love", loveOut);
                        }
                    }
                    // 友链信息基本配置：旧 featureConfig.linkConfig.submissionEnabled
                    // （是否开放公开提交申请）导入 spec.linkInfo.submissionEnabled，
                    // 曾关闭提交的老配置应保持关闭。
                    JsonNode linkConfig = values.get("linkConfig");
                    if (linkConfig == null || !linkConfig.isObject()) {
                        JsonNode feature = values.get("featureConfig");
                        if (feature != null && feature.isObject()) {
                            linkConfig = feature.get("linkConfig");
                        }
                    }
                    if (linkConfig != null && linkConfig.isObject()) {
                        JsonNode submission = linkConfig.get("submissionEnabled");
                        if (submission != null && submission.isBoolean()) {
                            ObjectNode linkInfoOut = JsonNodeFactory.instance.objectNode();
                            linkInfoOut.set("submissionEnabled", submission);
                            overlay.set("linkInfo", linkInfoOut);
                        }
                    }
                    // 审核模式开关：旧 auditConfig.auditModeEnabled 导入
                    // spec.auditMode.enabled，曾开启的审核模式不回退默认关闭。
                    JsonNode auditConfig = values.get("auditConfig");
                    if (auditConfig == null || !auditConfig.isObject()) {
                        JsonNode safety = values.get("safetyConfig");
                        if (safety != null && safety.isObject()) {
                            auditConfig = safety.get("auditConfig");
                        }
                    }
                    if (auditConfig != null && auditConfig.isObject()) {
                        JsonNode auditModeEnabled = auditConfig.get("auditModeEnabled");
                        if (auditModeEnabled != null && auditModeEnabled.isBoolean()) {
                            ObjectNode auditModeOut = JsonNodeFactory.instance.objectNode();
                            auditModeOut.set("enabled", auditModeEnabled);
                            overlay.set("auditMode", auditModeOut);
                        }
                    }
                    return overlay;
                });
    }

    private static void pick(JsonNode source, ObjectNode target, String... keys) {
        if (source == null || !source.isObject()) {
            return;
        }
        for (String key : keys) {
            JsonNode value = source.get(key);
            if (value != null && !value.isNull()) {
                target.set(key, value);
            }
        }
    }

    /**
     * 递归合并：overlay 中非空字段覆盖 base；对象类型递归；null 视为未提供。
     */
    private static ObjectNode merge(JsonNode base, JsonNode overlay) {
        if (!base.isObject() || !overlay.isObject()) {
            return (ObjectNode) base.deepCopy();
        }
        ObjectNode merged = (ObjectNode) base.deepCopy();
        overlay.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull()) {
                return;
            }
            JsonNode child = merged.get(entry.getKey());
            if (child != null && child.isObject() && value.isObject()) {
                merged.set(entry.getKey(), merge(child, value));
            } else {
                merged.set(entry.getKey(), value);
            }
        });
        return merged;
    }

    private ObjectNode defaultSpecTree() {
        return (ObjectNode) objectMapper.valueToTree(buildDefaultSpec());
    }

    private Spec treeToValue(ObjectNode tree) {
        return objectMapper.treeToValue(tree, Spec.class);
    }

    /**
     * 默认 spec（对齐旧 setting.yaml 的 value 缺省）。
     */
    private Spec buildDefaultSpec() {
        Spec spec = new Spec();
        spec.setProfile(buildDefaultProfile());
        spec.setPages(buildDefaultPages());
        spec.setAssets(buildDefaultAssets());
        spec.setPreferences(buildDefaultPreferences());
        spec.setLove(buildDefaultLove());
        spec.setLinkInfo(buildDefaultLinkInfo());
        spec.setMaintenance(buildDefaultMaintenance());
        // 审核模式（默认关闭）
        AuditMode auditMode = new AuditMode();
        auditMode.setEnabled(false);
        spec.setAuditMode(auditMode);
        return spec;
    }

    /**
     * 默认友链信息：基本配置（submissionEnabled 默认 true）+ 两个子配置
     * （miniInfo/siteInfo）全部留空；站长配置后经 getConfigs 直接下发
     * {@code pluginConfig.linkInfo}，不再使用 linksSubmitPlugin。
     */
    private static LinkInfo buildDefaultLinkInfo() {
        LinkInfo linkInfo = new LinkInfo();
        linkInfo.setSubmissionEnabled(true);
        linkInfo.setMiniInfo(new GeneralConfig.MiniInfo());
        linkInfo.setSiteInfo(new GeneralConfig.SiteInfo());
        return linkInfo;
    }

    private static GeneralConfig.Preferences buildDefaultPreferences() {
        // 与客户端内置默认对齐：首页/归档 single + image_bottom，
        // 文章列表 double + image_bottom；卡片样式统一组件 layout 值（image_*）
        GeneralConfig.Preferences preferences = new GeneralConfig.Preferences();
        preferences.setHomeListLayout("single");
        preferences.setHomeCardType("image_bottom");
        preferences.setArticlesListLayout("double");
        preferences.setArticleCardType("image_bottom");
        preferences.setArchivesListLayout("single");
        preferences.setArchivesCardType("image_bottom");
        preferences.setAvatarRadius(true);
        return preferences;
    }

    private static Profile buildDefaultProfile() {
        Profile profile = new Profile();

        GeneralConfig.AppInfo appInfo = new GeneralConfig.AppInfo();
        appInfo.setName("uni-halo");
        // 应用图标默认引用插件内置静态资源（ReverseProxy：/plugins/plugin-uni-halo/assets/static/**）
        appInfo.setLogo("/plugins/plugin-uni-halo/assets/static/logo.png");
        profile.setAppInfo(appInfo);

        Blogger blogger = new Blogger();
        blogger.setNickname("uni-halo");
        blogger.setAvatar("");
        blogger.setEmail("");
        blogger.setDescription("");
        // 主页
        blogger.setWebsite("");
        profile.setBlogger(blogger);

        Social social = new Social();
        social.setItems(defaultSocialItems());
        profile.setSocial(social);

        // 页脚版权（显示于【关于】页面页脚）
        Copyright copyright = new Copyright();
        copyright.setEnabled(true);
        copyright.setContent("「 2022 uni-halo 丨 开源项目@小莫唐尼 」");
        profile.setCopyrightConfig(copyright);
        return profile;
    }

    /**
     * 默认社交项（社交信息为动态列表：qq/wechat/email/github 四项，
     * 颜色/背景色 16 进制、priority 排序、visible 展示；app 端联系博主页按序渲染）。
     */
    private static List<SocialItem> defaultSocialItems() {
        return List.of(
                socialItem("企鹅号", "", "#12b7f5", "#12b7f51A", 1),
                socialItem("微信号", "", "#07c160", "#07c1601A", 2),
                socialItem("邮箱地址", "", "#f57c00", "#f57c001A", 3),
                socialItem("Github", "", "#24292f", "#24292f1A", 4));
    }

    private static SocialItem socialItem(String name, String content,
            String color, String bgColor, int priority) {
        SocialItem item = new SocialItem();
        item.setName(name);
        item.setContent(content);
        item.setColor(color);
        item.setBgColor(bgColor);
        item.setPriority(priority);
        item.setVisible(true);
        return item;
    }

    /**
     * 旧社交固定字段（authorConfig.social：enabled/qq/wechat/weibo/email/blog/bilibili/
     * juejin/csdn/gitee/github）迁移为 items 列表：仅取非空值的字段，name=平台中文名、
     * content=原值，颜色沿用默认社交项同款色板；enabled 忽略，不携带 key 平台标识。
     */
    private static JsonNode migrateLegacySocialItems(JsonNode oldSocial) {
        Map<String, String> names = Map.of(
                "qq", "企鹅号", "wechat", "微信号", "weibo", "微博地址", "email", "邮箱地址",
                "blog", "博客地址", "bilibili", "B站", "juejin", "掘金地址", "csdn", "CSDN",
                "gitee", "Gitee", "github", "Github");
        ObjectNode items = JsonNodeFactory.instance.objectNode();
        int priority = 1;
        for (Map.Entry<String, String> entry : names.entrySet()) {
            JsonNode value = oldSocial.get(entry.getKey());
            if (value == null || value.isNull() || value.asText().isBlank()) {
                continue;
            }
            SocialItem item = socialItem(entry.getValue(),
                    value.asText(), "#8a8a8a", "#8a8a8a1A", priority++);
            items.set(entry.getKey(),
                    JsonNodeFactory.instance.pojoNode(item));
        }
        if (items.size() == 0) {
            return null;
        }
        // 转数组
        tools.jackson.databind.node.ArrayNode array =
                JsonNodeFactory.instance.arrayNode();
        for (JsonNode node : items) {
            array.add(node);
        }
        return array;
    }

    private static Pages buildDefaultPages() {
        Pages pages = new Pages();

        Home home = new Home();
        home.setPageTitle("首页");
        home.setUseQuickNavigation(true);
        // 快捷导航默认 5 项（对齐客户端 uh-home-quick-nav 默认 navList，
        // 控制台可逐项配置）
        home.setQuickNavigation(defaultQuickNavigation());
        home.setUseCategory(true);
        // 首页分类栏选中引用：默认空（未选择时客户端回退内置行为），由站长挑选（固定 3 个）
        home.setCategories(List.of());
        pages.setHomeConfig(home);

        Gallery gallery = new Gallery();
        gallery.setPageTitle("图库");
        pages.setGalleryConfig(gallery);

        // 分类页/瞬间页标题（默认留空，客户端回退内置标题）
        CategoryPage categoryPage = new CategoryPage();
        categoryPage.setPageTitle("");
        pages.setCategoryConfig(categoryPage);

        MomentPage momentPage = new MomentPage();
        momentPage.setPageTitle("");
        pages.setMomentConfig(momentPage);

        // 其余功能页面标题（默认留空，客户端回退内置标题）
        LoveDiaryPage loveDiaryPage = new LoveDiaryPage();
        loveDiaryPage.setPageTitle("");
        // 恋爱页背景图（默认留空客户端内置回退）
        loveDiaryPage.setBgImageUrl("");
        pages.setLoveDiaryConfig(loveDiaryPage);

        ContactPage contactPage = new ContactPage();
        contactPage.setPageTitle("");
        pages.setContactConfig(contactPage);

        FavoritesPage favoritesPage = new FavoritesPage();
        favoritesPage.setPageTitle("");
        pages.setFavoritesConfig(favoritesPage);

        FriendLinksPage friendLinksPage = new FriendLinksPage();
        friendLinksPage.setPageTitle("");
        pages.setFriendLinksConfig(friendLinksPage);

        ArchivesPage archivesPage = new ArchivesPage();
        archivesPage.setPageTitle("");
        pages.setArchivesConfig(archivesPage);

        VotePage votePage = new VotePage();
        votePage.setPageTitle("");
        pages.setVoteConfig(votePage);

        DataVisualPage dataVisualPage = new DataVisualPage();
        dataVisualPage.setPageTitle("");
        pages.setDataVisualConfig(dataVisualPage);

        SettingPage settingPage = new SettingPage();
        settingPage.setPageTitle("");
        pages.setSettingConfig(settingPage);

        AboutProjectPage aboutProjectPage = new AboutProjectPage();
        aboutProjectPage.setPageTitle("");
        pages.setAboutProjectConfig(aboutProjectPage);

        NoticePage noticePage = new NoticePage();
        noticePage.setPageTitle("");
        pages.setNoticeConfig(noticePage);

        SearchPage searchPage = new SearchPage();
        searchPage.setPageTitle("");
        pages.setSearchConfig(searchPage);

        About about = new About();
        about.setPageTitle("关于博主");
        about.setBgImageUrl("/plugins/plugin-uni-halo/assets/static/uni_halo_profile_bg.jpg");
        about.setWaveImageUrl("/plugins/plugin-uni-halo/assets/static/uni_halo_about_wave.gif");
        // 页脚版权由应用资料 profile.copyrightConfig 承担（见 buildDefaultProfile）
        pages.setAboutConfig(about);

        // 免责声明页（不再需要启用开关，仅内容，默认留空）
        Disclaimer disclaimer = new Disclaimer();
        disclaimer.setContent("");
        pages.setDisclaimers(disclaimer);

        // 文章详情页内容与版权文案
        PostDetail postDetail = new PostDetail();
        postDetail.setShowComment(true);
        postDetail.setCopyrightEnabled(true);
        postDetail.setCopyrightAuthor("uni-halo");
        postDetail.setCopyrightDesc("使用《非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0)》"
                + "协议授权，文章来源于网上收集或者原创，若未在文章内说明的均为原创文章");
        postDetail.setCopyrightViolation("若侵害到您的权利，请您及时联系我，在收到通知后第一时间处理，"
                + "邮箱：xxxx@xx.com");
        pages.setPostDetailConfig(postDetail);

        // 我的页面功能入口：默认填充注册表条目——常用功能=home 组、其他功能=other 组，
        // 与前端 ui/src/constant/feature-entries.ts 注册表对齐（常用 7 项 / 其他 3 项）
        MyPage myPage = new MyPage();
        myPage.setCommonFeatures(defaultMyPageCommonFeatures());
        myPage.setOtherFeatures(defaultMyPageOtherFeatures());
        pages.setMyPageConfig(myPage);
        return pages;
    }

    /**
     * 我的页面-常用功能默认 7 项（对齐 app 端 about.vue navList：
     * 联系博主/我的收藏/恋爱日记/友情链接/文章归档/投票中心/数据看板，顺序即展示顺序；
     * bgColor 用品牌深色 hex8（app 端 about.vue 经 toLightBg 渲染为浅底）；
     * subTitle 对齐 app 端本地默认 rightText（favorites 无副标题）。
     */
    private static List<QuickNavigationItem> defaultMyPageCommonFeatures() {
        List<QuickNavigationItem> items = new ArrayList<>();
        QuickNavigationItem contactBlogger = navItem("contact-blogger", "联系博主", "#FF9800", "#FF9800F2",
                "uhemoji2-icon", "-wink", "/pages-blog/contact/contact");
        contactBlogger.setSubTitle("博主常用联系方式");
        items.add(contactBlogger);
        items.add(navItem("favorites", "我的收藏", "#FFB300", "#FFB300F2",
                "uhemoji2-icon", "-smiling", "/pages-blog/favorites/favorites"));
        QuickNavigationItem love = navItem("love", "恋爱日记", "#FF4C67", "#FF4C67F2",
                "uhemoji2-icon", "-in-love", "/pages-blog/love/love");
        love.setSubTitle("博主的恋爱日记");
        items.add(love);
        QuickNavigationItem friendLinks = navItem("friend-links", "友情链接", "#009688", "#009688F2",
                "uhemoji2-icon", "-cool", "/pages-blog/friend-links/friend-links");
        friendLinks.setSubTitle("看看博主朋友们吧");
        items.add(friendLinks);
        QuickNavigationItem archives = navItem("archives", "文章归档", "#03A9F4", "#03A9F4F2",
                "uhemoji2-icon", "-mask", "/pages-blog/archives/archives");
        archives.setSubTitle("全部文章");
        items.add(archives);
        QuickNavigationItem vote = navItem("vote", "投票中心", "#00BCD4", "#00BCD4F2",
                "uhemoji2-icon", "-confused", "/pages-blog/votes/votes");
        vote.setSubTitle("查看和进行投票");
        items.add(vote);
        QuickNavigationItem dataVisual = navItem("data-visual", "数据看板", "#663CC9", "#663CC9F2",
                "uhemoji2-icon", "-surprised", "/pages-blog/data-visual/data-visual");
        dataVisual.setSubTitle("站点数据可视化");
        items.add(dataVisual);
        return items;
    }

    /**
     * 我的页面-其他功能默认 3 项（对齐 app 端 about.vue navList：
     * 偏好设置/免责声明/关于项目，顺序即展示顺序；subTitle 对齐 app 端本地默认 rightText）。
     */
    private static List<QuickNavigationItem> defaultMyPageOtherFeatures() {
        List<QuickNavigationItem> items = new ArrayList<>();
        QuickNavigationItem setting = navItem("setting", "偏好设置", "#7986CB", "#7986CBF2",
                "uhemoji2-icon", "-tired", "/pages-blog/setting/setting");
        setting.setSubTitle("首页布局、卡片样式等本地偏好");
        items.add(setting);
        QuickNavigationItem disclaimers = navItem("disclaimers", "免责声明", "#795548", "#795548F2",
                "uhemoji2-icon", "-smirking", "/pages-blog/disclaimers/disclaimers");
        disclaimers.setSubTitle("博客内容免责声明");
        items.add(disclaimers);
        QuickNavigationItem about = navItem("about", "关于项目", "#607D8B", "#607D8BF2",
                "uhemoji2-icon", "-happy-", "/pages-blog/about/about");
        about.setSubTitle("小莫唐尼开源项目");
        items.add(about);
        return items;
    }

    /**
     * 默认快捷导航 5 项（对齐客户端 uh-home-quick-nav 默认 navList；
     * 控制台可配置，visible 默认全部显示，站长可逐项隐藏）。
     */
    private static List<QuickNavigationItem> defaultQuickNavigation() {
        List<QuickNavigationItem> items = new ArrayList<>();
        // 文章归档带副标题「全部文章」（对标 app 端 rightText）
        QuickNavigationItem archives = navItem("archives", "文章归档", "#03A9F4",
                "#03A9F424", "uhemoji2-icon", "-mask",
                "/pages-blog/archives/archives");
        archives.setSubTitle("全部文章");
        items.add(archives);
        items.add(navItem("vote", "投票中心", "#00BCD4", "#00BCD424",
                "uhemoji2-icon", "-confused", "/pages-blog/votes/votes"));
        items.add(navItem("disclaimers", "友情链接", "#009688", "#00968824",
                "uhemoji2-icon", "-wink", "/pages-blog/friend-links/friend-links"));
        items.add(navItem("love", "恋爱日记", "#FF4C67", "#FF4C6724",
                "uhemoji2-icon", "-in-love", "/pages-blog/love/love"));
        items.add(navItem("contact-blogger", "联系博主", "#FF9800", "#FF980024",
                "uhemoji2-icon", "-cool", "/pages-blog/contact/contact"));
        return items;
    }

    private static QuickNavigationItem navItem(String key, String title, String color,
            String bgColor, String iconPrefix, String icon, String path) {
        QuickNavigationItem item = new QuickNavigationItem();
        item.setKey(key);
        item.setTitle(title);
        item.setColor(color);
        item.setBgColor(bgColor);
        item.setIconPrefix(iconPrefix);
        item.setIcon(icon);
        item.setPath(path);
        item.setVisible(true);
        return item;
    }

    /**
     * 默认 assets：加载占位图（客户端内置回退兜底）；唯一内置默认 = 加载动图
     * （插件静态资源 /plugins/plugin-uni-halo/assets/static/…），
     * error 图留空走客户端回退。
     */
    private static Assets buildDefaultAssets() {
        Assets assets = new Assets();
        assets.setLoadingGifUrl("/plugins/plugin-uni-halo/assets/static/uni_halo_img_lazyload.gif");
        assets.setLoadingErrUrl("");
        return assets;
    }

    /**
     * 默认 love：恋爱日记入口仅密码（无 enabled 开关，入口显隐由页面设置-
     * 快捷导航/关于页功能入口注册表控制）；三模块入口默认值对齐 app 端 love.vue
     * 硬编码（title/subTitle 文案 + 颜色/图标背景色/跳转路径，priority 1/2/3）。
     * 恋爱页背景图由 pages.loveDiaryConfig.bgImageUrl 承担（默认留空）。
     */
    private static Love buildDefaultLove() {
        Love love = new Love();

        // 恋爱日记入口（恋爱页本身）：无开关，仅密码（默认未设置）
        ModuleSwitch loveDiary = new ModuleSwitch();
        loveDiary.setPasswordEnabled(false);
        love.setLoveDiary(loveDiary);

        // 恋爱故事模块默认开启（enabled=true）
        love.setOurStory(defaultLoveModule(true, "恋爱故事", "我们一起度过的那些经历",
                "#f83856", "#f8385699", "#fce7f3", "uhlove-icon", "gushi",
                "/pages-blog/love/stories", 1));

        // 恋爱相册模块默认关闭（enabled=false）
        love.setLovePhoto(defaultLoveModule(false, "恋爱相册", "定格了我们的那些小美好",
                "#60a5fa", "#93c5fd", "#dbeafe", "uhlove-icon", "xiangce",
                "/pages-blog/love/album", 2));

        // 恋爱清单模块默认关闭（enabled=false）
        love.setLoveDaily(defaultLoveModule(false, "恋爱清单", "你我之间的约定我们都在努力实现",
                "#f83856", "#f8385699", "#fce7f3", "uhlove-icon", "liebiao",
                "/pages-blog/love/list", 3));

        // 恋爱信息（纪念日 + 恋人信息）：默认留空（前端/输出端回退默认标题）
        love.setLoveInfo(new LoveInfo());
        return love;
    }

    /**
     * 三模块入口默认值（对齐 app 端 love.vue 现有硬编码：名称/副标题文案、
     * title/subTitle 颜色（hex8）、图标背景色、图标字体前缀与图标名、跳转路径与排序）。
     */
    private static ModuleSwitch defaultLoveModule(boolean enabled, String title,
            String subTitle, String titleColor, String subTitleColor, String iconBgColor,
            String iconPrefix, String icon, String path, int priority) {
        ModuleSwitch module = new ModuleSwitch();
        module.setEnabled(enabled);
        module.setTitle(title);
        module.setSubTitle(subTitle);
        module.setTitleColor(titleColor);
        module.setSubTitleColor(subTitleColor);
        module.setIconBgColor(iconBgColor);
        module.setIconPrefix(iconPrefix);
        module.setIcon(icon);
        module.setPath(path);
        module.setPriority(priority);
        module.setPasswordEnabled(false);
        return module;
    }

    /**
     * 维护模式默认：关闭；标题默认「站点维护中」；说明与排期窗口留空
     * （getConfigs 在 enabled=false 时不输出 maintenance 键，客户端视为未维护）。
     */
    private static Maintenance buildDefaultMaintenance() {
        Maintenance maintenance = new Maintenance();
        maintenance.setEnabled(false);
        maintenance.setTitle("站点维护中");
        maintenance.setNotice("");
        maintenance.setDescription("");
        return maintenance;
    }

    /**
     * 恋爱模块入口密码写入语义（与恋爱相册一致）：
     * 新密码非空 = 重设 BCrypt 哈希并启用；passwordRemoved = 清除密码；
     * 均未提供 = 保持现有哈希不变。写请求字段（password/passwordRemoved）
     * 消费后置空，避免落入存储。
     */
    private void applyLovePasswords(Love love, Love existing) {
        if (love == null) {
            return;
        }
        applyModulePassword(love.getLoveDiary(), existing != null ? existing.getLoveDiary() : null);
        applyModulePassword(love.getOurStory(), existing != null ? existing.getOurStory() : null);
        applyModulePassword(love.getLovePhoto(),
                existing != null ? existing.getLovePhoto() : null);
        applyModulePassword(love.getLoveDaily(), existing != null ? existing.getLoveDaily() : null);
    }

    private void applyModulePassword(ModuleSwitch module, ModuleSwitch existing) {
        if (module == null) {
            return;
        }
        String oldHash = existing != null ? existing.getPasswordHash() : null;
        if (!isBlank(module.getPassword())) {
            module.setPasswordHash(passwordEncoder.encode(module.getPassword()));
            module.setPasswordEnabled(true);
        } else if (Boolean.TRUE.equals(module.getPasswordRemoved())) {
            module.setPasswordHash(null);
            module.setPasswordEnabled(false);
        } else {
            module.setPasswordHash(oldHash);
            module.setPasswordEnabled(oldHash != null);
        }
        module.setPassword(null);
        module.setPasswordRemoved(null);
    }

    /**
     * 控制台读写响应脱敏：passwordHash/password/passwordRemoved 一律置空不回显，
     * passwordEnabled 按哈希是否为空派生（前端据此展示「已设置密码」状态）。
     * 返回深拷贝后的脱敏对象，不改动原始对象（save 场景避免污染落库入参）。
     */
    private GeneralConfig maskLovePasswords(GeneralConfig config) {
        if (config == null || config.getSpec() == null) {
            return config;
        }
        GeneralConfig copy = objectMapper.convertValue(config, GeneralConfig.class);
        Love love = copy.getSpec() != null ? copy.getSpec().getLove() : null;
        if (love != null) {
            maskModulePassword(love.getLoveDiary());
            maskModulePassword(love.getOurStory());
            maskModulePassword(love.getLovePhoto());
            maskModulePassword(love.getLoveDaily());
        }
        return copy;
    }

    private static void maskModulePassword(ModuleSwitch module) {
        if (module == null) {
            return;
        }
        module.setPasswordEnabled(!isBlank(module.getPasswordHash()));
        module.setPasswordHash(null);
        module.setPassword(null);
        module.setPasswordRemoved(null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 维护时间窗口校验：startTime 与 endTime 同时存在且 start ≥ end（含等号）时拒绝保存。
     * 时间字符串按 {@link MaintenanceResolver#parseTime} 解析（非法视为未设置，
     * 由输出端按该语义兜底判定，不因脏数据拒绝整个保存）。
     */
    private static void validateMaintenance(Maintenance maintenance) {
        if (maintenance == null) {
            return;
        }
        Instant start = MaintenanceResolver.parseTime(maintenance.getStartTime());
        Instant end = MaintenanceResolver.parseTime(maintenance.getEndTime());
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("维护结束时间必须晚于开始时间");
        }
    }
}
