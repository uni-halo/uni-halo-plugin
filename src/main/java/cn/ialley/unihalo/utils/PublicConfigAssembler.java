package cn.ialley.unihalo.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.ialley.unihalo.scheme.GeneralConfig;
import cn.ialley.unihalo.utils.MaintenanceResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 公开配置输出合成器（getConfigs 出口，只读）。
 *
 * <p>把「设置剩余组（ConfigMap）+ 通用配置单例 {@link GeneralConfig}」合成为小程序端
 * 依赖的旧 shape：</p>
 * <ul>
 *   <li>authorConfig / pageConfig / basicConfig（内容部分）/ imagesConfig / loveConfig
 *       五个顶层键由 GeneralConfig 重建（不再依赖 ConfigMap 旧组）；</li>
 *   <li>剔除敏感/无效字段：basicConfig.tokenConfig（个人令牌）、appConfig.startConfig
 *       （启动页配置）、auditConfig.auditModeData（死字段）；loginConfig 组只输出
 *       三个登录开关，Secret 名称与权限策略不外发；</li>
 *   <li>maintenance（additive 顶层键）：按 GeneralConfig.spec.maintenance
 *       时间窗口与当前时刻计算状态，仅 scheduled/active 时输出；</li>
 *   <li>其余设置组（captchaConfig / pluginConfig / linkConfig …）原样透传。</li>
 * </ul>
 *
 * @author 小莫唐尼
 */
public class PublicConfigAssembler {

    /**
     * 插件 Spring 上下文未注册 Jackson 3 ObjectMapper bean，故内部自行创建
     * （与 EmailService / GeneralConfigServiceImpl 同套路）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 合成公开配置输出（维护状态按当前时刻判定）。
     *
     * @param settings 当前插件设置（ReactiveSettingFetcher.getSettingValues 结果，可为空）
     * @param config   通用配置单例（可能为 null 或 spec 为空，此时内容组不输出）
     */
    public ObjectNode assemble(Map<String, JsonNode> settings, GeneralConfig config) {
        return assemble(settings, config, Instant.now());
    }

    /**
     * 合成公开配置输出（维护状态判定时钟可注入，便于测试覆盖
     * scheduled/active/到点自动结束等分支）。
     *
     * @param settings 当前插件设置（可为空）
     * @param config   通用配置单例（可能为 null 或 spec 为空）
     * @param now      维护状态判定的当前时刻（见 {@link MaintenanceResolver}）
     */
    public ObjectNode assemble(Map<String, JsonNode> settings, GeneralConfig config,
            Instant now) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        if (settings != null) {
            normalize(settings).forEach((group, node) -> {
                if (node == null || node.isNull()) {
                    return;
                }
                if (isContentGroup(group)) {
                    // 内容组由 GeneralConfig 重建，跳过原样透传
                    return;
                }
                if ("appConfig".equals(group)) {
                    root.set(group, withoutKeys(node, "startConfig"));
                } else if ("auditConfig".equals(group)) {
                    root.set(group, withoutKeys(node, "auditModeData"));
                } else if ("loginConfig".equals(group)) {
                    // 登录组：只下发「支持哪些登录方式」两个开关。
                    // wechatSecretName（Secret 资源名）、令牌有效期与注册策略
                    // 一律不外发——Secret 内容只能由服务端按名称读取。
                    root.set(group, sanitizeLogin(node));
                } else if ("pluginConfig".equals(group)) {
                    // votePlugin/linksPlugin 开关不再使用（app 端改用插件启用检测判定），
                    // 剔除旧 ConfigMap 残留避免继续透传；toolsPlugin 保留
                    root.set(group, withoutKeys(node, "votePlugin", "linksPlugin"));
                } else {
                    root.set(group, node);
                }
            });
        }

