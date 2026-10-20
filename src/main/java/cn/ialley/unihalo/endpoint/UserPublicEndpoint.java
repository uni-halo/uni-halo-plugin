package cn.ialley.unihalo.endpoint;

import static org.springframework.web.reactive.function.server.ServerResponse.notFound;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.vo.PublicUserProfileVo;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 用户公开资料接口（app 端，匿名可访问）。
 *
 * <p>路径前缀：{@code /apis/api.unihalo.ialley.cn/v1alpha1/users}。
 * 公开接口已被 role-template-anonymous.yaml 的 api.unihalo.ialley.cn
 * 全资源规则覆盖，无需额外 RBAC。
 *
 * <p>仅返回白名单字段（displayName/avatar/bio/registeredAt），
 * 不下发 email/phone 等敏感信息；用户不存在或被禁用返回 404。
 *
 * @author 小莫唐尼
 */
@Component
public class UserPublicEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    public UserPublicEndpoint(ReactiveExtensionClient client) {
        this.client = client;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.USER_PROFILE_API_BASE_PATH + "/{username}/profile", this::getProfile)
                .build();
    }

    /**
     * 用户公开资料（用户不存在或被禁用按 404 处理）。
     */
    private Mono<ServerResponse> getProfile(ServerRequest request) {
        var username = request.pathVariable("username");
        return client.fetch(User.class, username)
                .filter(user -> !Boolean.TRUE.equals(user.getSpec().getDisabled()))
                .map(this::toProfileVo)
                .flatMap(vo -> ok().bodyValue(vo))
                .switchIfEmpty(notFound().build());
    }

    private PublicUserProfileVo toProfileVo(User user) {
        var spec = user.getSpec();
        return new PublicUserProfileVo(
                user.getMetadata().getName(),
                spec.getDisplayName(),
                spec.getAvatar(),
                spec.getBio(),
                spec.getRegisteredAt());
    }
}
