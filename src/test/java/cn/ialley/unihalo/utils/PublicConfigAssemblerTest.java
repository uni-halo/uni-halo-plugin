package cn.ialley.unihalo.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import cn.ialley.unihalo.scheme.GeneralConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公开配置输出合成器单测：内容组重建、敏感/下线字段剔除、其余组透传。
 *
 * @author 小莫唐尼
 */
class PublicConfigAssemblerTest {

    private final PublicConfigAssembler assembler = new PublicConfigAssembler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造一份"旧 ConfigMap"形态的设置（含 🟦 内容组旧值、敏感/死字段）。
     */
    private static Map<String, JsonNode> legacySettings() {
        Map<String, JsonNode> settings = new LinkedHashMap<>();

        ObjectNode basicConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode tokenConfig = JsonNodeFactory.instance.objectNode();
        tokenConfig.put("personalToken", "should-not-leak");
        basicConfig.set("tokenConfig", tokenConfig);
        basicConfig.put("showAboutSystem", true);
        settings.put("basicConfig", basicConfig);

        ObjectNode pageConfig = JsonNodeFactory.instance.objectNode();
        pageConfig.set("homeConfig", JsonNodeFactory.instance.objectNode());
        settings.put("pageConfig", pageConfig);

        ObjectNode authorConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode blogger = JsonNodeFactory.instance.objectNode();
        blogger.put("nickname", "legacy-nickname");
        authorConfig.set("blogger", blogger);
        settings.put("authorConfig", authorConfig);

        ObjectNode imagesConfig = JsonNodeFactory.instance.objectNode();
        imagesConfig.put("defaultImageUrl", "legacy-image-url");
        settings.put("imagesConfig", imagesConfig);

        ObjectNode appConfig = JsonNodeFactory.instance.objectNode();
        appConfig.set("startConfig", JsonNodeFactory.instance.objectNode());
        ObjectNode appInfo = JsonNodeFactory.instance.objectNode();
        appInfo.put("name", "UniHalo v3.x");
        appInfo.put("appId", "wx-xxx");
        appInfo.put("appSecret", "secret-not-for-public");
        appConfig.set("appInfo", appInfo);
        settings.put("appConfig", appConfig);

        ObjectNode auditConfig = JsonNodeFactory.instance.objectNode();
        auditConfig.put("auditModeEnabled", true);
        auditConfig.set("auditModeData", JsonNodeFactory.instance.objectNode());
        settings.put("auditConfig", auditConfig);

        ObjectNode loveConfig = JsonNodeFactory.instance.objectNode();
        loveConfig.put("loveEnabled", true);
        loveConfig.set("ourStory", JsonNodeFactory.instance.objectNode().put("enabled", true));
        settings.put("loveConfig", loveConfig);
        // loveConfig（2026-09-03 迁入 GeneralConfig.spec.love）：旧 ConfigMap 值仅作
        // 导入源，不再透传输出；loveConfig 顶层键由 spec.love 重建。
        // 2026-09-10 起总开关 loveEnabled 已下线，仅模块开关/页面图片参与重建。
        return settings;
    }

    private GeneralConfig config(String json) throws Exception {
        return objectMapper.readValue(json, GeneralConfig.class);
    }

