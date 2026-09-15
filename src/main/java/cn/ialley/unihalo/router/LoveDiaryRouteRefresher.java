package cn.ialley.unihalo.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.PluginConfigUpdatedEvent;

import cn.ialley.unihalo.utils.LoveRouteResolver;

/**
 * 恋爱日记路由快照刷新器。
 *
 * <p>路由快照（{@link LoveRouteResolver#snapshot()}）是同步谓词唯一能读的东西，
 * 因此它必须有明确的刷新时机：</p>
 * <ol>
 *   <li><b>插件启动</b>（本类 {@code afterPropertiesSet}）—— 首帧就有路由；</li>
 *   <li><b>设置在控制台保存</b>（{@link PluginConfigUpdatedEvent}）—— 改路径后立即生效，
 *       不需要重载插件；</li>
 *   <li><b>页面渲染路径</b>（{@code resolvePlan()} 的 TTL 过期）；
 *       <b>谓词未命中且快照过期</b>时也会异步触发一次，让下一个请求命中。</li>
 * </ol>
 *
 * <p>三者为「或」关系：任一条生效即可；即使事件不触发，也会在 30s TTL 内自愈。</p>
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveDiaryRouteRefresher
        implements InitializingBean, ApplicationListener<PluginConfigUpdatedEvent> {

    private final LoveRouteResolver routeResolver;

    @Override
    public void afterPropertiesSet() {
        refresh("插件启动");
    }

    @Override
    public void onApplicationEvent(PluginConfigUpdatedEvent event) {
        refresh("设置已保存");
    }

    /**
     * 异步刷新（fire-and-forget）：失败只记日志，绝不影响请求链路。
     */
    private void refresh(String reason) {
        routeResolver.refresh()
                .subscribe(plan -> log.info("恋爱日记主题页路由快照已刷新（{}）：{}",
                                reason, plan.getPaths()),
                        error -> log.warn("刷新恋爱日记主题页路由快照失败（{}）：{}",
                                reason, error.getMessage()));
    }
}
