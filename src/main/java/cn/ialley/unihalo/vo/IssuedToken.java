package cn.ialley.unihalo.vo;

import java.time.Instant;
import java.util.Set;

/**
 * 签发的访问令牌（Halo 原生 PAT 形态）。
 *
 * <p>{@code token} 为 {@code pat_} + RS256 JWT，可被 Halo 全局过滤链直接识别，
 * 等价于用户在「个人中心 → 个人令牌」中手动创建的令牌。客户端按
 * {@code Authorization: Bearer <token>} 携带即可访问 Halo 原生 API 与本插件接口。</p>
 *
 * @param token     带 {@code pat_} 前缀的令牌串
 * @param patName   对应 PersonalAccessToken 扩展的 metadata.name（用于吊销/续期）
 * @param username  所属 Halo 用户
 * @param roles     本次实际授予的角色（已做防提权裁剪）
 * @param expiresAt 过期时间，null 表示不过期（不推荐）
 * @author 小莫唐尼
 */
public record IssuedToken(
        String token,
        String patName,
        String username,
        Set<String> roles,
        Instant expiresAt
) {
}