    @Test
    void shouldStripSensitiveAndRetireFieldsAndPassThroughOthers() throws Exception {
        // 内容组由 GeneralConfig 重建；love 分区映射回旧 loveConfig 顶层键（脱敏输出；
        // 2026-09-10 起总开关 loveEnabled 已下线；pageImages 已迁移至
        // pageConfig.loveDiaryConfig.bgImageUrl，不再经 loveConfig 下发）
        GeneralConfig config = config("""
                {"spec":{"profile":{"blogger":{"nickname":"测试博主"}},
                  "love":{"ourStory":{"enabled":true,"iconUrl":"","passwordHash":"$2a$10$fakehash"},
                    "lovePhoto":{"enabled":false,"iconUrl":""},
                    "loveDaily":{"enabled":false,"iconUrl":""}}}}
                """);

        ObjectNode root = assembler.assemble(legacySettings(), config);

        // 剔除：startConfig / auditModeData / tokenConfig
        assertNull(root.get("appConfig").get("startConfig"), "startConfig 已下线应剔除");
        assertEquals("UniHalo v3.x", root.get("appConfig").get("appInfo").get("name").asText());
        assertNull(root.get("auditConfig").get("auditModeData"), "auditModeData 死字段应剔除");
        assertTrue(root.get("auditConfig").get("auditModeEnabled").asBoolean());
        // 内容组由 GeneralConfig 重建：basicConfig 不再输出（内容已迁入页面设置）
        assertFalse(root.has("basicConfig"), "版权/免责/文章详情迁出后 basicConfig 不应输出");
        assertTrue(root.has("authorConfig"), "authorConfig 应由 GeneralConfig 重建");
        assertEquals("测试博主",
                root.get("authorConfig").get("blogger").get("nickname").asText());
        // loveConfig 由 spec.love 重建（不再透传旧 ConfigMap loveConfig 值；
        // 总开关 loveEnabled 已下线不输出；pageImages 已迁移不输出）
        assertFalse(root.get("loveConfig").has("loveEnabled"),
                "总开关 loveEnabled 已下线不应输出");
        assertFalse(root.get("loveConfig").has("pageImages"),
                "pageImages 已迁移至 pageConfig.loveDiaryConfig.bgImageUrl 不应输出");
        assertTrue(root.get("loveConfig").get("ourStory").get("enabled").asBoolean());
        // 模块仅 enabled + passwordEnabled
        assertTrue(root.get("loveConfig").get("ourStory").get("passwordEnabled").asBoolean(),
                "ourStory 有密码哈希 → passwordEnabled=true");
        assertFalse(root.get("loveConfig").get("lovePhoto").get("passwordEnabled").asBoolean());
        assertFalse(root.get("loveConfig").get("ourStory").has("passwordHash"),
                "密码哈希一律不下发");
        assertFalse(root.get("loveConfig").get("ourStory").has("iconUrl"),
                "已下线 iconUrl 不应输出");
    }

