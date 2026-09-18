package cn.ialley.unihalo.vo;

import java.util.List;

import lombok.Data;

/**
 * 恋爱日记页配置视图（已解析、已脱敏；模板只读）。
 *
 * 字段来源：{@link #loveDateTitle}/{@link #loveDate}/恋人昵称与头像 ←
 * {@code FeatureConfig.Love.loveInfo}；{@link #modules} ← 三个模块入口（已按
 * {@code priority} 降序、已剔除无路由者）；{@link #bgImageUrl} ← 功能设置
 * {@code featureConfig.spec.love.diaryPage.bgImageUrl}（与小程序端同一处配置）。
 *
 * @author 小莫唐尼
 */
@Data
public class LoveConfigVo {

    /** 恋爱功能总开关（模块入口列表是否为空同样反映这一点） */
    private boolean enabled;

    /** 纪念日标题（如「这是我们一起走过的」，留空由模板回落默认文案） */
    private String loveDateTitle;

    /** 恋爱纪念日（yyyy-MM-dd），模板据此 + JS 计算恋爱天数 */
    private String loveDate;

    /**
     * 恋爱天数（纪念日当天 = 第 1 天，服务端计算）。
     *
     * 给倒计时一个 SSR 初值：无 JS / 首屏时也能看到「第 N 天」而不是占位符；
     * JS 加载后接管，每秒刷新天/时/分/秒。日期非法或无纪念日时为 {@code null}。
     */
    private Integer loveDays;

    /** 男生昵称 */
    private String boyNickname;

    /** 男生头像 */
    private String boyAvatar;

    /** 女生昵称 */
    private String girlNickname;

    /** 女生头像 */
    private String girlAvatar;

    /** 恋爱页背景图（来自功能设置，可为空 = 用主题背景） */
    private String bgImageUrl;

    /** 模块入口（已按 priority 降序，且只含已注册路由的模块） */
    private List<LoveModuleVo> modules = List.of();
}
