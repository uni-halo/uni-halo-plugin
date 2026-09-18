package cn.ialley.unihalo.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.PluginConfigUpdatedEvent;

import cn.ialley.unihalo.utils.LoveRouteResolver;

/**
 * 恋爱日记路由快照刷新器。快照（{@link LoveRouteResolver#snapshot()}）是同步谓词
 * 唯一能读的东西，刷新时机三者互为兜底：插件启动（首帧即有路由）、控制台保存设置
 * （{@link PluginConfigUpdatedEvent}，改路径立即生效）、谓词未命中且快照 TTL 过期时
 * 异步刷新（30s 内自愈）。
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
