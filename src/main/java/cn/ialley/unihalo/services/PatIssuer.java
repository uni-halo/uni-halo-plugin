package cn.ialley.unihalo.services;

import java.time.Instant;
import java.util.Set;

import cn.ialley.unihalo.vo.IssuedToken;
import reactor.core.publisher.Mono;

/**
 * PAT 签发器（移动端登录的 token 出口）。
 *
 * <p>把「创建 PersonalAccessToken 扩展 + 用 Halo 私钥签 JWT」两步封装在单一接口后，
 * 便于未来 Halo 变更 PAT 的 JWT claims 契约时整体切换到反射调用官方
 * {@code PatService}（方案 B），而不影响登录流程其余部分。</p>
 *
 * <p>实现必须 fail closed：自检未通过时 {@link #issue} 直接报错，
 * 绝不签发一个 Halo 无法识别的令牌。</p>
 *
 * @author 小莫唐尼
 */
public interface PatIssuer {

    /**
     * 为指定用户签发一个 PAT。
     *
     * @param username  目标用户（Halo User 的 metadata.name）
     * @param roles     本次授予的角色名集合，必须已由调用方做过防提权裁剪
     * @param expiresAt 过期时间；null 表示不过期（**不推荐**，过期只由 JWT exp 保证）
     * @return 签发的令牌
     */
    Mono<IssuedToken> issue(String username, Set<String> roles, Instant expiresAt);

    /**
     * 吊销指定 PAT（置 revoked 标记，不影响 JWT 本身，靠扩展校验拦截）。
     *
     * @param patName  PAT 扩展名
     * @param username 归属用户（防止越权吊销他人令牌）
     */
    Mono<Void> revoke(String patName, String username);

    /**
     * 签发能力是否可用（启动自检结果）。不可用时应关闭对外登录能力。
     */
    boolean available();
}
