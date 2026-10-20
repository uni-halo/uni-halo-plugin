package cn.ialley.unihalo.vo;

import java.time.Instant;

/**
 * 用户公开资料（公开端点白名单视图，仅含前台可展示字段）。
 *
 * @param username     Halo 用户名（metadata.name）
 * @param displayName  显示名称
 * @param avatar       头像地址
 * @param bio          个人简介
 * @param registeredAt 注册时间
 * @author 小莫唐尼
 */
public record PublicUserProfileVo(
        String username,
        String displayName,
        String avatar,
        String bio,
        Instant registeredAt
) {
}
