package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.ialley.unihalo.scheme.FeatureConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 公开配置出口（getConfigs）白名单与脱敏规则测试。
 */
@DisplayName("公开配置出口白名单与脱敏")
class PublicConfigAssemblerTest {

    private final PublicConfigAssembler assembler = new PublicConfigAssembler();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Instant NOW = Instant.parse("2026-09-24T00:00:00Z");

    @Test
    @DisplayName("四个白名单组（themeWidget/themeTemplate/safety/integration）原样透传")
    void whitelistGroupsPassThrough() throws Exception {
        Map<String, JsonNode> settings = Map.of(
                "themeWidgetConfig", mapper.readTree("{\"a\":1}"),
                "themeTemplateConfig", mapper.readTree("{\"b\":2}"),
                "safetyConfig", mapper.readTree("{\"c\":3}"),
                "integrationConfig", mapper.readTree("{\"d\":4}"));
        var out = assembler.assemble(settings, null, NOW);
        assertThat(out.get("themeWidgetConfig").get("a").asInt()).isEqualTo(1);
        assertThat(out.get("themeTemplateConfig").get("b").asInt()).isEqualTo(2);
        assertThat(out.get("safetyConfig").get("c").asInt()).isEqualTo(3);
        assertThat(out.get("integrationConfig").get("d").asInt()).isEqualTo(4);
    }

    @Test
    @DisplayName("未登记的组与 settings 里的 featureConfig 一律不透传（白名单制）")
    void nonWhitelistGroupNeverLeaked() {
        Map<String, JsonNode> settings = Map.of(
                "secretGroup", mapper.createObjectNode().put("token", "x"),
                "featureConfig", mapper.createObjectNode().put("evil", true));
        var out = assembler.assemble(settings, null, NOW);
        assertThat(out.has("secretGroup")).isFalse();
        assertThat(out.has("featureConfig")).isFalse();
    }

    @Test
    @DisplayName("loginConfig 仅下发 client 内两个登录开关；Secret 资源名/令牌有效期/注册策略全部剔除")
    void loginConfigExposesOnlyClientSwitches() throws Exception {
        JsonNode login = mapper.readTree("""
                {"client":{"passwordLoginEnabled":true,"wechatLoginEnabled":false,"extra":"x"},
                 "wechatSecretName":"secret-name","tokenValiditySeconds":7200,"allowRegister":true}
                """);
        var out = assembler.assemble(Map.of("loginConfig", login), null, NOW);
        var client = out.get("loginConfig").get("client");
        assertThat(client.get("passwordLoginEnabled").asBoolean()).isTrue();
        assertThat(client.get("wechatLoginEnabled").asBoolean()).isFalse();
        assertThat(client.has("extra")).isFalse();
        assertThat(out.get("loginConfig").has("wechatSecretName")).isFalse();
        assertThat(out.get("loginConfig").has("tokenValiditySeconds")).isFalse();
        assertThat(out.get("loginConfig").has("allowRegister")).isFalse();
    }

