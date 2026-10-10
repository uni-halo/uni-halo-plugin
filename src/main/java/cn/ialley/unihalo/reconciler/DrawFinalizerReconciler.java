package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.Draw;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 绘画作品（Draw）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class DrawFinalizerReconciler extends AbstractFinalizerReconciler<Draw> {

    public DrawFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<Draw> schemeType() {
        return Draw.class;
    }

    @Override
    protected Draw newExtension() {
        return new Draw();
    }
}
