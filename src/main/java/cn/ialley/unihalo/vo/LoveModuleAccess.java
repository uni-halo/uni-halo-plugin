package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 恋爱模块访问状态（供锁定页渲染<b>页内解锁表单</b>）。
 *
 * <h3>安全约束（红线）</h3>
 * <p>本模型只承载「锁状态 + 表单文案」，<b>绝不包含任何业务字段</b>：
 * 没有标题、没有日期、没有地点、没有图片 URL、没有条数。
 * 模块锁定时 Finder 只把这个对象交给模板，页面 HTML 里因此不存在任何可被
 * F12 挖出的内容 —— 这是「页内表单而非弹窗/遮罩」方案的成立前提。</p>
 *
 * <p>{@link #moduleTitle} 取自站长在功能设置里填写的模块名称（该字段本就经公开
 * {@code getConfigs} 下发），仅用于「XX 模块已加密」这类表单标题文案，不构成内容泄露。</p>
 *
 * <p>注意：本类用 Lombok {@code @Data}（生成 {@code isLocked()}/{@code getModule()}），
 * <b>不用 record</b> —— Thymeleaf + SpEL 的属性访问只认 JavaBean getter，
 * record 的 {@code locked()} 取不到值，模板里 {@code ${access.locked}} 会直接炸。</p>
 *
 * @author 小莫唐尼
 */
@Data
public class LoveModuleAccess {

    /** 模块 key：{@code loveDiary} / {@code ourStory} / {@code lovePhoto} / {@code loveDaily} */
    private String module;

    /** 是否锁定：true = 模板渲染页内解锁表单，且不渲染任何内容区 */
    private boolean locked;

    /** 模块标题（仅表单标题文案；取站长配置的模块名，非内容） */
    private String moduleTitle;

    /** 该 scope 当前是否真的要求验证码（决定是否渲染验证码输入框） */
    private boolean captchaRequired;

    /** 验证码 scope 配置键（本场景恒为 {@code loveModuleUnlock}） */
    private String captchaScope;

    /**
     * 未锁定态：模板正常渲染内容区（此时内容由 Finder 数据方法返回）。
     */
    public static LoveModuleAccess unlocked(String module) {
        LoveModuleAccess access = new LoveModuleAccess();
        access.setModule(module);
        access.setLocked(false);
        access.setModuleTitle("");
        access.setCaptchaRequired(false);
        access.setCaptchaScope(null);
        return access;
    }

    /**
     * 锁定态：模板只渲染解锁表单。
     *
     * @param module          模块 key
     * @param moduleTitle     模块名称（表单标题文案）
     * @param captchaRequired 该 scope 是否要求验证码
     */
    public static LoveModuleAccess locked(String module, String moduleTitle,
            boolean captchaRequired) {
        LoveModuleAccess access = new LoveModuleAccess();
        access.setModule(module);
        access.setLocked(true);
        access.setModuleTitle(moduleTitle == null ? "" : moduleTitle);
        access.setCaptchaRequired(captchaRequired);
        access.setCaptchaScope(captchaRequired ? "loveModuleUnlock" : null);
        return access;
    }
}
