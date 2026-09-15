package cn.ialley.unihalo.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * 「本次渲染已解锁哪些模块」的 Reactor Context 载体。
 *
 * <h3>为什么需要它</h3>
 * <p>SSR 页面没法像小程序那样在 URL 上长期带 {@code ?token=}（会被收藏、进 referrer、进日志），
 * 所以锁定页服务端渲染的是<b>页内解锁表单</b>，一个业务字段都不下发。</p>
 *
 * <p>但解锁成功后如果让前端 JS 「自己按接口数据再拼一遍 HTML」，就等于把四页模板
 * 在 JS 里重写一遍 —— 两套 markup 必然漂移，且每加一个字段都要改两处。
 * 这里改用<b>一次带凭证的文档请求</b>：</p>
 *
 * <pre>
 *   ① 用户在页内表单提交密码 → POST /love-modules/unlock → {token}
 *   ② JS：fetch(当前 URL, { headers: { 'X-UniHalo-Love-Token': token } })
 *   ③ 服务端：校验 token → 把「该模块已解锁」写进 Reactor Context → Finder 据此不返回空列表
 *   ④ JS：把返回 HTML 里的内容区替换进当前页（不刷新、不跳转）
 * </pre>
 *
 * <p>于是<b>只有一套模板</b>，锁语义仍然由服务端裁决（前端删掉表单也没用）；
 * 并且因为渲染结果依请求头而变，{@code LoveDiaryRouter} 会同时置
 * {@code ModelConst.NO_CACHE}，避免中间缓存把「解锁版」缓存给所有人。</p>
 *
 * <h3>为什么用 Reactor Context 而不是改 Finder 签名</h3>
 * <p>Finder 的方法是<b>主题集成契约</b>（已冻结）。把解锁上下文放进 Reactor Context，
 * 可以让 Finder 的签名、返回类型一字不改，锁定判定仍然全部发生在 Finder 内部。</p>
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
