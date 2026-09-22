package cn.ialley.unihalo.vo;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 登录结果。
 *
 * {@code token} 为 {@code pat_} 前缀的 Halo 原生个人访问令牌，客户端按
 * {@code Authorization: Bearer <token>} 携带即可访问 Halo 原生 API 与本插件接口。
 * {@code permissions} 供小程序做菜单/按钮级显隐控制。
 *
 * @author 小莫唐尼
 */
public record LoginResult(
        String token,
        String tokenType,
        Instant expiresAt,
        String patName,
        LoginUser user,
        Set<String> roles,
        List<PermissionRule> permissions
) {

    /**
     * 登录用户摘要（不含任何敏感字段）。
     *
     * @param passwordSetByUser 用户是否「自主设置过密码」（读 {@code unihalo.ialley.cn/password-set-by-user}
     *                          注解）。与 Halo UC 的 {@code passwordSet}（密码哈希是否存在）不同：
     *                          插件代生成的随机/固定密码用户该值为 false，app 端据此走免旧密码的
     *                          首次设密通道（方案 B）。
     */
    public record LoginUser(String name, String displayName, String avatar, String email,
            boolean passwordSetByUser) {
    }

    /** 由角色模板递归展开的 RBAC 规则。 */
    public record PermissionRule(
            List<String> apiGroups,
            List<String> resources,
            List<String> verbs
    ) {
    }
}
