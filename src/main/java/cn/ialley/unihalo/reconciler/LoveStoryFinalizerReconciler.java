package cn.ialley.unihalo.reconciler;

import cn.ialley.unihalo.scheme.LoveStory;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;

/**
 * 恋爱故事（LoveStory）的删除收尾：等待删除过渡期后移除 finalizer，交由框架完成物理删除。
 *
 * @author 小莫唐尼
 */
@Component
public class LoveStoryFinalizerReconciler extends AbstractFinalizerReconciler<LoveStory> {

    public LoveStoryFinalizerReconciler(ExtensionClient client) {
        super(client);
    }

    @Override
    protected Class<LoveStory> schemeType() {
        return LoveStory.class;
    }

    @Override
    protected LoveStory newExtension() {
        return new LoveStory();
    }
}
