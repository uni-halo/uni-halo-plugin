package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.Banner;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 轮播图条目（Banner）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class BannerFinalizerReconciler extends AbstractFinalizerReconciler<Banner> {

    public BannerFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<Banner> schemeType() {
        return Banner.class;
    }

    @Override
    protected Banner newExtension() {
        return new Banner();
    }
}
