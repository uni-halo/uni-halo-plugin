package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.LoveAlbum;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 恋爱相册（LoveAlbum）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class LoveAlbumFinalizerReconciler extends AbstractFinalizerReconciler<LoveAlbum> {

    public LoveAlbumFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<LoveAlbum> schemeType() {
        return LoveAlbum.class;
    }

    @Override
    protected LoveAlbum newExtension() {
        return new LoveAlbum();
    }
}
