package cn.ialley.unihalo.vo;

import java.time.Instant;

/**
 * 令牌探活结果（App 端启动 / 回前台时调用 {@code GET auth/token-check}）。
 *
 * 客户端判定规则：HTTP 200 = 本体有效，继续使用；401 = 已失效
 * （过期 / 被新设备互踢 / 已吊销），清本地态回登录页。因此本 VO
 * 只在有效时返回，{@code valid} 恒为 true，保留该字段是给客户端
 * 一个不必解包即可断言的显式标记。
 *
 * @param valid     恒为 true（无效时走统一错误出口，不构造本 VO）
 * @param username  所属 Halo 用户
 * @param patName   对应 PersonalAccessToken 扩展的 metadata.name；
 *                  非令牌登录（浏览器会话）时为 null
 * @param expiresAt 过期时间，null 表示不过期；客户端可据此做预过期提醒
 * @author 小莫唐尼
 */
public record TokenCheckVo(
        boolean valid,
        String username,
        String patName,
        Instant expiresAt
) {
}
