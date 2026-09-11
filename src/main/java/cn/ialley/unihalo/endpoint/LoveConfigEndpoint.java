package cn.ialley.unihalo.endpoint;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.GeneralConfig;
import cn.ialley.unihalo.scheme.LoveConfig;
import cn.ialley.unihalo.services.GeneralConfigService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.Metadata;

/**
 * 恋爱配置接口（控制台，需登录）。
 *
 * <p>2026-09-11 起数据源迁移至 {@code GeneralConfig.spec.love.loveInfo}
 * （原「恋爱管理-恋爱配置」内容迁入「通用配置-恋爱设置-恋爱信息」），
 * 本接口保留以 LoveConfig 兼容形态读写同一数据源，供旧前端/其他调用方使用。</p>
 *
 * @author 小莫唐尼
 */
@Component
public class LoveConfigEndpoint implements CustomEndpoint {

    private final GeneralConfigService generalConfigService;

    public LoveConfigEndpoint(GeneralConfigService generalConfigService) {
        this.generalConfigService = generalConfigService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.LOVE_CONFIG_API_BASE_PATH, this::getLoveConfig)
                .PUT(Constants.LOVE_CONFIG_API_BASE_PATH, this::saveLoveConfig)
                .build();
    }

    private Mono<ServerResponse> getLoveConfig(ServerRequest request) {
        return generalConfigService.get()
                .map(LoveConfigEndpoint::toLoveConfig)
                .flatMap(config -> ServerResponse.ok().bodyValue(config));
    }

    private Mono<ServerResponse> saveLoveConfig(ServerRequest request) {
        return request.bodyToMono(LoveConfig.class)
                .flatMap(body -> generalConfigService.get()
                        .flatMap(config -> {
                            applyLoveInfo(config, body);
                            return generalConfigService.save(config);
                        }))
                .map(LoveConfigEndpoint::toLoveConfig)
                .flatMap(saved -> ServerResponse.ok().bodyValue(saved))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest()
                                .bodyValue(Map.of("message", e.getMessage())));
    }

    /**
     * GeneralConfig.spec.love.loveInfo → LoveConfig 兼容形态
     * （spec = {loveDateTitle, loveDate, loveInfo{男孩女孩昵称头像}}）。
     */
    private static LoveConfig toLoveConfig(GeneralConfig config) {
        LoveConfig out = new LoveConfig();
        Metadata metadata = new Metadata();
        metadata.setName(Constants.LOVE_CONFIG_SINGLETON_NAME);
        out.setMetadata(metadata);

        LoveConfig.LoveConfigSpec spec = new LoveConfig.LoveConfigSpec();
        GeneralConfig.LoveInfo info = config.getSpec() != null
                && config.getSpec().getLove() != null
                ? config.getSpec().getLove().getLoveInfo() : null;
        if (info != null) {
            spec.setLoveDateTitle(info.getLoveDateTitle());
            spec.setLoveDate(info.getLoveDate());
            LoveConfig.LoveInfo loveInfo = new LoveConfig.LoveInfo();
            loveInfo.setBoyNickname(info.getBoyNickname());
            loveInfo.setBoyAvatar(info.getBoyAvatar());
            loveInfo.setGirlNickname(info.getGirlNickname());
            loveInfo.setGirlAvatar(info.getGirlAvatar());
            spec.setLoveInfo(loveInfo);
        } else {
            spec.setLoveDateTitle("这是我们一起走过的");
            spec.setLoveInfo(new LoveConfig.LoveInfo());
        }
        out.setSpec(spec);
        return out;
    }

    /**
     * LoveConfig 请求体 → 写入 GeneralConfig.spec.love.loveInfo
     * （仅覆盖恋爱信息字段，不动恋爱页图片/模块开关）。
     */
    private static void applyLoveInfo(GeneralConfig config, LoveConfig body) {
        if (config.getSpec() == null) {
            config.setSpec(new GeneralConfig.Spec());
        }
        GeneralConfig.Love love = config.getSpec().getLove();
        if (love == null) {
            love = new GeneralConfig.Love();
            config.getSpec().setLove(love);
        }
        GeneralConfig.LoveInfo info = new GeneralConfig.LoveInfo();
        if (body.getSpec() != null) {
            info.setLoveDateTitle(body.getSpec().getLoveDateTitle());
            info.setLoveDate(body.getSpec().getLoveDate());
            if (body.getSpec().getLoveInfo() != null) {
                info.setBoyNickname(body.getSpec().getLoveInfo().getBoyNickname());
                info.setBoyAvatar(body.getSpec().getLoveInfo().getBoyAvatar());
                info.setGirlNickname(body.getSpec().getLoveInfo().getGirlNickname());
                info.setGirlAvatar(body.getSpec().getLoveInfo().getGirlAvatar());
            }
        }
        love.setLoveInfo(info);
    }
}