    @Test
    void shouldRebuildContentGroupsFromGeneralConfigSpec() throws Exception {
        // 2026-09-10 起版权/免责/文章详情迁移至页面设置：pageConfig.aboutConfig 版权、
        // pageConfig.disclaimers、pageConfig.postDetailConfig；basicConfig 组不再输出
        GeneralConfig config = config("""
                {"spec":{
                  "profile":{
                    "blogger":{"nickname":"新博主","avatar":"https://a/1.png","email":"a@b.com","description":"简介","website":"https://site.com"},
                    "social":{"items":[{"name":"企鹅号","content":"123","color":"#12b7f5","bgColor":"#12b7f51A","priority":1,"visible":true}]},
                    "copyrightConfig":{"enabled":true,"content":"© 版权"}
                  },
                  "pages":{
                    "homeConfig":{"pageTitle":"首页","useQuickNavigation":true,"useCategory":false},
                    "galleryConfig":{"pageTitle":"图库"},
                    "categoryConfig":{"pageTitle":"分类"},
                    "momentConfig":{"pageTitle":"瞬间"},
                    "aboutConfig":{"pageTitle":"关于博主","bgImageUrl":"https://bg/1.jpg","waveImageUrl":""},
                    "disclaimers":{"content":"<p>免责声明</p>"},
                    "postDetailConfig":{"showComment":true,"copyrightEnabled":false,
                       "copyrightAuthor":"uni-halo","copyrightDesc":"desc","copyrightViolation":"vio"}
                  },
                  "assets":{"loadingGifUrl":"https://img/loading.gif","loadingErrUrl":"https://img/err.png"}
                }}
                """);

        ObjectNode root = assembler.assemble(legacySettings(), config);

        // authorConfig：来自 profile.blogger/social，旧值被覆盖（博主资料含官网地址）
        assertEquals("新博主", root.get("authorConfig").get("blogger").get("nickname").asText());
        assertEquals("https://site.com",
                root.get("authorConfig").get("blogger").get("website").asText());
        assertEquals("123", root.get("authorConfig").get("social").get("items").get(0).get("content").asText());
        // 2026-09-11 起社交项去掉 key 平台标识（app 端按 color/bgColor 色块渲染）
        assertFalse(root.get("authorConfig").get("social").get("items").get(0).has("key"),
                "社交项已去掉 key 平台标识不应输出");
        // basicConfig 不再输出（版权/免责/文章详情已迁入页面设置）
        assertFalse(root.has("basicConfig"), "basicConfig 内容迁出后不应输出");
        // pageConfig：版权随 aboutConfig 输出、免责声明/文章详情随行输出
        assertEquals("© 版权", root.get("pageConfig").get("aboutConfig")
                .get("copyrightConfig").get("content").asText());
        assertEquals("<p>免责声明</p>",
                root.get("pageConfig").get("disclaimers").get("content").asText());
        assertTrue(root.get("pageConfig").get("postDetailConfig").get("showComment").asBoolean());
        assertEquals("uni-halo",
                root.get("pageConfig").get("postDetailConfig").get("copyrightAuthor").asText());
        // pageConfig：homeConfig 轮播渲染参数已下线（2026-09-08），分类页/瞬间页标题随行输出
        assertFalse(root.get("pageConfig").get("homeConfig").get("useCategory").asBoolean());
        assertFalse(root.get("pageConfig").get("homeConfig").has("bannerConfig"),
                "bannerConfig 已下线不应输出");
        assertEquals("图库", root.get("pageConfig").get("galleryConfig").get("pageTitle").asText());
        assertEquals("分类",
                root.get("pageConfig").get("categoryConfig").get("pageTitle").asText());
        assertEquals("瞬间",
                root.get("pageConfig").get("momentConfig").get("pageTitle").asText());
        // imagesConfig：来自 assets（2026-09-08 起仅 loading/error 两键，默认图片配置已下线）
        assertEquals("https://img/loading.gif",
                root.get("imagesConfig").get("loadingGifUrl").asText());
        assertEquals("https://img/err.png",
                root.get("imagesConfig").get("loadingErrUrl").asText());
        assertFalse(root.get("imagesConfig").has("defaultImageUrl"),
                "defaultImageUrl 配置已下线不应输出");
        assertFalse(root.get("imagesConfig").has("loadingEmptyUrl"),
                "loadingEmptyUrl 配置已下线不应输出");
        assertNotNull(root.get("appConfig").get("appInfo"));
    }

    @Test
    void shouldRebuildAuditConfigFromSpecAuditMode() throws Exception {
        // 审核模式开关（2026-09-11 由 setting safetyConfig.auditConfig 迁入 spec.auditMode）：
        // 输出端重建回旧 auditConfig.auditModeEnabled 形态，覆盖旧 ConfigMap 残留透传值
        // （legacySettings 中 auditModeEnabled=true），客户端 shape 不变。
        GeneralConfig config = config("""
                {"spec":{"auditMode":{"enabled":false}}}
                """);

        ObjectNode root = assembler.assemble(legacySettings(), config);

        assertFalse(root.get("auditConfig").get("auditModeEnabled").asBoolean(),
                "spec.auditMode.enabled=false 应覆盖 legacy 透传的 true");
        assertFalse(root.get("auditConfig").has("auditModeData"),
                "auditModeData 死字段不应随重建输出");
    }

