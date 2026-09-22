package cn.ialley.unihalo.process;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import cn.ialley.unihalo.utils.SettingGroupResolver;

/**
 * 主题组件（widget）通用注入处理器：向主题页 {@code <head>} 注入各主题组件的
 * 内联配置块与静态脚本（经插件 ReverseProxy 暴露，带版本号防缓存）。
 *
 * <p>组件注册表 {@link #WIDGETS} 声明每个组件的配置键、window 全局键与脚本路径；
 * 新增组件只需在注册表加一条 + setting.yaml {@code themeWidgetConfig} 组加对应
 * 子组 + packages 下新增前端包，本类无需改动。每个组件独立 fail closed
 * （未启用/配置异常均不注入该组件，且不影响其他组件）。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
public class ThemeWidgetHeadProcessor implements TemplateHeadProcessor {

    private static final String DOMAIN = "themeWidgetConfig";

    /**
     * 主题组件注册表：configKey = themeWidgetConfig 下的子组名，
     * globalKey = 注入给前端的 window 全局变量名，jsPath = 静态脚本路径
     * （相对 /plugins/uni-halo/assets/static/，?version=%s 由注入时填充）。
     */
    private record WidgetSpec(String configKey, String globalKey, String jsPath) {
    }

    private static final WidgetSpec FLOAT_PROFILE_WIDGET =
        new WidgetSpec("floatProfileWidget", "__UNI_HALO_FLOAT_PROFILE_WIDGET__",
            "widgets/float-profile-widget/float-profile-widget.js");

    private static final WidgetSpec[] WIDGETS = {FLOAT_PROFILE_WIDGET};

    private final ReactiveSettingFetcher settingFetcher;
    private final PluginWrapper pluginWrapper;

    /**
     * 插件 Spring 上下文未注册 Jackson 3 ObjectMapper bean，内部自行创建
     * （与 PublicConfigAssembler / FeatureConfigServiceImpl 同套路）。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ThemeWidgetHeadProcessor(ReactiveSettingFetcher settingFetcher,
            PluginWrapper pluginWrapper) {
        this.settingFetcher = settingFetcher;
        this.pluginWrapper = pluginWrapper;
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        return Flux.fromArray(WIDGETS)
            .flatMap(spec -> inject(context, model, spec))
            .then();
    }

    /**
     * 注入单个组件：未启用或配置异常时静默跳过（fail closed，页面零残留），
     * 不影响其他组件的注入。
     */
    private Mono<Void> inject(ITemplateContext context, IModel model, WidgetSpec spec) {
        return SettingGroupResolver.group(settingFetcher, DOMAIN, spec.configKey())
            .flatMap(node -> {
                if (!node.path("enabled").asBoolean(false)) {
                    return Mono.empty();
                }
                // 悬浮名片卡片需已配置太阳码图片，否则视为未配置不注入
                if (spec == FLOAT_PROFILE_WIDGET
                        && node.path("imageUrl").asString("").isBlank()) {
                    return Mono.empty();
                }
                try {
                    IModelFactory factory = context.getModelFactory();
                    model.add(factory.createText(componentScript(spec, buildConfig(spec, node))));
                } catch (Exception e) {
                    // 配置序列化失败按未启用处理，避免注入残缺脚本
                    log.warn("序列化主题组件 [{}] 配置失败，本次不注入：{}",
                        spec.configKey(), e.getMessage());
                }
                return Mono.empty();
            });
    }

    /**
     * 组装前端配置（缺失字段取默认值，字段显式置空时保持为空由前端不渲染）。
     * 默认值统一收在 {@link #applyDefaults}，新增组件字段时在此追加。
     */
    private ObjectNode buildConfig(WidgetSpec spec, JsonNode node) {
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.put("enabled", node.path("enabled").asBoolean(false));
        config.put("pageScope", node.path("pageScope").asString("all"));
        config.put("pagePatterns", node.path("pagePatterns").asString(""));
        config.put("position", node.path("position").asString("bottom-right"));
        config.put("offsetX", node.path("offsetX").asInt(0));
        config.put("offsetY", node.path("offsetY").asInt(0));
        config.put("name", node.path("name").asString("小程序"));
        config.put("nameSize", node.path("nameSize").asInt(14));
        config.put("nameColor", node.path("nameColor").asString("#333333"));
        config.put("description", node.path("description").asString(""));
        config.put("descSize", node.path("descSize").asInt(12));
        config.put("descColor", node.path("descColor").asString("#999999"));
        config.put("imageUrl", node.path("imageUrl").asString(""));
        config.put("cardWidth", node.path("cardWidth").asInt(100));
        config.put("dragEnabled", node.path("dragEnabled").asBoolean(true));
        config.put("defaultState", node.path("defaultState").asString("default"));
        config.put("closeEnabled", node.path("closeEnabled").asBoolean(true));
        config.put("edgeHideEnabled", node.path("edgeHideEnabled").asBoolean(true));
        config.put("edgeHideDistance", node.path("edgeHideDistance").asInt(24));
        config.put("rememberClosed", node.path("rememberClosed").asBoolean(true));
        config.put("miniProgramApply", node.path("miniProgramApply").asBoolean(false));
        return config;
    }

    private String componentScript(WidgetSpec spec, ObjectNode config) {
        String version = pluginWrapper.getDescriptor().getVersion();
        String jsUrl =
            "/plugins/uni-halo/assets/static/" + spec.jsPath() + "?version=%s".formatted(version);
        // 防名称/描述等文本含 </script> 提前闭合脚本标签
        String configJson = objectMapper.writeValueAsString(config).replace("</", "<\\/");
        return """
            <!-- uni-halo v3.x theme widget [%s] start -->
            <script>window.%s = %s;</script>
            <script defer src="%s"></script>
            <!-- uni-halo v3.x theme widget [%s] end -->
            """.formatted(spec.configKey(), spec.globalKey(), configJson, jsUrl,
            spec.configKey());
    }
}
