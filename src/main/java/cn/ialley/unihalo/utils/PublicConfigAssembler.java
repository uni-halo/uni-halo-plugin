package cn.ialley.unihalo.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.ialley.unihalo.scheme.FeatureConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 公开配置输出合成器（getConfigs 出口，只读）。
 *
 * 输出 = setting.yaml 活组白名单 + 功能设置单例 spec（脱敏）+ 服务端计算态：
 * {@code featureConfig} 为 {@link FeatureConfig} spec 整体下发（密码相关字段
 * {@code passwordHash/password/passwordRemoved} 绝不出服务端）；{@code safetyConfig /
 * themeWidgetConfig / themeTemplateConfig} 白名单透传；{@code integrationConfig} 组装输出
 * （AI助手的 chatPrompt 追加应用信息/博主资料/社交信息，其余内容透传）；
 * {@code loginConfig} 仅输出登录方式开关，Secret 资源名、令牌有效期与注册策略不外发；
 * {@code maintenance} 为 additive 顶层键，按 spec.maintenance 时间窗口计算，
 * 仅 scheduled/active 时输出（键缺失 = 未维护）。其余设置组一律不透传——白名单制，新增组须同步此处。
 *
 * @author 小莫唐尼
 */
public class PublicConfigAssembler {

    /** 设置组白名单：原样透传（setting.yaml 新增组须同步维护；integrationConfig 走组装） */
    private static final String[] PASSTHROUGH_GROUPS = {
        "themeWidgetConfig", "themeTemplateConfig", "safetyConfig"};

    /**
     * 插件 Spring 上下文未注册 Jackson 3 ObjectMapper bean，故内部自行创建
     * （与 EmailService / FeatureConfigServiceImpl 同套路）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 合成公开配置输出（维护状态按当前时刻判定）。
     *
     * @param settings 当前插件设置（ReactiveSettingFetcher.getSettingValues 结果，可为空）
     * @param config   功能设置单例（可能为 null 或 spec 为空，此时 featureConfig 键不输出）
     */
    public ObjectNode assemble(Map<String, JsonNode> settings, FeatureConfig config) {
        return assemble(settings, config, Instant.now());
    }

