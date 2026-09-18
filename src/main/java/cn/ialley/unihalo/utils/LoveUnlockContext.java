package cn.ialley.unihalo.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * 「本次渲染已解锁哪些模块」的 Reactor Context 载体。
 *
 * <p>SSR 页面不能在 URL 上长期带 {@code ?token=}，因此锁定页渲染页内解锁表单；解锁成功后
 * 由前端 JS 带 {@value #HEADER} 头重新请求当前文档，服务端校验后把解锁状态写入 Reactor
 * Context，Finder 据此返回数据 —— 只有一套模板，锁语义仍由服务端裁决。渲染结果依请求头
 * 而变，调用方须置 {@code ModelConst.NO_CACHE} 避免中间缓存污染。</p>
 *
 * <p>用 Reactor Context 而非改 Finder 签名：Finder 方法是已冻结的主题集成契约。</p>
 *
 * @author 小莫唐尼
 */
public final class LoveUnlockContext {

    /** 携带模块解锁 token 的请求头（仅同源 fetch 使用，不参与导航） */
    public static final String HEADER = "X-UniHalo-Love-Token";

    /** Reactor Context key：模块 key → 已解锁（恒为 {@code true}） */
    private static final String KEY = "uh-love.unlocked-modules";

    private LoveUnlockContext() {
    }

    /**
     * 读取本次请求的解锁覆盖表；没有则给空 Map。
     */
    @SuppressWarnings("unchecked")
    public static Mono<Map<String, Boolean>> overrides() {
        return Mono.deferContextual(context -> {
            Object value = context.getOrDefault(KEY, Map.of());
            return Mono.just(value instanceof Map ? (Map<String, Boolean>) value : Map.of());
        });
    }

    /**
     * 把解锁集合写入下游（Finder）可见的 Context。
     *
     * @param mono    待执行的下游链
     * @param modules 已通过 token 校验的模块 key；空集合时原样返回，零开销
     */
    public static <T> Mono<T> withUnlocked(Mono<T> mono, Set<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return mono;
        }
        Map<String, Boolean> overrides = new HashMap<>(modules.size());
        modules.forEach(module -> overrides.put(module, Boolean.TRUE));
        return mono.contextWrite(context -> context.put(KEY, overrides));
    }
}
