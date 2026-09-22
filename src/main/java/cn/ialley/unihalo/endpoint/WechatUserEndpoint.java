package cn.ialley.unihalo.endpoint;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.utils.NotificationHelper;
import cn.ialley.unihalo.utils.UserConnectionSupport;
import cn.ialley.unihalo.vo.WechatBindingVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.UserConnection;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 微信绑定关系管理（控制台，需登录）。
 *
 * Halo 原生只提供用户「自己解绑自己」的入口，Console 侧没有绑定关系管理界面；
 * 本端点补上管理员视角：查看某个用户的微信绑定、以及解绑（只删 UserConnection，
 * 不删 Halo 用户，用户可重新绑定或改用密码登录）。仅在用户详情页选项卡中使用，
 * 不单独建列表页 —— 用户本体管理一律走 Halo 原生能力。
 *
 * <p>管理员代解绑必须<b>通知到被解绑用户</b>：用户会突然无法微信登录，
 * 不告知将无从排查。解绑同时删除该账号的<b>全部</b>绑定记录（含历史重复数据），
 * 只删一条会出现「已解绑但仍能微信登录」。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUserEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    private final NotificationHelper notificationHelper;

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

    /**
     * 查询绑定状态：默认只返回脱敏标识。
     *
     * <p>{@code ?reveal=true} 才下发完整 openid —— 排查「用户微信登不上」需要拿它
     * 去微信侧核对，但这是敏感操作，必须<b>显式请求且留痕</b>：每次查看记一条
     * 审计日志（谁看、看谁的），避免变成「谁都能顺手看一眼」。
     *
     * <p>刻意用 query param 而不是新开子资源端点：新端点要同步 role-template
     * 与已建角色快照，成本高且易漏；同一路径下 RBAC 粒度不变，显式参数 + 审计
     * 已经达到「默认最小披露」的目的。
     */
    private Mono<ServerResponse> getBinding(ServerRequest request) {
        var username = request.pathVariable("username");
        var reveal = Boolean.parseBoolean(request.queryParam("reveal").orElse("false"));
        return findConnection(username)
                .map(connection -> {
                    var raw = connection.getSpec().getProviderUserId();
                    if (reveal) {
                        log.info("【UniHalo】管理员查看完整微信标识：targetUser={}", username);
                        return WechatBindingVo.revealed(username, raw,
                                connection.getSpec().getUpdatedAt());
                    }
                    return WechatBindingVo.masked(username, raw,
                            connection.getSpec().getUpdatedAt());
                })
                .defaultIfEmpty(WechatBindingVo.unbound(username))
                .flatMap(vo -> ServerResponse.ok().bodyValue(vo));
    }

    private Mono<ServerResponse> unbind(ServerRequest request) {
        var username = request.pathVariable("username");
        return findConnections(username)
                .collectList()
                .flatMap(connections -> {
                    if (connections.isEmpty()) {
                        // 幂等：本来就未绑定，不产生无意义的通知
                        return ok();
                    }
                    return Flux.fromIterable(connections)
                            .flatMap(client::delete)
                            .then(Mono.defer(() -> notificationHelper.emitWechatUnbound(
                                    username, NotificationHelper.OPERATOR_ADMIN)))
                            .then(Mono.defer(WechatUserEndpoint::ok));
                });
    }

    private static Mono<ServerResponse> ok() {
        return ServerResponse.ok().bodyValue(Map.of("success", true));
    }

    private Mono<UserConnection> findConnection(String username) {
        // 同一账号若存在历史重复记录，取最新的那条（并告警），避免取到哪条全看
        // Halo list 的返回顺序 —— 否则管理员看到的绑定状态可能不是实际生效的那条
        return UserConnectionSupport.newest(findConnections(username));
    }

    private Flux<UserConnection> findConnections(String username) {
        return client.list(UserConnection.class,
                        connection -> connection.getSpec() != null
                                && Constants.WECHAT_REGISTRATION_ID.equals(
                                        connection.getSpec().getRegistrationId())
                                && username.equals(connection.getSpec().getUsername()),
                        null);
    }
}