    /**
     * 合成公开配置输出（维护状态判定时钟可注入，便于测试覆盖
     * scheduled/active/到点自动结束等分支）。
     *
     * @param settings 当前插件设置（可为空）
     * @param config   功能设置单例（可能为 null 或 spec 为空）
     * @param now      维护状态判定的当前时刻（见 {@link MaintenanceResolver}）
     */
    public ObjectNode assemble(Map<String, JsonNode> settings, FeatureConfig config, Instant now) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        if (settings != null) {
            JsonNode login = settings.get("loginConfig");
            if (login != null && !login.isNull()) {
                root.set("loginConfig", sanitizeLogin(login));
            }
            for (String group : PASSTHROUGH_GROUPS) {
                JsonNode node = settings.get(group);
                if (node != null && node.isObject()) {
                    root.set(group, node);
                }
            }
            // integrationConfig：AI助手组装输出（chatPrompt 追加站点信息），其余内容透传
            JsonNode integration = settings.get("integrationConfig");
            if (integration != null && integration.isObject()) {
                root.set("integrationConfig", assembleIntegration(integration, config));
            }
        }
        JsonNode spec = sanitizeFeatureSpec(toSpecTree(config));
        if (spec != null && spec.isObject()) {
            root.set("featureConfig", spec);
        }
        // 维护模式（additive 顶层键）：状态由时间窗口按 now 计算，
        // 仅 scheduled/active 输出；enabled=false 或已到点自动结束 → 键缺失
        if (spec != null && spec.isObject()) {
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
        }
        return root;
    }

    private JsonNode toSpecTree(FeatureConfig config) {
        if (config == null || config.getSpec() == null) {
            return null;
        }
        return objectMapper.valueToTree(config.getSpec());
    }

    /**
     * 功能设置 spec 出口脱敏（防御式）：无论入参是否已经
     * {@code featureConfigService.get()} 脱敏，恋爱模块各节点的
     * passwordHash / password / passwordRemoved 一律剔除，
     * 密码相关字段绝不下发客户端。
     */
    private static JsonNode sanitizeFeatureSpec(JsonNode spec) {
        if (spec == null || !spec.isObject()) {
            return spec;
        }
        ObjectNode out = (ObjectNode) spec.deepCopy();
        JsonNode love = out.get("love");
        if (love != null && love.isObject()) {
            for (JsonNode module : love) {
                if (module != null && module.isObject()) {
                    ObjectNode moduleOut = (ObjectNode) module;
                    moduleOut.remove("passwordHash");
                    moduleOut.remove("password");
                    moduleOut.remove("passwordRemoved");
                }
            }
        }
        return out;
    }

    /**
     * integrationConfig 组装：pluginConfig.aiAssistant 存在且站点信息非空时，
     * 把应用信息/博主资料/社交信息追加到 chatPrompt 之后；aiAssistant 缺失或站点信息
     * 为空时原样返回（未配置过 = 维持现状），pluginConfig 下其余插件节点保持透传。
     */
    private static JsonNode assembleIntegration(JsonNode node, FeatureConfig config) {
        JsonNode pluginConfig = node.get("pluginConfig");
        JsonNode assistant = pluginConfig != null && pluginConfig.isObject()
                ? pluginConfig.get("aiAssistant") : null;
        if (assistant == null || !assistant.isObject()) {
            return node;
        }
        String siteInfo = buildProfileText(config);
        if (siteInfo.isEmpty()) {
            return node;
        }
        ObjectNode out = (ObjectNode) node.deepCopy();
        ObjectNode assistantOut = (ObjectNode) ((ObjectNode) out.get("pluginConfig")).get("aiAssistant");
        String chat = jsonText(assistant.get("chatPrompt"));
        assistantOut.put("chatPrompt", chat == null || chat.isBlank() ? siteInfo : chat + "\n\n" + siteInfo);
        return out;
    }

    /**
     * 站点信息提示词文本（FeatureConfig.spec.profile 序列化）：应用信息/博主资料/
     * 社交信息逐行拼装，社交项过滤 visible && content 后按 priority 降序输出；
     * 全部字段缺失时返回空串（调用方不追加，避免空段落）。
     */
    private static String buildProfileText(FeatureConfig config) {
        if (config == null || config.getSpec() == null || config.getSpec().getProfile() == null) {
            return "";
        }
        FeatureConfig.Profile profile = config.getSpec().getProfile();
        List<String> lines = new ArrayList<>();
        if (profile.getAppInfo() != null && notBlank(profile.getAppInfo().getName())) {
            lines.add("应用名称：" + profile.getAppInfo().getName().trim());
        }
        FeatureConfig.Blogger blogger = profile.getBlogger();
        if (blogger != null) {
            if (notBlank(blogger.getNickname())) {
                lines.add("博主昵称：" + blogger.getNickname().trim());
            }
            if (notBlank(blogger.getDescription())) {
                lines.add("博主简介：" + blogger.getDescription().trim());
            }
            if (notBlank(blogger.getIntro())) {
                lines.add("博主介绍：" + htmlToText(blogger.getIntro()));
            }
            if (notBlank(blogger.getWebsite())) {
                lines.add("博主主页：" + blogger.getWebsite().trim());
            }
            if (notBlank(blogger.getEmail())) {
                lines.add("博主邮箱：" + blogger.getEmail().trim());
            }
        }
        FeatureConfig.Social social = profile.getSocial();
        if (social != null && social.getItems() != null) {
            String accounts = social.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getVisible()) && notBlank(item.getContent()))
                    .sorted(Comparator.comparing(FeatureConfig.SocialItem::getPriority,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(item -> notBlank(item.getName())
                            ? item.getName().trim() + " " + item.getContent().trim()
                            : item.getContent().trim())
                    .collect(Collectors.joining("；"));
            if (!accounts.isEmpty()) {
                lines.add("社交账号：" + accounts);
            }
        }
        if (lines.isEmpty()) {
            return "";
        }
        return "【站点信息（系统自动附加，请勿修改）】\n" + String.join("\n", lines);
    }

    /** 富文本 HTML 转纯文本（剥标签 + 合并空白），限长 300 字符，供提示词拼接 */
    private static String htmlToText(String html) {
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return text.length() > 300 ? text.substring(0, 300) : text;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 登录配置公开输出脱敏（getConfigs loginConfig 组）：
     * 仅输出 client 子对象内的 passwordLoginEnabled / wechatLoginEnabled
     * 两个开关（客户端经 loginConfig.client.passwordLoginEnabled 读取），
     * 供小程序端决定登录页展示哪些入口；wechatSecretName（Secret 资源名）、
     * 令牌有效期与注册策略（Halo 系统设置的「允许注册」「默认角色」）
     * 属于服务端决策，全部不下发。
     */
    private static JsonNode sanitizeLogin(JsonNode node) {
        JsonNode login = node.get("client");
        if (login == null || !login.isObject()) {
            return JsonNodeFactory.instance.objectNode();
        }
        ObjectNode client = JsonNodeFactory.instance.objectNode();
        pick(login, client, "passwordLoginEnabled", "wechatLoginEnabled");
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.set("client", client);
        return out;
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
        return node == null || node.isNull() ? null : node.asString();
    }

    private static Boolean jsonBool(JsonNode node) {
        return node == null || node.isNull() || !node.isBoolean() ? null : node.asBoolean();
    }
}
