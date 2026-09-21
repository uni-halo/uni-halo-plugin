package cn.ialley.unihalo.process;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.theme.dialect.TemplateHeadProcessor;
import run.halo.app.theme.router.ModelConst;
import tools.jackson.databind.ObjectMapper;

import cn.ialley.unihalo.captcha.CaptchaScope;
import cn.ialley.unihalo.captcha.CaptchaService;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.utils.LoveDiaryConfigResolver;
import cn.ialley.unihalo.vo.LoveDiaryThemeConfig;

/**
 * 恋爱日记主题页 Head 注入处理器。
 *
 * 注入门槛（fail-closed，两条都要满足）：模板 {@code _templateId} 以
 * {@code plugin:uni-halo:love} 开头，且 {@code themeConfig.loveDiaryTheme.enabled == true}
 * 且配置读取成功 —— 关闭或配置异常时一个字节都不注入。样式/脚本走同源静态前缀而非 CDN，
 * 避免离线/内网白屏、CSP 坑与版本漂移。
 *
 * 注入内容：页面配置 → {@code window.__UNI_HALO_LOVE_DIARY__}；设计 token/正文层 CSS、
 * hljs 配色、图标字体；{@code love-diary.js}（defer，hljs 由 JS 按需加载）。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveDiaryHeadProcessor implements TemplateHeadProcessor {

    private static final String STATIC_BASE =
            Constants.PLUGIN_STATIC_PREFIX + Constants.LOVE_STATIC_DIR;

    private static final String PUBLIC_API_BASE =
            "/apis/" + Constants.PUBLIC_CUSTOM_API_GROUP_NAME;

    private final LoveDiaryConfigResolver configResolver;

    private final CaptchaService captchaService;

    private final PluginWrapper pluginWrapper;

    /** 插件 Spring 上下文未注册 Jackson ObjectMapper bean，内部自建（与其它类同套路）。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
            IElementModelStructureHandler structureHandler) {
        String templateId = templateId(context);
        if (templateId == null) {
            return Mono.empty();
        }
        return Mono.zip(configResolver.resolveEnabled(),
                        captchaService.requiredFor(CaptchaScope.LOVE_MODULE_UNLOCK)
                                .defaultIfEmpty(false)
                                .onErrorResume(e -> Mono.just(false)),
                        captchaService.requiredFor(CaptchaScope.LOVE_ALBUM_UNLOCK)
                                .defaultIfEmpty(false)
                                .onErrorResume(e -> Mono.just(false)))
                .flatMap(tuple -> {
                    try {
                        IModelFactory factory = context.getModelFactory();
                        model.add(factory.createText(
                                headFragment(tuple.getT1(), tuple.getT2(), tuple.getT3())));
                    } catch (Exception e) {
                        // 序列化失败按「不注入」处理，避免半截脚本把页面搞坏
                        log.warn("注入恋爱日记主题页资源失败，本次不注入：{}", e.getMessage());
                    }
                    return Mono.empty();
                });
    }

    /** 仅当 {@code _templateId} 命中恋爱日记前缀时返回它，否则返回 null。 */
    private static String templateId(ITemplateContext context) {
        if (!context.containsVariable(ModelConst.TEMPLATE_ID)) {
            return null;
        }
        Object value = context.getVariable(ModelConst.TEMPLATE_ID);
        if (value instanceof String id && id.startsWith(Constants.LOVE_TEMPLATE_ID_PREFIX)) {
            return id;
        }
        return null;
    }

    private String headFragment(LoveDiaryThemeConfig config, boolean moduleCaptchaRequired,
            boolean albumCaptchaRequired) {
        String version = pluginWrapper.getDescriptor().getVersion();
        String suffix = "?version=" + version;
        Map<String, Object> script = new LinkedHashMap<>();
        script.put("apiBase", PUBLIC_API_BASE);
        script.put("captchaApi", PUBLIC_API_BASE + "/captcha/generate");
        script.put("moduleUnlockApi", PUBLIC_API_BASE + "/love-modules/unlock");
        script.put("albumUnlockApiTemplate", PUBLIC_API_BASE + "/love-albums/{name}/unlock");
        script.put("loveStoriesApi", PUBLIC_API_BASE + "/love-stories");
        script.put("loveAlbumsApi", PUBLIC_API_BASE + "/love-albums");
        script.put("loveItemsApi", PUBLIC_API_BASE + "/love-daily-items");
        script.put("assetsBase", STATIC_BASE);
        script.put("hljsUrl", STATIC_BASE + "/hljs.bundle.js" + suffix);
        script.put("primaryColor", config.getPrimaryColor());
        script.put("layoutMode", config.getLayoutMode());
        script.put("moduleCaptchaRequired", moduleCaptchaRequired);
        script.put("albumCaptchaRequired", albumCaptchaRequired);

        String json = objectMapper.writeValueAsString(script).replace("</", "<\\/");

        StringBuilder html = new StringBuilder(1024);
        html.append("<!-- uni-halo love diary start -->");
        // ⚠️ 顺序有讲究：<link> 必须排在承载 :root 变量的内联 <style> 之前。
        // CSS 自定义属性在「同特异性」时按文档顺序后者胜 —— 主题色是站长配的，
        // 必须能盖掉 love-diary.css 里的默认值。
        html.append("<link rel=\"stylesheet\" href=\"").append(STATIC_BASE)
                .append("/love-diary.css").append(suffix).append("\">");
        html.append("<link rel=\"stylesheet\" href=\"").append(STATIC_BASE)
                .append("/rich-text.css").append(suffix).append("\">");
        html.append("<link rel=\"stylesheet\" href=\"").append(STATIC_BASE)
                .append("/fonts/uhlove-iconfont.css").append(suffix).append("\">");
        html.append("<link rel=\"stylesheet\" href=\"").append(STATIC_BASE)
                .append("/hljs.css").append(suffix).append("\">");
        html.append("<style>:root{--uh-love-primary:")
                .append(config.getPrimaryColor())
                .append(";--uh-love-primary-soft:")
                .append(softColor(config.getPrimaryColor()))
                .append(";}</style>");
        html.append("<script>window.__UNI_HALO_LOVE_DIARY__=").append(json).append(";</script>");
        html.append("<script defer src=\"").append(STATIC_BASE)
                .append("/love-diary.js").append(suffix).append("\"></script>");
        html.append("<!-- uni-halo love diary end -->");
        return html.toString();
    }

    /**
     * 由主色推导柔和的图标底色（{@code #rrggbb} → {@code #rrggbb1f}）。
     * 不做 {@code color-mix}，避免老浏览器拿不到值。
     */
    private static String softColor(String primary) {
        if (primary == null) {
            return "#fce7f3";
        }
        String value = primary.trim();
        if (value.length() == 7 && value.charAt(0) == '#') {
            return value + "1f";
        }
        // #rgb → #rrggbb1f；#rrggbbaa 保持原样（已自带透明度）
        if (value.length() == 4 && value.charAt(0) == '#') {
            StringBuilder expanded = new StringBuilder("#");
            for (int i = 1; i < 4; i++) {
                expanded.append(value.charAt(i)).append(value.charAt(i));
            }
            return expanded.append("1f").toString();
        }
        return value;
    }
}
