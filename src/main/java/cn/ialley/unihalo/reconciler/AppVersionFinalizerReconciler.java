package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.AppVersion;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 应用版本（AppVersion）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class AppVersionFinalizerReconciler extends AbstractFinalizerReconciler<AppVersion> {

    public AppVersionFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<AppVersion> schemeType() {
        return AppVersion.class;
    }

    @Override
    protected AppVersion newExtension() {
        return new AppVersion();
    }
}
