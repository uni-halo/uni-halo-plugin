package cn.ialley.unihalo.endpoint;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.vo.WechatBindingVo;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.UserConnection;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 微信绑定关系管理（控制台，需登录）。
 *
 * <p>Halo 原生只提供「用户中心 → 账号绑定」这种<strong>自己解绑自己</strong>的入口
 * （{@code uc.api.auth.halo.run / user-connections/disconnect}，resourceNames 固定为 {@code -}），
 * Console 侧没有全量绑定关系的管理界面。这个端点补上管理员视角：查看某个用户的微信绑定、
 * 以及解绑（只删 UserConnection，<strong>不删 Halo 用户</strong>，用户可以重新绑定或改用密码登录）。</p>
 *
 * <p>只在用户详情页以选项卡形式使用，不单独建列表页 —— 用户本体的禁用、删号、改角色
 * 一律走 Halo 原生用户管理，避免维护两套。</p>
 *
 * @author 小莫唐尼
 */
@Component
@RequiredArgsConstructor
public class WechatUserEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.WECHAT_USER_API_BASE_PATH + "/{username}/binding",
                        this::getBinding)
                .DELETE(Constants.WECHAT_USER_API_BASE_PATH + "/{username}/binding",
                        this::unbind)
                .build();
    }

    private Mono<ServerResponse> getBinding(ServerRequest request) {
        var username = request.pathVariable("username");
        return findConnection(username)
                .map(connection -> new WechatBindingVo(
                        username,
                        true,
                        connection.getSpec().getProviderUserId(),
                        connection.getSpec().getUpdatedAt()))
                .defaultIfEmpty(WechatBindingVo.unbound(username))
                .flatMap(vo -> ServerResponse.ok().bodyValue(vo));
    }

    private Mono<ServerResponse> unbind(ServerRequest request) {
        var username = request.pathVariable("username");
        return findConnection(username)
                .flatMap(client::delete)
                .then(Mono.defer(WechatUserEndpoint::ok))
                .switchIfEmpty(Mono.defer(WechatUserEndpoint::ok));
    }

    private static Mono<ServerResponse> ok() {
        return ServerResponse.ok().bodyValue(Map.of("success", true));
    }

    private Mono<UserConnection> findConnection(String username) {
        return client.list(UserConnection.class,
                        connection -> connection.getSpec() != null
                                && Constants.WECHAT_REGISTRATION_ID.equals(
                                        connection.getSpec().getRegistrationId())
                                && username.equals(connection.getSpec().getUsername()),
                        null)
                .next();
    }
}
