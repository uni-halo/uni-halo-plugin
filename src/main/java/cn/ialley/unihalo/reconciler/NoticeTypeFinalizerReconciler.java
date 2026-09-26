package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.NoticeType;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 公告类型（NoticeType）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除；不影响已关联公告。
 *
 * @author 小莫唐尼
 */
@Component
public class NoticeTypeFinalizerReconciler extends AbstractFinalizerReconciler<NoticeType> {

    public NoticeTypeFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<NoticeType> schemeType() {
        return NoticeType.class;
    }

    @Override
    protected NoticeType newExtension() {
        return new NoticeType();
    }
}
