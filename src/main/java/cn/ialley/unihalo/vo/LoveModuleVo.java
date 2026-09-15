package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 恋爱模块入口（首页三卡片）。
 *
 * <h3>与 app 端数据契约的唯一有意差异</h3>
 * <p>app 端 {@code ModuleSwitch.path} 是<b>小程序页面路由</b>；主题端链接必须是
 * <b>Halo 路径</b>，故本模型用 {@link #url} 承载已注册的 Halo 路由，
 * 并且<b>物理上不提供 {@code path} 字段</b> —— 防止模板作者误用小程序路由。</p>
 *
 * <p>路由未注册（留空 / 冲突 / 未启用）的模块<b>不会被下发</b>，因此模板里的
 * {@code modules} 列表恒为「点得开的入口」，不需要额外判空兜底。</p>
 *
 * @author 小莫唐尼
 */
@Data
public class LoveModuleVo {

    /** 模块 key：{@code ourStory} / {@code lovePhoto} / {@code loveDaily} */
    private String key;

    /** 入口标题（站长可配，留空回落内置名称） */
    private String title;

    /** 入口副标题 */
    private String subTitle;

    /** 标题颜色（hex8 #rrggbbaa，缺失回落设计 token） */
    private String titleColor;

    /** 副标题颜色 */
    private String subTitleColor;

    /** 图标方块背景色 */
    private String iconBgColor;

    /**
     * 彩色图标字体类名（{@code uhlove-icon-*}）。
     * 优先用站长配置的 {@code iconPrefix + icon}，缺失时按 key 映射内置图标。
     */
    private String iconClass;

    /**
     * 主题端链接（Halo 路由，来自 {@link LoveRoutePlan#getPaths()}）。
     * 模板链接一律取它，<b>不得硬编码</b>。
     */
    private String url;

    /** 排序（越大越前） */
    private Integer priority;

    /**
     * 该模块当前是否处于锁定态（决定入口是否显示锁图标）。
     * 注意：这只影响入口图标，模块页自身仍会在服务端做一次锁判定。
     */
    private boolean locked;
}
