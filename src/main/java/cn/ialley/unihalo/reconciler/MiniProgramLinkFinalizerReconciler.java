package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.MiniProgramLink;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 小程序链接（MiniProgramLink）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class MiniProgramLinkFinalizerReconciler
        extends AbstractFinalizerReconciler<MiniProgramLink> {

    public MiniProgramLinkFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<MiniProgramLink> schemeType() {
        return MiniProgramLink.class;
    }

    @Override
    protected MiniProgramLink newExtension() {
        return new MiniProgramLink();
    }
}
