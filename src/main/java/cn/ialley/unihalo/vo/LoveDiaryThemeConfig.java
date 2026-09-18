package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 恋爱日记主题页配置（已解析、已校验、不可变的运行时视图）。
 *
 * 来源：setting.yaml 的 {@code themeConfig.loveDiaryTheme} 组。只承载已归一化的字段，
 * 非法值一律回落默认值，保证下游拿到的一定是合法值。v1.5 起刻意不在此处：背景图取
 * {@code featureConfig.spec.love.diaryPage.bgImageUrl}（站长只维护一份）；正文排版/
 * 高亮/灯箱不提供开关，一律用主题 {@code .prose} 渲染。路由字段保留原始配置文本，
 * 解析为完整路径由 {@link LoveRoutePlan} 负责（解析需异步读取 permalink，不混在一个 VO 里）。
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
