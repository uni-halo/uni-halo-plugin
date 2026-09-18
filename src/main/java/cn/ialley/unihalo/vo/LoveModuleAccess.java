package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 恋爱模块访问状态（供锁定页渲染页内解锁表单）。
 *
 * 安全红线：只承载「锁状态 + 表单文案」，绝不含任何业务字段（标题/日期/地点/图片/
 * 条数），锁定页 HTML 里没有任何可被 F12 挖出的内容。{@link #moduleTitle} 取自功能设置
 * 的模块名称（本就经公开 {@code getConfigs} 下发），仅用于表单标题文案。
 *
 * 注意：必须用 Lombok {@code @Data} 而非 record —— Thymeleaf + SpEL 只认 JavaBean
 * getter，record 的 {@code locked()} 取不到值，模板里 {@code ${access.locked}} 会炸。
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
