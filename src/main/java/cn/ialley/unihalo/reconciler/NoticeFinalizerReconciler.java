package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.Notice;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 公告（Notice）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class NoticeFinalizerReconciler extends AbstractFinalizerReconciler<Notice> {

    public NoticeFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<Notice> schemeType() {
        return Notice.class;
    }

    @Override
    protected Notice newExtension() {
        return new Notice();
    }
}