    @Test
    @DisplayName("loginConfig 缺失 client 子对象 → 输出空对象（客户端按开关缺失回退）")
    void loginConfigWithoutClientYieldsEmptyObject() {
        JsonNode login = mapper.createObjectNode().put("wechatSecretName", "s");
        var out = assembler.assemble(Map.of("loginConfig", login), null, NOW);
        assertThat(out.get("loginConfig").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("loginConfig 为 null 节点 → 整组不输出")
    void nullLoginNodeIsDropped() {
        Map<String, JsonNode> settings = Map.of("loginConfig", mapper.nullNode());
        var out = assembler.assemble(settings, null, NOW);
        assertThat(out.has("loginConfig")).isFalse();
    }

    @Test
    @DisplayName("settings 为 null → 只按功能设置单例合成，不抛错")
    void nullSettingsYieldsEmptyRoot() {
        var out = assembler.assemble(null, null, NOW);
        assertThat(out.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("featureConfig 出口脱敏：love 各模块 passwordHash / password / passwordRemoved 强制剔除（防御式）")
    void featureConfigStripsLovePasswordFields() {
        FeatureConfig config = new FeatureConfig();
        FeatureConfig.Spec spec = new FeatureConfig.Spec();
        FeatureConfig.Love love = new FeatureConfig.Love();
        FeatureConfig.ModuleSwitch ourStory = new FeatureConfig.ModuleSwitch();
        ourStory.setEnabled(true);
        ourStory.setTitle("我们的故事");
        ourStory.setPasswordHash("$2a$bcrypt-hash");
        ourStory.setPassword("plain-password");
        ourStory.setPasswordRemoved(false);
        love.setOurStory(ourStory);
        FeatureConfig.ModuleSwitch diary = new FeatureConfig.ModuleSwitch();
        diary.setPasswordHash("$2a$hash");
        love.setLoveDiary(diary);
        spec.setLove(love);
        config.setSpec(spec);

        var out = assembler.assemble(Map.of(), config, NOW);
        var feature = out.get("featureConfig");
        // 无论入参是否已脱敏，密码相关字段一律不下发客户端
        for (String module : new String[] {"ourStory", "loveDiary"}) {
            var node = feature.get("love").get(module);
            assertThat(node.has("passwordHash")).isFalse();
            assertThat(node.has("password")).isFalse();
            assertThat(node.has("passwordRemoved")).isFalse();
        }
        assertThat(feature.get("love").get("ourStory").get("title").asString())
                .isEqualTo("我们的故事");
        assertThat(feature.get("love").get("ourStory").get("enabled").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("功能设置单例为 null 或 spec 为空 → 不输出 featureConfig 键")
    void nullOrSpeclessConfigYieldsNoFeatureConfigKey() {
        assertThat(assembler.assemble(Map.of(), null, NOW).has("featureConfig")).isFalse();
        assertThat(assembler.assemble(Map.of(), new FeatureConfig(), NOW).has("featureConfig"))
                .isFalse();
    }

    @Test
    @DisplayName("维护中（开始时间已过、结束时间未到）→ 输出 maintenance 键 status=active；enabled 开关本身不外发")
    void maintenanceActiveEmitsAdditiveKey() {
        var out = assembler.assemble(Map.of(), maintenanceConfig(
                true, "2026-09-01T00:00:00Z", "2099-01-01T00:00:00Z"), NOW);
        var maintenance = out.get("maintenance");
        assertThat(maintenance.get("status").asString()).isEqualTo("active");
        assertThat(maintenance.get("title").asString()).isEqualTo("站点维护中");
        assertThat(maintenance.get("notice").asString()).isEqualTo("正在升级");
        assertThat(maintenance.has("enabled")).isFalse();
    }

    @Test
    @DisplayName("开始时间在未来 → status=scheduled（维护预告）")
    void maintenanceScheduledWhenStartInFuture() {
        var out = assembler.assemble(Map.of(), maintenanceConfig(
                true, "2026-09-25T00:00:00Z", "2099-01-01T00:00:00Z"), NOW);
        assertThat(out.get("maintenance").get("status").asString()).isEqualTo("scheduled");
    }

    @Test
    @DisplayName("已到恢复时间（自动结束）或开关关闭 → maintenance 键缺失")
    void maintenanceEndedOrDisabledOmitsKey() {
        var ended = assembler.assemble(Map.of(), maintenanceConfig(
                true, "2026-09-01T00:00:00Z", "2026-09-23T00:00:00Z"), NOW);
        assertThat(ended.has("maintenance")).isFalse();
        var disabled = assembler.assemble(Map.of(), maintenanceConfig(
                false, null, null), NOW);
        assertThat(disabled.has("maintenance")).isFalse();
    }

    private FeatureConfig maintenanceConfig(boolean enabled, String start, String end) {
        FeatureConfig config = new FeatureConfig();
        FeatureConfig.Spec spec = new FeatureConfig.Spec();
        FeatureConfig.Maintenance maintenance = new FeatureConfig.Maintenance();
        maintenance.setEnabled(enabled);
        maintenance.setTitle("站点维护中");
        maintenance.setNotice("正在升级");
        maintenance.setStartTime(start);
        maintenance.setEndTime(end);
        spec.setMaintenance(maintenance);
        config.setSpec(spec);
        return config;
    }
}
