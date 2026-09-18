package cn.ialley.unihalo.vo;

import java.util.Map;

import lombok.Data;

/**
 * 恋爱日记首页模型（{@code myFinder.loveHome()} 的返回值）。
 *
 * 【锁定态（恋爱日记入口本身设了密码）】
 * {@link #access} 为 locked，且 {@link #config} 与 {@link #overview}
 * 保持 null —— 模板检测到 {@code access.locked} 后只渲染解锁表单，
 * 页面 HTML 中不含昵称/头像/纪念日/摘要等任何业务字段。
 * {@link #routes} 仍会下发（它是路径而非内容，且模板渲染导航需要）。
 *
 * @author 小莫唐尼
 */
@Data
public class LovePageVo {

    /** 恋爱日记模块访问状态（决定渲染内容区还是解锁表单） */
    private LoveModuleAccess access;

    /** 页面配置（锁定态为 null） */
    private LoveConfigVo config;

    /** 各模块最新 3 条摘要（锁定态为 null；{@code showOverview=false} 时三个列表为空） */
    private LoveOverviewVo overview;

    /**
     * 实际注册成功的路由：{@code home/stories/albums/daily} → 路径。
     * 模板内一切站内链接都从这里取，不硬编码（站长改了路径也能对上）。
     */
    private Map<String, String> routes = Map.of();
}
