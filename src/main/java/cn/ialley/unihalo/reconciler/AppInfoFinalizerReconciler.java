package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.AppInfo;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 应用信息（AppInfo）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class AppInfoFinalizerReconciler extends AbstractFinalizerReconciler<AppInfo> {

    public AppInfoFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<AppInfo> schemeType() {
        return AppInfo.class;
    }

    @Override
    protected AppInfo newExtension() {
        return new AppInfo();
    }
}
