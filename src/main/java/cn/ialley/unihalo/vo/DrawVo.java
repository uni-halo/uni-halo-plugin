package cn.ialley.unihalo.vo;

import cn.ialley.unihalo.scheme.Draw;
import lombok.Data;
import run.halo.app.extension.MetadataOperator;

/**
 * 绘画作品响应视图：业务数据（metadata/spec）原样输出，
 * owner 由服务端读取时实时解析 User 组装（昵称/头像始终最新，语义同官方评论 OwnerInfo）。
 *
 * @author 小莫唐尼
 */
@Data
public class DrawVo {

    private MetadataOperator metadata;

    private Draw.DrawSpec spec;

    /**
     * 作者信息（用户不存在时回退：昵称=用户名、头像空）
     */
    private Draw.DrawOwner owner;

    public static DrawVo from(Draw draw, Draw.DrawOwner owner) {
        DrawVo vo = new DrawVo();
        vo.setMetadata(draw.getMetadata());
        vo.setSpec(draw.getSpec());
        vo.setOwner(owner);
        return vo;
    }
}
