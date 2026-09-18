package cn.ialley.unihalo.vo;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户资料（{@code GET /auth/profile} 的响应）。
 *
 * 刻意不含令牌：早期实现是复用 {@link LoginResult}，导致每次调用都新签一枚 PAT ——
 * 既堆积令牌，又让旧令牌继续有效（等于无法通过重新登录收口）。会话恢复只需读用户与权限，
 * 令牌由登录接口一次性下发，客户端自行缓存到过期。
 *
 * @param user        登录用户摘要（不含任何敏感字段）
 * @param roles       当前令牌实际持有的角色
 * @param permissions 由角色模板递归展开的 RBAC 规则，供前端做显隐控制
 * @author 小莫唐尼
 */
public record ProfileVo(
        LoginResult.LoginUser user,
        Set<String> roles,
        List<LoginResult.PermissionRule> permissions
) {
}
