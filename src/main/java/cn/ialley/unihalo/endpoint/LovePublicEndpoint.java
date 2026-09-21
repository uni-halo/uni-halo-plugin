package cn.ialley.unihalo.endpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.captcha.CaptchaScope;
import cn.ialley.unihalo.captcha.CaptchaService;
import cn.ialley.unihalo.captcha.CaptchaValidationException;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.LoveAlbum;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.services.LoveAlbumService;
import cn.ialley.unihalo.services.LoveDailyItemService;
import cn.ialley.unihalo.services.LoveInfoService;
import cn.ialley.unihalo.services.LoveStoryService;
import cn.ialley.unihalo.utils.AlbumTokenManager;
import cn.ialley.unihalo.utils.LoveModuleTokenManager;
import cn.ialley.unihalo.vo.LoveAlbumVo;
import cn.ialley.unihalo.vo.LoveInfoVo;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

/**
 * 恋爱功能公开接口（小程序端，匿名可访问）。
 *
 * 恋爱配置统一经公开 getConfigs 的 loveConfig 组下发，不再提供独立的 /love-config
 * 聚合接口；本端点仅保留恋爱数据接口，相册接口按锁定状态脱敏。模块入口密码：三个入口
 * 可分别设密码，设置后数据接口要求携带 {@code ?token=}（经 {@code POST
 * /love-modules/unlock} 校验密码换取，30 分钟有效），未带或无效返回 401；
 * 未设密码的模块不校验（老客户端无感）。相册级密码保持外层模块锁 + 内层相册锁。
 *
 * @author 小莫唐尼
 */
@Component
public class LovePublicEndpoint implements CustomEndpoint {

    private final FeatureConfigService featureConfigService;
    private final LoveAlbumService loveAlbumService;
    private final LoveDailyItemService loveDailyItemService;
    private final LoveStoryService loveStoryService;
    private final LoveInfoService loveInfoService;
    private final AlbumTokenManager albumTokenManager;
    private final LoveModuleTokenManager loveModuleTokenManager;
    private final CaptchaService captchaService;

