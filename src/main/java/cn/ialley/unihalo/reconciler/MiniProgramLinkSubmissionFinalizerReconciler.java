package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.MiniProgramLinkSubmission;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 小程序链接投稿（MiniProgramLinkSubmission）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class MiniProgramLinkSubmissionFinalizerReconciler
        extends AbstractFinalizerReconciler<MiniProgramLinkSubmission> {

    public MiniProgramLinkSubmissionFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<MiniProgramLinkSubmission> schemeType() {
        return MiniProgramLinkSubmission.class;
    }

    @Override
    protected MiniProgramLinkSubmission newExtension() {
        return new MiniProgramLinkSubmission();
    }
}