        JsonNode spec = toSpecTree(config);
        if (spec == null || !spec.isObject()) {
            return root;
        }
        JsonNode profile = spec.get("profile");
        if (profile != null && profile.isObject()) {
            ObjectNode authorConfig = JsonNodeFactory.instance.objectNode();
            pick(profile, authorConfig, "blogger", "social");
            if (authorConfig.size() > 0) {
                root.set("authorConfig", authorConfig);
            }
            // 版权/免责/文章详情位于页面设置（pageConfig.aboutConfig 版权 /
            // pageConfig.disclaimers / pageConfig.postDetailConfig），basicConfig 组不再输出；
            // 存量 basicConfig 由 isContentGroup 跳过透传
            JsonNode appInfo = profile.get("appInfo");
            if (appInfo != null && !appInfo.isNull()) {
                // 应用信息（名称/图标）→ appConfig.appInfo 形态（覆盖存量设置值）
                root.set("appConfig",
                        JsonNodeFactory.instance.objectNode().set("appInfo", appInfo));
            }
        }
        JsonNode pages = spec.get("pages");
        if (pages != null && pages.isObject()) {
            ObjectNode pageConfig = JsonNodeFactory.instance.objectNode();
            // postDetailConfig 文章详情页、disclaimers 免责声明页随行输出；
            // 页脚版权取自 profile.copyrightConfig，输出端映射回
            // pageConfig.aboutConfig.copyrightConfig（app 端消费位置不变）
            pick(pages, pageConfig, "homeConfig", "galleryConfig", "aboutConfig",
                    "categoryConfig", "momentConfig", "postDetailConfig", "disclaimers",
                    // 其余功能页面标题（均含 pageTitle）
                    "loveDiaryConfig", "contactConfig", "favoritesConfig",
                    "friendLinksConfig", "archivesConfig", "voteConfig", "dataVisualConfig",
                    "settingConfig", "aboutProjectConfig", "noticeConfig", "searchConfig");
            // 页脚版权（来自 profile.copyrightConfig；app 端 about.vue 读
            // pageConfig.aboutConfig.copyrightConfig 保持不变）
            JsonNode copyright = profile != null ? profile.get("copyrightConfig") : null;
            if (copyright != null && !copyright.isNull()) {
                JsonNode about = pageConfig.get("aboutConfig");
                if (about != null && about.isObject()) {
                    ((ObjectNode) about).set("copyrightConfig", copyright);
                }
            }
            // 我的页面功能入口（additive 键）：两组均为空时不输出，
            // app 端展示内置默认（语义同 linkInfo「全部留空不输出」）
            JsonNode myPage = pages.get("myPageConfig");
            if (myPage != null && myPage.isObject() && hasNonBlankText(myPage)) {
                pageConfig.set("myPageConfig", myPage);
            }
            if (pageConfig.size() > 0) {
                root.set("pageConfig", pageConfig);
            }
        }
        JsonNode assets = spec.get("assets");
        if (assets != null && !assets.isNull()) {
            root.set("imagesConfig", assets);
        }
        // 站点级展示偏好默认（L0，additive 顶层键）：客户端 layout.home/cardType/
        // isAvatarRadius 的站点默认来源（客户端可本地覆盖）
        JsonNode preferences = spec.get("preferences");
        if (preferences != null && !preferences.isNull()) {
            root.set("preferences", preferences);
        }
        // 恋爱模块：spec.love → 旧顶层 loveConfig shape（pageImages/模块开关）。
        // 模块开关脱敏输出：仅 enabled + passwordEnabled（是否已设置密码，
        // 客户端据此决定是否弹密码验证），密码相关字段一律不下发小程序端；
        // 入口展示由模块入口开关与 navList 统一管理。
        JsonNode love = spec.get("love");
        if (love != null && love.isObject()) {
            root.set("loveConfig", sanitizeLove(love));
        }
        // 维护模式（additive 顶层键）：状态由时间窗口按 now 计算，
        // 仅 scheduled/active 输出；enabled=false 或已到点自动结束 → 键缺失（客户端视为未维护）
        JsonNode maintenance = spec.get("maintenance");
        if (maintenance != null && maintenance.isObject()) {
            MaintenanceResolver.MaintenanceStatus status = MaintenanceResolver.resolve(
                    jsonBool(maintenance.get("enabled")),
                    jsonText(maintenance.get("startTime")),
                    jsonText(maintenance.get("endTime")), now);
            if (status != MaintenanceResolver.MaintenanceStatus.NONE) {
                ObjectNode maintenanceOut = JsonNodeFactory.instance.objectNode();
                maintenanceOut.put("status", status.name().toLowerCase());
                pick(maintenance, maintenanceOut, "title", "notice", "description",
                        "startTime", "endTime");
                root.set("maintenance", maintenanceOut);
            }
        }
        // 审核模式开关：输出端重建回旧 auditConfig.auditModeEnabled 形态，
        // 覆盖旧 ConfigMap 残留透传，客户端 shape 不变；auditModeData 死字段不随输出
        JsonNode auditMode = spec.get("auditMode");
        if (auditMode != null && auditMode.isObject()) {
            JsonNode enabled = auditMode.get("enabled");
            if (enabled != null && enabled.isBoolean()) {
                ObjectNode auditOut = JsonNodeFactory.instance.objectNode();
                auditOut.put("auditModeEnabled", enabled.asBoolean());
                root.set("auditConfig", auditOut);
            }
        }
        // 友链信息：spec.linkInfo 直接下发到 pluginConfig.linkInfo，
        // 结构 = {submissionEnabled, miniInfo, siteInfo}：
        // - submissionEnabled（基本配置）供小程序端「提交申请」入口显隐判断（默认 true）；
        // - miniInfo（小程序信息）供小程序端「申请信息」弹窗（uh-links-mini-info）读取；
        // - siteInfo（站点信息）字段对齐 Halo 官方 plugin-links 友链提交 API
        //   （link-applications 请求体：displayName/url/logo/description/backlink/feedUrls）。
        // app 端作者区使用应用设置-博主资料（authorConfig.blogger）。
        // 输出判定：仅 submissionEnabled 显式配置 或 miniInfo/siteInfo 有非空内容时输出
        // （全部留空不输出，app 端展示「暂未配置」占位）。
        // 旧 linksSubmitPlugin（blogName/blogLogo/blogUrl/blogDesc 等键）不再下发。
        JsonNode linkInfo = spec.get("linkInfo");
        if (linkInfo != null && linkInfo.isObject()) {
            ObjectNode linkInfoOut = (ObjectNode) linkInfo.deepCopy();
            // submissionEnabled 不参与内容空判断（默认 true 不应使空配置变为「已配置」）
            JsonNode submission = linkInfoOut.remove("submissionEnabled");
            boolean hasContent = hasNonBlankText(linkInfoOut);
            boolean hasSubmission = submission != null && submission.isBoolean();
            if (hasContent || hasSubmission) {
                if (hasSubmission) {
                    linkInfoOut.set("submissionEnabled", submission);
                }
                JsonNode pluginConfigNode = root.get("pluginConfig");
                ObjectNode pluginConfig = pluginConfigNode != null && pluginConfigNode.isObject()
                        ? (ObjectNode) pluginConfigNode.deepCopy()
                        : JsonNodeFactory.instance.objectNode();
                pluginConfig.set("linkInfo", linkInfoOut);
                root.set("pluginConfig", pluginConfig);
            }
        }
        return root;
    }

    /**
     * 把三域组与新「基本配置」组展开回「旧模块顶层键」形态（新旧结构二选一，
     * 不会同时存在）：featureConfig → linkConfig；safetyConfig →
     * auditConfig / captchaConfig；integrationConfig → pluginConfig；
     * baseConfig.appInfo → 归并至 appConfig.appInfo（保持客户端旧 shape）。
     * 旧结构原样保留。恋爱组（loveConfig）由 spec.love 重建输出，
     * 不再从 featureConfig.loveConfig 展开透传（旧 ConfigMap 残留键在此被丢弃）。
     */
    private static Map<String, JsonNode> normalize(Map<String, JsonNode> settings) {
        Map<String, JsonNode> out = new LinkedHashMap<>(settings);
        expand(out, "featureConfig", "linkConfig");
        expand(out, "safetyConfig", "auditConfig", "captchaConfig");
        expand(out, "integrationConfig", "pluginConfig");
        // 基本配置（应用信息：名称/图标）→ 输出为 appConfig.appInfo 形态
        JsonNode base = out.remove("baseConfig");
        if (base != null && base.isObject()) {
            JsonNode appInfo = base.get("appInfo");
            if (appInfo != null && !appInfo.isNull()) {
                JsonNode existing = out.get("appConfig");
                if (existing == null || !existing.isObject()) {
                    out.put("appConfig",
                            JsonNodeFactory.instance.objectNode().set("appInfo", appInfo));
                } else {
                    ObjectNode merged = (ObjectNode) existing.deepCopy();
                    merged.set("appInfo", appInfo);
                    out.put("appConfig", merged);
                }
            }
        }
        return out;
    }

    private static void expand(Map<String, JsonNode> out, String domain, String... modules) {
        JsonNode domainNode = out.remove(domain);
        if (domainNode == null || !domainNode.isObject()) {
            return;
        }
        for (String module : modules) {
            JsonNode value = domainNode.get(module);
            if (value != null && !value.isNull()) {
                out.put(module, value);
            }
        }
    }

    private JsonNode toSpecTree(GeneralConfig config) {
        if (config == null || config.getSpec() == null) {
            return null;
        }
        return objectMapper.valueToTree(config.getSpec());
    }

    /**
     * 恋爱配置公开输出脱敏（getConfigs loveConfig 组）：
     * 恋爱日记入口（页面入口）：仅输出 passwordEnabled（无 enabled 开关，
     * 入口显隐由页面设置-快捷导航/功能入口注册表控制，不在恋爱配置下发）；
     * 三模块入口（ourStory/lovePhoto/loveDaily）：enabled + passwordEnabled +
     * 入口列表数据（title/subTitle/titleColor/subTitleColor/iconBgColor/path/
     * priority，app 端直接按模块 key 渲染入口列表），按 priority 降序输出；
     * passwordEnabled 优先透传输入中已派生的布尔（getConfigs 入参经
     * generalConfigService.get() 脱敏，哈希已置空；直接传入原始 spec 时回退
     * 按 passwordHash 非空派生），password / passwordHash / passwordRemoved
     * 等密码字段一律不下发小程序端；
     * loveInfo（纪念日 + 恋人信息）：含任意非空字段时输出，
     * 全空不输出（app 端回退内置默认标题）。
     * 恋爱页背景图由 pageConfig.loveDiaryConfig.bgImageUrl 承担，不再经 loveConfig 下发。
     */
    private static JsonNode sanitizeLove(JsonNode love) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        // 恋爱日记入口（页面入口）：仅密码状态，无开关
        JsonNode loveDiary = love.get("loveDiary");
        if (loveDiary != null && loveDiary.isObject()) {
            ObjectNode loveDiaryOut = JsonNodeFactory.instance.objectNode();
            loveDiaryOut.put("passwordEnabled", hasModulePassword(loveDiary));
            if (loveDiaryOut.size() > 0) {
                out.set("loveDiary", loveDiaryOut);
            }
        }
        // 三模块入口：全字段输出，按 priority 降序（app 端按序渲染入口列表）
        List<String> moduleKeys = new ArrayList<>(List.of("ourStory", "lovePhoto", "loveDaily"));
        moduleKeys.sort(Comparator
                .comparingInt((String key) -> modulePriority(love.get(key))).reversed());
        for (String moduleKey : moduleKeys) {
            JsonNode module = love.get(moduleKey);
            if (module != null && module.isObject()) {
                ObjectNode moduleOut = JsonNodeFactory.instance.objectNode();
                pick(module, moduleOut, "enabled", "title", "subTitle", "titleColor",
                        "subTitleColor", "iconBgColor", "iconPrefix", "icon", "path", "priority");
                moduleOut.put("passwordEnabled", hasModulePassword(module));
                if (moduleOut.size() > 0) {
                    out.set(moduleKey, moduleOut);
                }
            }
        }
        // 恋爱信息（additive：无内容不输出，app 端回退内置默认）
        JsonNode loveInfo = love.get("loveInfo");
        if (loveInfo != null && loveInfo.isObject() && hasNonBlankText(loveInfo)) {
            out.set("loveInfo", loveInfo);
        }
        return out;
    }

    /**
     * 登录配置公开输出脱敏（getConfigs loginConfig 组）：
     * 仅输出 passwordLoginEnabled / wechatLoginEnabled 两个开关，
     * 供小程序端决定登录页展示哪些入口；wechatSecretName（Secret 资源名）、
     * 令牌有效期与注册策略（Halo 系统设置的「允许注册」「默认角色」）
     * 属于服务端决策，全部不下发。
     */
    private static JsonNode sanitizeLogin(JsonNode node) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        JsonNode login = node.get("loginConfig");
        if (login != null && login.isObject()) {
            ObjectNode loginOut = JsonNodeFactory.instance.objectNode();
            pick(login, loginOut, "passwordLoginEnabled", "wechatLoginEnabled");
            if (loginOut.size() > 0) {
                out.set("loginConfig", loginOut);
            }
        }
        return out;
    }

    /** 模块 priority（缺失/非法按 0 处理，排序时垫底） */
    private static int modulePriority(JsonNode module) {
        if (module == null) {
            return 0;
        }
        JsonNode priority = module.get("priority");
        return priority != null && priority.isNumber() ? priority.asInt() : 0;
    }

    /**
     * passwordEnabled 派生：优先读已派生的布尔（getConfigs 入参经
     * maskLovePasswords 脱敏，passwordHash 恒置空，但 passwordEnabled
     * 已按原始哈希正确派生）；回退按 passwordHash 非空判定
     * （兼容直接传入未脱敏原始 spec 的调用方）。
     */
    private static boolean hasModulePassword(JsonNode module) {
        JsonNode passwordEnabled = module.get("passwordEnabled");
        JsonNode hash = module.get("passwordHash");
        return (passwordEnabled != null && passwordEnabled.isBoolean()
                && passwordEnabled.asBoolean())
                || (hash != null && !hash.isNull() && !hash.asText().isBlank());
    }

    private static boolean isContentGroup(String group) {
        return "authorConfig".equals(group) || "pageConfig".equals(group)
                || "basicConfig".equals(group) || "imagesConfig".equals(group)
                || "loveConfig".equals(group);
    }

    /**
     * 节点（含嵌套对象/数组）中是否存在至少一个非空文本字段（用于判断链接配置
     * 是否值得覆盖输出）。递归检查：ObjectNode 遍历子值、ArrayNode 遍历元素，
     * 仅对标量节点取文本，避免对 ObjectNode 调 asText() 抛 JsonNodeException。
     */
    private static boolean hasNonBlankText(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isValueNode()) {
            return !node.asText().isBlank();
        }
        for (JsonNode child : node) {
            if (hasNonBlankText(child)) {
                return true;
            }
        }
        return false;
    }

    private static ObjectNode withoutKeys(JsonNode node, String... keys) {
        ObjectNode copy = node.isObject()
                ? (ObjectNode) node.deepCopy()
                : JsonNodeFactory.instance.objectNode();
        for (String key : keys) {
            copy.remove(key);
        }
        return copy;
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

    private static String jsonText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Boolean jsonBool(JsonNode node) {
        return node == null || node.isNull() || !node.isBoolean() ? null : node.asBoolean();
    }
}
