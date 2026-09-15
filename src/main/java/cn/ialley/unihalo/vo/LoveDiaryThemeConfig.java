package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 恋爱日记主题页配置（已解析、已校验、不可变的运行时视图）。
 *
 * <p>来源：setting.yaml 的 {@code themeConfig.loveDiaryTheme} 组。本类只承载
 * <b>已归一化</b>的字段：非法值一律回落到默认值，保证下游（路由注册 / 模板 /
 * HeadProcessor）拿到的一定是合法值。</p>
 *
 * <h3>v1.5 起刻意「不在这里」的东西</h3>
 * <ul>
 *   <li><b>背景图</b> → 取功能设置 {@code featureConfig.spec.love.diaryPage.bgImageUrl}
 *       （与小程序端同一处配置，站长只需维护一份）；</li>
 *   <li><b>正文排版 / 代码高亮 / 灯箱 / 宽表格</b> → 不再提供开关。正文一律用主题
 *       自己的排版根类 {@code .prose} 渲染（见设计文档 §12），组件级行为固定。</li>
 * </ul>
 *
 * <p>路由相关字段保留<b>原始配置文本</b>（{@code rawRoute*}），解析为完整路径由
 * {@link LoveRoutePlan} 负责 —— 因为解析需要异步读取 Halo 已占用的 permalink，
 * 属两件事，不混在一个 VO 里。</p>
 *
 * @author 小莫唐尼
 */
@Data
public class LoveDiaryThemeConfig {

    /** 总开关。false 时插件不注册任何前台路由、不注入任何资源（默认值）。 */
    private boolean enabled;

    /** 原始配置：首页路径（以 / 开头 = 绝对路径；留空 = 不注册） */
    private String rawRouteHome;

    /** 原始配置：故事路径（不以 / 开头 = 相对首页的子段；留空 = 不注册） */
    private String rawRouteStories;

    /** 原始配置：相册路径（同上） */
    private String rawRouteAlbums;

    /** 原始配置：清单路径（同上） */
    private String rawRouteDaily;

    /** 页面外壳：{@code auto}（跟随主题布局）| {@code standalone}（独立页面） */
    private String layoutMode;

    /** 强调色（hex，已校验） */
    private String primaryColor;

    /** 首页是否显示各模块最新 3 条 */
    private boolean showOverview;

    /**
     * 关闭态：设置缺失、读取异常、或 {@code enabled=false} 时统一使用。
     */
    public static LoveDiaryThemeConfig disabled() {
        LoveDiaryThemeConfig config = new LoveDiaryThemeConfig();
        config.setEnabled(false);
        config.setRawRouteHome("/love");
        config.setRawRouteStories("stories");
        config.setRawRouteAlbums("albums");
        config.setRawRouteDaily("daily");
        config.setLayoutMode("auto");
        config.setPrimaryColor("#f83856");
        config.setShowOverview(true);
        return config;
    }
}
