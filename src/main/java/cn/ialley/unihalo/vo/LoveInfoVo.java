package cn.ialley.unihalo.vo;

import cn.ialley.unihalo.scheme.LoveInfo;
import lombok.Data;

/**
 * 恋爱信息公开视图（app 端专用接口返回体，只含展示字段，不带 metadata）。
 *
 * @author 小莫唐尼
 */
@Data
public class LoveInfoVo {

    /** 纪念日标题（留空由客户端回落默认文案） */
    private String loveDateTitle;

    /** 恋爱纪念日（yyyy-MM-dd），客户端据此计算恋爱天数 */
    private String loveDate;

    /** 男生昵称 */
    private String boyNickname;

    /** 男生头像 */
    private String boyAvatar;

    /** 女生昵称 */
    private String girlNickname;

    /** 女生头像 */
    private String girlAvatar;

    public static LoveInfoVo from(LoveInfo info) {
        LoveInfoVo vo = new LoveInfoVo();
        LoveInfo.LoveInfoSpec spec = info == null ? null : info.getSpec();
        if (spec != null) {
            vo.setLoveDateTitle(spec.getLoveDateTitle());
            vo.setLoveDate(spec.getLoveDate());
            vo.setBoyNickname(spec.getBoyNickname());
            vo.setBoyAvatar(spec.getBoyAvatar());
            vo.setGirlNickname(spec.getGirlNickname());
            vo.setGirlAvatar(spec.getGirlAvatar());
        }
        return vo;
    }
}