    @Test
    void shouldOutputAuditConfigEnabledFromSpecAuditMode() throws Exception {
        // 开启审核模式：输出 auditConfig.auditModeEnabled=true（app 端据此过滤数据展示）
        GeneralConfig config = config("""
                {"spec":{"auditMode":{"enabled":true}}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertTrue(root.get("auditConfig").get("auditModeEnabled").asBoolean());
    }

    @Test
    void shouldKeepRawSettingsWhenNoContentSpec() throws Exception {
        GeneralConfig config = config("{}");

        ObjectNode root = assembler.assemble(legacySettings(), config);

        assertFalse(root.has("authorConfig"));
        assertFalse(root.has("pageConfig"));
        assertFalse(root.has("imagesConfig"));
        assertFalse(root.has("basicConfig"));
        // loveConfig 已属重建组：无 spec.love 时不再透传旧 ConfigMap 值
        assertFalse(root.has("loveConfig"));
        assertNull(root.get("appConfig").get("startConfig"));
    }

    @Test
    void shouldExpandDomainGroupsIntoLegacyShape() throws Exception {
        // 方案 B 三域结构（featureConfig/safetyConfig/integrationConfig）
        Map<String, JsonNode> settings = new LinkedHashMap<>();

        ObjectNode featureConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode loveConfig = JsonNodeFactory.instance.objectNode();
        loveConfig.put("loveEnabled", true);
        ObjectNode ourStory = JsonNodeFactory.instance.objectNode();
        ourStory.put("enabled", true);
        loveConfig.set("ourStory", ourStory);
        featureConfig.set("loveConfig", loveConfig);
        ObjectNode linkConfig = JsonNodeFactory.instance.objectNode();
        linkConfig.put("submissionEnabled", false);
        featureConfig.set("linkConfig", linkConfig);
        settings.put("featureConfig", featureConfig);

        ObjectNode safetyConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode captchaConfig = JsonNodeFactory.instance.objectNode();
        captchaConfig.put("enabled", true);
        ObjectNode scope = JsonNodeFactory.instance.objectNode();
        scope.put("linkSubmission", true);
        captchaConfig.set("scope", scope);
        safetyConfig.set("captchaConfig", captchaConfig);
        ObjectNode auditConfig = JsonNodeFactory.instance.objectNode();
        auditConfig.put("auditModeEnabled", true);
        safetyConfig.set("auditConfig", auditConfig);
        settings.put("safetyConfig", safetyConfig);

        ObjectNode integrationConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode pluginConfig = JsonNodeFactory.instance.objectNode();
        // 2026-09-10 起 votePlugin/linksPlugin 已下线：残留键应在输出端剔除；
        // toolsPlugin 保留透传（此处用 toolsPlugin 验证 integrationConfig.pluginConfig 展开）
        ObjectNode toolsPlugin = JsonNodeFactory.instance.objectNode();
        toolsPlugin.put("enabled", true);
        pluginConfig.set("toolsPlugin", toolsPlugin);
        integrationConfig.set("pluginConfig", pluginConfig);
        settings.put("integrationConfig", integrationConfig);

        // 基本配置（应用信息：仅名称/图标，动态码字段已下线）
        ObjectNode baseConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode appInfo = JsonNodeFactory.instance.objectNode();
        appInfo.put("name", "uni-halo");
        appInfo.put("logo", "https://uni-halo.925i.cn/uni_halo/uni_halo_logo.png");
        baseConfig.set("appInfo", appInfo);
        settings.put("baseConfig", baseConfig);

        ObjectNode root = assembler.assemble(settings, config("{}"));

        // 域组展开为旧模块顶层键
        // loveConfig 已迁出 setting（2026-09-03）：featureConfig.loveConfig 残留不再展开输出
        assertFalse(root.has("loveConfig"));
        assertFalse(root.get("linkConfig").get("submissionEnabled").asBoolean());
        assertTrue(root.get("captchaConfig").get("enabled").asBoolean());
        assertTrue(root.get("captchaConfig").get("scope").get("linkSubmission").asBoolean());
        assertTrue(root.get("auditConfig").get("auditModeEnabled").asBoolean());
        // 2026-09-10 起 votePlugin/linksPlugin 下线：残留键剔除；toolsPlugin 保留透传
        assertTrue(root.get("pluginConfig").get("toolsPlugin").get("enabled").asBoolean());
        assertFalse(root.get("pluginConfig").has("votePlugin"));
        // baseConfig.appInfo 归并为旧 appConfig.appInfo 形态
        assertEquals("uni-halo", root.get("appConfig").get("appInfo").get("name").asText());
        assertFalse(root.get("appConfig").has("startConfig"));
        assertFalse(root.get("appConfig").get("appInfo").has("appId"));
        // 域组/基本配置组本身不再出现在输出
        assertFalse(root.has("featureConfig"));
        assertFalse(root.has("safetyConfig"));
        assertFalse(root.has("integrationConfig"));
        assertFalse(root.has("baseConfig"));
        // 无内容 spec 时内容组不输出
        assertFalse(root.has("authorConfig"));
    }

    @Test
    void shouldRebuildLoveConfigFromSpecLove() throws Exception {
        GeneralConfig config = config("""
                {"spec":{"love":{
                  "loveDiary":{"enabled":true,"passwordHash":"$2a$10$fakehash",
                    "passwordEnabled":true},
                  "ourStory":{"enabled":true,"title":"恋爱故事","subTitle":"我们一起度过的那些经历",
                    "titleColor":"#f83856","subTitleColor":"#f8385699","iconBgColor":"#fce7f3",
                    "path":"/pages-blog/love/stories","priority":2,
                    "passwordHash":"$2a$10$fakehash","passwordEnabled":true},
                  "lovePhoto":{"enabled":false,"title":"恋爱相册","subTitle":"定格了我们的那些小美好",
                    "titleColor":"#60a5fa","subTitleColor":"#93c5fd","iconBgColor":"#dbeafe",
                    "path":"/pages-blog/love/album","priority":1},
                  "loveDaily":{"enabled":true,"title":"恋爱清单","subTitle":"你我之间的约定我们都在努力实现",
                    "titleColor":"#f83856","subTitleColor":"#f8385699","iconBgColor":"#fce7f3",
                    "path":"/pages-blog/love/list","priority":3,
                    "passwordHash":"","passwordEnabled":false}
                }}}
                """);

        ObjectNode root = assembler.assemble(legacySettings(), config);

        // spec.love → 旧顶层 loveConfig shape；脱敏输出：
        // 2026-09-10 起总开关 loveEnabled 已下线；pageImages 已迁移不输出
        assertFalse(root.get("loveConfig").has("loveEnabled"),
                "总开关 loveEnabled 已下线不应输出");
        assertFalse(root.get("loveConfig").has("pageImages"),
                "pageImages 已迁移至 pageConfig.loveDiaryConfig.bgImageUrl 不应输出");
        assertFalse(root.get("loveConfig").has("navList"),
                "navList 已下线（三模块自身承载入口列表数据）不应输出");
        // 恋爱日记入口（页面入口）：仅 passwordEnabled，无 enabled 开关
        assertTrue(root.get("loveConfig").get("loveDiary").get("passwordEnabled").asBoolean(),
                "loveDiary 有密码哈希 → passwordEnabled=true");
        assertFalse(root.get("loveConfig").get("loveDiary").has("enabled"),
                "loveDiary 无 enabled 开关（入口显隐由快捷导航/功能入口注册表控制）");
        assertFalse(root.get("loveConfig").get("loveDiary").has("passwordHash"),
                "密码哈希一律不下发");
        // 三模块入口：enabled + passwordEnabled + 入口列表数据，按 priority 降序输出
        assertTrue(root.get("loveConfig").get("loveDaily").get("enabled").asBoolean());
        assertTrue(root.get("loveConfig").get("ourStory").get("passwordEnabled").asBoolean(),
                "ourStory 有哈希 → passwordEnabled=true");
        assertFalse(root.get("loveConfig").get("loveDaily").get("passwordEnabled").asBoolean(),
                "loveDaily 哈希为空 → passwordEnabled=false");
        assertEquals("恋爱故事", root.get("loveConfig").get("ourStory").get("title").asText());
        assertEquals("#f8385699",
                root.get("loveConfig").get("ourStory").get("subTitleColor").asText());
        assertEquals("#dbeafe",
                root.get("loveConfig").get("lovePhoto").get("iconBgColor").asText());
        assertEquals("/pages-blog/love/list",
                root.get("loveConfig").get("loveDaily").get("path").asText());
        assertFalse(root.get("loveConfig").get("ourStory").has("passwordHash"),
                "密码哈希一律不下发");
        assertFalse(root.get("loveConfig").get("ourStory").has("iconUrl"),
                "已下线 iconUrl 不应输出");
        // 三模块按 priority 降序输出（app 端按序渲染入口列表）
        List<String> loveKeys = new ArrayList<>();
        root.get("loveConfig").properties().forEach(entry -> loveKeys.add(entry.getKey()));
        assertEquals(List.of("loveDiary", "loveDaily", "ourStory", "lovePhoto"), loveKeys,
                "loveDiary 在前，三模块按 priority 降序（loveDaily 3 > ourStory 2 > lovePhoto 1）");
        // legacySettings 顶层 loveConfig 旧值（loveEnabled=true）不再透传输出
        assertFalse(root.get("loveConfig").has("loveDate"));
        assertFalse(root.get("loveConfig").has("loveInfo"));
    }

    @Test
    void shouldDerivePasswordEnabledFromMaskedInput() throws Exception {
        // 生产链路回归（2026-09-12）：getConfigs 入参为 generalConfigService.get()
        // 的脱敏结果（maskLovePasswords 已把 passwordHash 置空、passwordEnabled 按
        // 原始哈希派生），sanitizeLove 不得再依赖已置空的哈希（否则恒 false）。
        GeneralConfig config = config("""
                {"spec":{"love":{
                  "loveDiary":{"enabled":true,"passwordEnabled":true,
                    "passwordHash":null,"password":null,"passwordRemoved":null},
                  "ourStory":{"enabled":true,"passwordEnabled":false,
                    "passwordHash":null},
                  "lovePhoto":{"enabled":false,"passwordEnabled":false,
                    "passwordHash":null},
                  "loveDaily":{"enabled":false,"passwordEnabled":false,
                    "passwordHash":null}
                }}}
                """);

        ObjectNode root = assembler.assemble(legacySettings(), config);

        assertTrue(root.get("loveConfig").get("loveDiary").get("passwordEnabled").asBoolean(),
                "脱敏输入下 passwordEnabled 应透传已派生布尔（true）");
        assertFalse(root.get("loveConfig").get("loveDiary").has("enabled"),
                "loveDiary 无 enabled 开关（脱敏输入下亦不输出）");
        assertFalse(root.get("loveConfig").get("ourStory").get("passwordEnabled").asBoolean());
        assertFalse(root.get("loveConfig").get("loveDiary").has("passwordHash"),
                "密码哈希一律不下发");
        assertFalse(root.get("loveConfig").get("loveDiary").has("password"),
                "密码一律不下发");
    }

    // ===== 维护模式（2026-09-04 新增，additive 顶层键）=====

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    @Test
    void shouldNotOutputMaintenanceWhenDisabled() throws Exception {
        GeneralConfig config = config("""
                {"spec":{"maintenance":{"enabled":false,"title":"维护","description":""}}}
                """);

        ObjectNode root = assembler.assemble(null, config, NOW);

        assertFalse(root.has("maintenance"), "enabled=false 不应输出 maintenance 键");
    }

    @Test
    void shouldOutputMaintenanceWhenActive() throws Exception {
        GeneralConfig config = config("""
                {"spec":{"maintenance":{"enabled":true,"title":"系统升级维护",
                  "notice":"升级维护中，请稍后再试",
                  "description":"<p>维护详情</p>",
                  "startTime":"2026-09-03T22:00:00Z","endTime":"2026-09-05T02:00:00Z"}}}
                """);

        ObjectNode root = assembler.assemble(null, config, NOW);

        var maintenance = root.get("maintenance");
        assertTrue(root.has("maintenance"));
        assertEquals("active", maintenance.get("status").asText());
        assertEquals("系统升级维护", maintenance.get("title").asText());
        assertEquals("升级维护中，请稍后再试", maintenance.get("notice").asText());
        assertEquals("<p>维护详情</p>", maintenance.get("description").asText());
        assertEquals("2026-09-03T22:00:00Z", maintenance.get("startTime").asText());
        assertEquals("2026-09-05T02:00:00Z", maintenance.get("endTime").asText());
    }

    @Test
    void shouldOutputMaintenanceWhenScheduled() throws Exception {
        GeneralConfig config = config("""
                {"spec":{"maintenance":{"enabled":true,"title":"维护预告",
                  "startTime":"2026-09-05T00:00:00Z","endTime":null}}}
                """);

        ObjectNode root = assembler.assemble(null, config, NOW);

        assertEquals("scheduled", root.get("maintenance").get("status").asText());
        // endTime 为空 → 不输出该字段（客户端不渲染倒计时区）
        assertFalse(root.get("maintenance").has("endTime"));
    }

    @Test
    void shouldNotOutputMaintenanceWhenAutoEnded() throws Exception {
        // endTime 已到 → 视为已按计划自动结束（到点自动关闭）
        GeneralConfig config = config("""
                {"spec":{"maintenance":{"enabled":true,"title":"已结束的维护",
                  "startTime":"2026-09-02T00:00:00Z","endTime":"2026-09-03T00:00:00Z"}}}
                """);

        ObjectNode root = assembler.assemble(null, config, NOW);

        assertFalse(root.has("maintenance"), "endTime 已到不应输出 maintenance 键");
    }

    // ===== 友链信息（2026-09-08 拆分子结构 miniInfo/siteInfo；2026-09-10 起去 authorInfo）=====

    @Test
    void shouldPassThroughNestedLinkInfoWhenAnyTextPresent() throws Exception {
        // 嵌套子结构（对象内对象）：hasNonBlankText 必须递归检查，不能对 ObjectNode 调 asText
        GeneralConfig config = config("""
                {"spec":{"linkInfo":{
                  "submissionEnabled":false,
                  "miniInfo":{"displayName":"小程序","miniProgramCode":"gh_xxx","link":"/pages-blog/friend-links/friend-links"},
                  "siteInfo":{"displayName":"","url":"","logo":"","description":""}
                }}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertTrue(root.has("pluginConfig"), "存在非空子字段应输出 pluginConfig");
        JsonNode linkInfo = root.get("pluginConfig").get("linkInfo");
        assertNotNull(linkInfo);
        assertEquals("小程序", linkInfo.get("miniInfo").get("displayName").asText());
        // 2026-09-10 起作者信息已下线（由应用设置-博主资料承担），不再输出 authorInfo
        assertFalse(linkInfo.has("authorInfo"), "authorInfo 已下线不应输出");
        // 2026-09-11 起基本配置 submissionEnabled 随行输出（app 端提交入口显隐判断）
        assertFalse(linkInfo.get("submissionEnabled").asBoolean(),
                "submissionEnabled 显式配置应原样输出");
    }

    @Test
    void shouldNotOutputNestedLinkInfoWhenAllBlank() throws Exception {
        // 全部子字段为空（含嵌套空对象）→ 不输出 pluginConfig.linkInfo（app 端展示「暂未配置」占位）
        GeneralConfig config = config("""
                {"spec":{"linkInfo":{
                  "miniInfo":{"displayName":"","miniProgramCode":"","link":""},
                  "siteInfo":{"displayName":"","url":"","logo":"","description":""}
                }}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertFalse(root.has("pluginConfig"), "全部留空不应输出 pluginConfig.linkInfo");
    }

    @Test
    void shouldOutputLinkInfoWhenOnlySubmissionEnabledConfigured() throws Exception {
        // 仅显式配置 submissionEnabled（miniInfo/siteInfo 全空）也应输出
        // （app 端需据 submissionEnabled 隐藏「提交申请」入口，即使内容未配置）
        GeneralConfig config = config("""
                {"spec":{"linkInfo":{
                  "submissionEnabled":false,
                  "miniInfo":{"displayName":"","miniProgramCode":"","link":""},
                  "siteInfo":{"displayName":"","url":"","logo":"","description":""}
                }}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertTrue(root.has("pluginConfig"), "submissionEnabled 显式配置应输出 pluginConfig.linkInfo");
        assertFalse(root.get("pluginConfig").get("linkInfo").get("submissionEnabled").asBoolean());
    }

    @Test
    void shouldNotOutputLinkInfoWhenSubmissionEnabledMissingAndContentBlank() throws Exception {
        // submissionEnabled 缺失（未配置，默认 true 不落库）+ 内容全空 → 不输出
        // （老数据/默认结构下 app 端回退「暂未配置」占位，提交入口默认开放）
        GeneralConfig config = config("""
                {"spec":{"linkInfo":{
                  "miniInfo":{"displayName":"","miniProgramCode":"","link":""},
                  "siteInfo":{"displayName":"","url":"","logo":"","description":""}
                }}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertFalse(root.has("pluginConfig"), "无 submissionEnabled 且内容全空不应输出 linkInfo");
    }

    // ===== 我的页面功能入口（2026-09-10 新增，pageConfig.myPageConfig）=====

    @Test
    void shouldNotOutputMyPageConfigWhenBothGroupsEmpty() throws Exception {
        // 两组均为空 → 不输出 myPageConfig（app 端展示内置默认）；其他页配置不受影响
        GeneralConfig config = config("""
                {"spec":{"pages":{
                  "homeConfig":{"pageTitle":"首页"},
                  "myPageConfig":{"commonFeatures":[],"otherFeatures":[]}
                }}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        assertTrue(root.has("pageConfig"), "其他页配置存在时 pageConfig 仍输出");
        assertFalse(root.get("pageConfig").has("myPageConfig"),
                "两组均为空不应输出 myPageConfig 键");
        assertEquals("首页",
                root.get("pageConfig").get("homeConfig").get("pageTitle").asText());
    }

    @Test
    void shouldOutputMyPageConfigWhenAnyFeaturePresent() throws Exception {
        // 常用功能有条目 → 输出 myPageConfig（additive 键，含 subTitle）
        GeneralConfig config = config("""
                {"spec":{"pages":{"myPageConfig":{
                  "commonFeatures":[{"key":"archives","title":"文章归档","subTitle":"全部文章",
                    "color":"#03A9F4","bgColor":"rgba(3,169,244,0.14)",
                    "iconPrefix":"uhemoji2-icon","icon":"-mask",
                    "path":"/pages-blog/archives/archives","visible":true}],
                  "otherFeatures":[]
                }}}}
                """);

        ObjectNode root = assembler.assemble(null, config);

        var myPage = root.get("pageConfig").get("myPageConfig");
        assertNotNull(myPage, "存在功能条目应输出 myPageConfig");
        assertEquals("文章归档",
                myPage.get("commonFeatures").get(0).get("title").asText());
        assertEquals("全部文章",
                myPage.get("commonFeatures").get(0).get("subTitle").asText());
        assertTrue(myPage.get("commonFeatures").get(0).get("visible").asBoolean());
        assertFalse(myPage.get("otherFeatures").has(0), "空组不应有元素");
    }

    // ===== pluginConfig 残留剔除（2026-09-10 起 votePlugin/linksPlugin 下线）=====

    @Test
    void shouldStripRetiredPluginSwitchesFromPluginConfig() throws Exception {
        // 旧 ConfigMap 残留 votePlugin/linksPlugin → 输出端剔除；toolsPlugin 保留透传
        Map<String, JsonNode> settings = new LinkedHashMap<>();

        ObjectNode integrationConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode pluginConfig = JsonNodeFactory.instance.objectNode();
        ObjectNode votePlugin = JsonNodeFactory.instance.objectNode();
        votePlugin.put("enabled", true);
        pluginConfig.set("votePlugin", votePlugin);
        ObjectNode linksPlugin = JsonNodeFactory.instance.objectNode();
        linksPlugin.put("enabled", true);
        pluginConfig.set("linksPlugin", linksPlugin);
        ObjectNode toolsPlugin = JsonNodeFactory.instance.objectNode();
        toolsPlugin.put("enabled", true);
        pluginConfig.set("toolsPlugin", toolsPlugin);
        integrationConfig.set("pluginConfig", pluginConfig);
        settings.put("integrationConfig", integrationConfig);

        ObjectNode root = assembler.assemble(settings, config("{}"));

        assertFalse(root.get("pluginConfig").has("votePlugin"), "votePlugin 已下线应剔除");
        assertFalse(root.get("pluginConfig").has("linksPlugin"), "linksPlugin 已下线应剔除");
        assertTrue(root.get("pluginConfig").get("toolsPlugin").get("enabled").asBoolean(),
                "toolsPlugin 保留透传");
    }

    @Test
    void shouldOutputImmediateMaintenanceWithoutTimes() throws Exception {
        // enabled=true 无窗口 → active（立即维护、持续至手动关闭），startTime/endTime 均不输出
        GeneralConfig config = config("""
                {"spec":{"maintenance":{"enabled":true,"title":"立即维护","description":""}}}
                """);

        ObjectNode root = assembler.assemble(null, config, NOW);

        assertEquals("active", root.get("maintenance").get("status").asText());
        assertFalse(root.get("maintenance").has("startTime"));
        assertFalse(root.get("maintenance").has("endTime"));
    }
}
