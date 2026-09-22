package cn.ialley.unihalo.utils;

import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.UserConnection;

/**
 * 微信绑定关系（{@code UserConnection}）的取值规则与重复告警收口。
 *
 * <p>背景（P1-5 绑定非原子）：绑定是「查重 → 创建」两步，中间既无锁也无唯一约束，
 * 同一微信身份并发绑定仍可能落两条记录；历史数据里还有 openid → unionid 回落
 * 造成的重复。而历史代码一律 {@code Flux#next()}，取到哪条取决于 Halo list 的
 * 返回顺序（未定义），表现为「同一个微信随机登进两个账号」「解绑一条后仍能登录」。
 *
 * <p>本类把取值规则固定为「按创建时间取最新」，让结果可预测；命中重复时记 warn
 * 并列出资源名，便于事后核对。<b>只做取值与告警，不删除任何数据</b>——存量收敛
 * 交给解绑（解绑会删除该账号名下的全部记录）。
 *
 * @author 小莫唐尼
 */
@Slf4j
public final class UserConnectionSupport {

    /** 创建时间倒序（最新优先）；同毫秒按资源名升序，保证取值稳定可复现。 */
    private static final Comparator<UserConnection> NEWEST_FIRST = Comparator
            .<UserConnection, Instant>comparing(UserConnectionSupport::createdAt).reversed()
            .thenComparing(UserConnectionSupport::nameOf);

    private UserConnectionSupport() {
    }

    /**
     * 取最新的一条绑定关系；无记录时返回 {@code empty}。
     *
     * 命中多条时先 warn（含资源名清单）再返回最新的一条：重复本身说明出现了
     * 并发或历史遗留，需要站长可见，但不能用删除动作自动处理。
     */
    public static Mono<UserConnection> newest(Flux<UserConnection> connections) {
        return connections.collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        return Mono.<UserConnection>empty();
                    }
                    if (list.size() > 1) {
                        log.warn("【UniHalo】检测到重复的微信绑定关系（{} 条）：{}，"
                                        + "已按创建时间取最新的一条；如确认异常请联系站长核对",
                                list.size(),
                                list.stream()
                                        .map(UserConnectionSupport::nameOf)
                                        .collect(Collectors.joining(", ")));
                        list.sort(NEWEST_FIRST);
                    }
                    return Mono.just(list.get(0));
                });
    }

    private static Instant createdAt(UserConnection connection) {
        var metadata = connection.getMetadata();
        return metadata == null || metadata.getCreationTimestamp() == null
                ? Instant.EPOCH
                : metadata.getCreationTimestamp();
    }

    private static String nameOf(UserConnection connection) {
        var metadata = connection.getMetadata();
        return metadata == null || metadata.getName() == null ? "" : metadata.getName();
    }
}