    public LovePublicEndpoint(FeatureConfigService featureConfigService,
            LoveAlbumService loveAlbumService,
            LoveDailyItemService loveDailyItemService,
            LoveStoryService loveStoryService,
            LoveInfoService loveInfoService,
            AlbumTokenManager albumTokenManager,
            LoveModuleTokenManager loveModuleTokenManager,
            CaptchaService captchaService) {
        this.featureConfigService = featureConfigService;
        this.loveAlbumService = loveAlbumService;
        this.loveDailyItemService = loveDailyItemService;
        this.loveStoryService = loveStoryService;
        this.loveInfoService = loveInfoService;
        this.albumTokenManager = albumTokenManager;
        this.loveModuleTokenManager = loveModuleTokenManager;
        this.captchaService = captchaService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.END_POINT_API_BASE_PATH + "/love-stories", this::listStories)
                .GET(Constants.END_POINT_API_BASE_PATH + "/love-albums", this::listAlbums)
                .GET(Constants.END_POINT_API_BASE_PATH + "/love-albums/{name}", this::getAlbum)
                .POST(Constants.END_POINT_API_BASE_PATH + "/love-albums/{name}/unlock",
                        this::unlockAlbum)
                .POST(Constants.END_POINT_API_BASE_PATH + "/love-modules/unlock",
                        this::unlockLoveModule)
                .GET(Constants.END_POINT_API_BASE_PATH + "/love-daily-items", this::listDailyItems)
                .GET(Constants.END_POINT_API_BASE_PATH + "/love-info", this::getLoveInfo)
                .build();
    }

    /**
     * 恋爱信息（纪念日 + 恋人信息，匿名公开；未配置时字段为 null）。
     */
    private Mono<ServerResponse> getLoveInfo(ServerRequest request) {
        return loveInfoService.fetchOrDefault()
                .map(LoveInfoVo::from)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    /**
     * 故事列表（多条目；恋爱故事入口设置密码时要求携带模块 token）。
     */
    private Mono<ServerResponse> listStories(ServerRequest request) {
        int page = queryPage(request);
        int size = querySize(request);
        return requireModuleAccess(request, "ourStory",
                loveStoryService.listPublic(page, size)
                        .flatMap(body -> ServerResponse.ok().bodyValue(body)));
    }

    /**
     * 相册列表：加密相册 locked=true 且不含 photos；恋爱相册入口设置密码时
     * 要求携带模块 token（外层模块锁 + 内层相册锁）。
     */
    private Mono<ServerResponse> listAlbums(ServerRequest request) {
        int page = queryPage(request);
        int size = querySize(request);
        return requireModuleAccess(request, "lovePhoto",
                loveAlbumService.listPublic(page, size)
                        .map(result -> {
                            List<LoveAlbumVo> items = result.getItems().stream()
                                    .map(album -> LoveAlbumVo.from(album, isLocked(album)))
                                    .toList();
                            return new ListResult<>(result.getPage(), result.getSize(),
                                    result.getTotal(), items);
                        })
                        .flatMap(body -> ServerResponse.ok().bodyValue(body)));
    }

    /**
     * 相册详情：加密相册需携带解锁 token 才返回 photos；恋爱相册入口设置密码时
     * 同样要求模块 token。
     *
     * 两个 token 不能共用一个参数名：外层模块锁读 {@code ?token=}，
     * 内层相册锁过去也读 {@code ?token=}，于是「模块锁 + 相册锁」同时打开时
     * 客户端只能带一个，照片永远出不来。故相册锁改为优先读 {@code ?albumToken=}，
     * 读不到再回落 {@code ?token=}（老客户端行为不变）。
     */
    private Mono<ServerResponse> getAlbum(ServerRequest request) {
        String name = request.pathVariable("name");
        String token = request.queryParam("albumToken")
                .or(() -> request.queryParam("token"))
                .orElse("");
        return requireModuleAccess(request, "lovePhoto",
                loveAlbumService.getByName(name)
                        // 公开详情：删除中对象视为不存在
                        .filter(album -> album.getMetadata() == null
                                || album.getMetadata().getDeletionTimestamp() == null)
                        .map(album -> LoveAlbumVo.from(album,
                                isLocked(album) && !albumTokenManager.verify(name, token)))
                        .flatMap(body -> ServerResponse.ok().bodyValue(body))
                        .switchIfEmpty(Mono.defer(() -> ServerResponse.notFound().build())));
    }

    /**
     * 密码解锁：校验通过后签发 HMAC 签名 token 并返回相册照片。
     * 验证码校验在密码校验之前，失败不暴露密码正确性；相册入口模块锁先行校验。
     */
    private Mono<ServerResponse> unlockAlbum(ServerRequest request) {
        String name = request.pathVariable("name");
        return requireModuleAccess(request, "lovePhoto",
                captchaService.requireValid(request, CaptchaScope.LOVE_ALBUM_UNLOCK)
                        .then(request.bodyToMono(UnlockRequest.class)
                                .flatMap(body -> loveAlbumService
                                        .verifyPassword(name, body.getPassword()))
                                .flatMap(ok -> {
                                    if (!ok) {
                                        return ServerResponse.badRequest()
                                                .bodyValue(Map.of("message", "密码不正确"));
                                    }
                                    String token = albumTokenManager.issue(name);
                                    return loveAlbumService.getByName(name)
                                            .map(album -> {
                                                Map<String, Object> result = new LinkedHashMap<>();
                                                result.put("token", token);
                                                result.put("photos", album.getSpec() != null
                                                        && album.getSpec().getPhotos() != null
                                                                ? album.getSpec().getPhotos()
                                                                : List.of());
                                                return result;
                                            })
                                            .flatMap(body -> ServerResponse.ok().bodyValue(body));
                                }))
                        .onErrorResume(CaptchaValidationException.class, this::captchaForbidden));
    }

    /**
     * 恋爱模块入口解锁：校验模块存在且密码匹配后签发 HMAC 签名 token
     * （scope = 模块名，30 分钟有效；未设置密码的模块一律拒绝，不暴露是否已设置）。
     * 验证码校验在密码校验之前，失败不暴露密码正确性（scope=loveModuleUnlock，
     * 覆盖恋爱日记/恋爱故事/恋爱相册入口/恋爱清单等模块入口解锁）。
     */
    private Mono<ServerResponse> unlockLoveModule(ServerRequest request) {
        return captchaService.requireValid(request, CaptchaScope.LOVE_MODULE_UNLOCK)
                .then(request.bodyToMono(LoveModuleUnlockRequest.class)
                        .flatMap(body -> featureConfigService
                                .verifyLoveModulePassword(body.getModule(), body.getPassword())
                                .flatMap(ok -> {
                                    if (!ok) {
                                        return ServerResponse.badRequest()
                                                .bodyValue(Map.of("message", "密码不正确"));
                                    }
                                    return ServerResponse.ok()
                                            .bodyValue(Map.of("token",
                                                    loveModuleTokenManager.issue(body.getModule())));
                                })))
                .onErrorResume(CaptchaValidationException.class, this::captchaForbidden);
    }

    /**
     * 恋爱模块入口访问控制：模块设置密码（锁定）时校验 {@code ?token=}，
     * 未带或无效返回 401 {@code {reason: "locked"}}；
     * 未锁定直接放行（老客户端无感）。
     */
    private Mono<ServerResponse> requireModuleAccess(ServerRequest request, String module,
            Mono<ServerResponse> body) {
        return featureConfigService.isLoveModuleLocked(module)
                .flatMap(locked -> {
                    if (!locked) {
                        return body;
                    }
                    String token = request.queryParam("token").orElse("");
                    if (loveModuleTokenManager.verify(module, token)) {
                        return body;
                    }
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                            .bodyValue(Map.of("reason", "locked"));
                });
    }

    /**
     * 验证码校验失败：403 + 附新验证码（前端即时刷新重试）。
     */
    private Mono<ServerResponse> captchaForbidden(CaptchaValidationException e) {
        return captchaService.generate()
                .flatMap(captcha -> ServerResponse.status(HttpStatus.FORBIDDEN)
                        .bodyValue(Map.of("message", e.getMessage(), "captcha", captcha)));
    }

    /**
     * 恋爱清单公开列表（只读，支持状态筛选与分页）。
     *
     * ⚠️ 与故事/相册一致，必须走 {@code requireModuleAccess}：恋爱清单入口
     * 同样可以设密码（见类注释），漏判会让加密清单被匿名接口直接读走，
     * 而主题端 Finder 却已按锁拦截 —— 两端语义必须一致（设计报告 §7.2）。
     */
    private Mono<ServerResponse> listDailyItems(ServerRequest request) {
        int page = queryPage(request);
        int size = querySize(request);
        String status = request.queryParam("status").orElse("").trim();
        return requireModuleAccess(request, "loveDaily",
                loveDailyItemService.listPublic(status, page, size)
                        .flatMap(body -> ServerResponse.ok().bodyValue(body)));
    }

    private static boolean isLocked(LoveAlbum album) {
        return album.getSpec() != null
                && Boolean.TRUE.equals(album.getSpec().getPasswordEnabled());
    }

    private static int queryPage(ServerRequest request) {
        return request.queryParam("page").map(Integer::parseInt).orElse(1);
    }

    private static int querySize(ServerRequest request) {
        return request.queryParam("size").map(Integer::parseInt).orElse(20);
    }

    /**
     * 解锁请求体
     */
    @Data
    public static class UnlockRequest {
        private String password;
    }

    /**
     * 恋爱模块入口解锁请求体（module = loveDiary/ourStory/lovePhoto/loveDaily）
     */
    @Data
    public static class LoveModuleUnlockRequest {
        private String module;
        private String password;
    }
}
