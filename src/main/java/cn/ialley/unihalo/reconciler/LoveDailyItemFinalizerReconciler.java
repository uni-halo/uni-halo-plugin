package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.LoveDailyItem;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 恋爱日常（LoveDailyItem）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class LoveDailyItemFinalizerReconciler
        extends AbstractFinalizerReconciler<LoveDailyItem> {

    public LoveDailyItemFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<LoveDailyItem> schemeType() {
        return LoveDailyItem.class;
    }

    @Override
    protected LoveDailyItem newExtension() {
        return new LoveDailyItem();
    }
}
