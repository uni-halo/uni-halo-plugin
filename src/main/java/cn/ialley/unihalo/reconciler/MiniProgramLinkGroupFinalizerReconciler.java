package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.MiniProgramLinkGroup;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 小程序链接分组（MiniProgramLinkGroup）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除；不影响组内链接。
 *
 * @author 小莫唐尼
 */
@Component
public class MiniProgramLinkGroupFinalizerReconciler
        extends AbstractFinalizerReconciler<MiniProgramLinkGroup> {

    public MiniProgramLinkGroupFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<MiniProgramLinkGroup> schemeType() {
        return MiniProgramLinkGroup.class;
    }

    @Override
    protected MiniProgramLinkGroup newExtension() {
        return new MiniProgramLinkGroup();
    }
}
