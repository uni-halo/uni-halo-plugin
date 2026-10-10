package cn.ialley.unihalo.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.ReasonPayload;
import run.halo.app.notification.UserIdentity;

/**
 * UniHalo 通知发送助手：封装「自动订阅 + 发事件」两步，供注册欢迎与设密确认复用。
 *
 * <p>订阅幂等由 Halo {@code NotificationCenter.subscribe} 保证（同一 subscriber + interestReason
 * 重复订阅返回既有 Subscription），无需自行查重。事件经 {@code NotificationReasonEmitter.emit}
 * 创建 Reason，通知中心按已声明的 NotificationTemplate 渲染为站内信（Notification），
 * recipient 即订阅者本人。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHelper {

    /** 事件类型：注册欢迎（ReasonType 由 extensions/notification-reason-types.yaml 声明，名称须与 YAML 一致）。 */
    public static final String REASON_USER_REGISTERED = "uni-halo-user-registered";
    /** 事件类型：首次设置密码成功。 */
    public static final String REASON_PASSWORD_SET = "uni-halo-password-set";
    /** 事件类型：微信绑定成功。 */
    public static final String REASON_WECHAT_BOUND = "uni-halo-wechat-bound";
    /** 事件类型：解除微信绑定。 */
    public static final String REASON_WECHAT_UNBOUND = "uni-halo-wechat-unbound";

    /** 绑定方式：小程序内一键绑定。 */
    public static final String BIND_WAY_APP = "微信小程序一键绑定";
    /** 绑定方式：UC 扫码绑定。 */
    public static final String BIND_WAY_SCAN = "扫码绑定";
    /** 解绑操作方：用户本人。 */
    public static final String OPERATOR_SELF = "本人操作";
    /** 解绑操作方：站点管理员（Console 用户详情代解绑）。 */
    public static final String OPERATOR_ADMIN = "站点管理员";

    private final NotificationReasonEmitter reasonEmitter;
    private final NotificationCenter notificationCenter;

    /**
     * 注册欢迎通知：attributes 仅含 username/displayName/registeredAt/email。
     *
     * <p>不携带任何密码信息：密码不出现在通知属性或正文中（避免明文密码持久化
     * 到站内通知），密码登录由用户在 App 内自行设置密码后启用。邮箱为空时模板不出现邮箱行。
     */
    public Mono<Void> emitUserRegistered(String username, String displayName, String email) {
        var attributes = new HashMap<String, Object>();
        attributes.put("username", username);
        attributes.put("displayName", displayName);
        if (email != null && !email.isBlank()) {
            attributes.put("email", email);
        }
        attributes.put("registeredAt", now());
        return subscribeOnce(username, REASON_USER_REGISTERED)
                .then(emit(username, REASON_USER_REGISTERED, attributes));
    }

    /** 首次设密确认通知：attributes 含 username/setAt。 */
    public Mono<Void> emitPasswordSet(String username) {
        var attributes = new HashMap<String, Object>();
        attributes.put("username", username);
        attributes.put("setAt", now());
        return subscribeOnce(username, REASON_PASSWORD_SET)
                .then(emit(username, REASON_PASSWORD_SET, attributes));
    }

    /**
     * 微信绑定成功通知：attributes 含 username/boundAt/bindWay。
     *
     * <p>绑定是账号入口的变更，必须让用户可感知：否则微信被他人误绑、
     * 或本人操作后无从核对。
     */
    public Mono<Void> emitWechatBound(String username, String bindWay) {
        var attributes = new HashMap<String, Object>();
        attributes.put("username", username);
        attributes.put("boundAt", now());
        attributes.put("bindWay", bindWay);
        return subscribeOnce(username, REASON_WECHAT_BOUND)
                .then(emit(username, REASON_WECHAT_BOUND, attributes));
    }

    /**
     * 解除微信绑定通知：attributes 含 username/unboundAt/operator。
     *
     * <p>管理员代解绑尤其需要通知：用户会突然无法微信登录，不告知将无从排查。
     *
     * @param operator 操作方文案，取 {@link #OPERATOR_SELF} 或 {@link #OPERATOR_ADMIN}
     */
    public Mono<Void> emitWechatUnbound(String username, String operator) {
        var attributes = new HashMap<String, Object>();
        attributes.put("username", username);
        attributes.put("unboundAt", now());
        attributes.put("operator", operator);
        return subscribeOnce(username, REASON_WECHAT_UNBOUND)
                .then(emit(username, REASON_WECHAT_UNBOUND, attributes));
    }

    /** 为用户订阅指定事件类型（expression 过滤仅接收本人事件）。 */
    private Mono<Subscription> subscribeOnce(String username, String reasonType) {
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(username);
        var interestReason = new Subscription.InterestReason();
        interestReason.setReasonType(reasonType);
        interestReason.setExpression("props.username == '%s'".formatted(username));
        return notificationCenter.subscribe(subscriber, interestReason)
                // 订阅失败只记日志不阻断注册/设密主流程：通知是增强体验，不是业务依赖
                .onErrorResume(e -> {
                    log.warn("【UniHalo】为用户 {} 订阅通知事件 {} 失败", username, reasonType, e);
                    return Mono.empty();
                });
    }

    /** 发送事件（创建 Reason，触发通知中心按模板渲染下发）。 */
    private Mono<Void> emit(String username, String reasonType,
            Map<String, Object> attributes) {
        return reasonEmitter.emit(reasonType, payloadBuilder -> payloadBuilder
                .subject(userSubject(username))
                // Reason.spec.author 为必填字段，缺失会被 Extension 校验拒绝（SchemaViolationException）
                .author(UserIdentity.of(username))
                .attributes(attributes))
                .onErrorResume(e -> {
                    log.warn("【UniHalo】发送通知事件 {}（用户 {}）失败", reasonType, username, e);
                    return Mono.empty();
                });
    }

    /** 事件主体指向该 User 扩展资源（Halo User 为核心无组资源，apiVersion = v1alpha1）。 */
    private static Reason.Subject userSubject(String username) {
        var subject = new Reason.Subject();
        subject.setApiVersion("v1alpha1");
        subject.setKind("User");
        subject.setName(username);
        subject.setTitle(username);
        return subject;
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
