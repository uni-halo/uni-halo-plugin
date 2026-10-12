package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * 邮箱验证票据（HMAC-SHA256 无状态签名，10 分钟有效）。
 *
 * <p>用于「PAT 被 Halo email-not-set 拦截」死锁下的自建邮箱验证闭环：
 * 登录接口在 {@code emailVerified=false} 时随响应签发，客户端凭票据调用
 * {@code auth/-/send-email-verification}（发码）与 {@code auth/-/verify-email}
 * （验码写库）两个公开端点，服务端内部 {@code ReactiveExtensionClient} 直写
 * User 扩展，全程不经过 HTTP 层的 PAT 校验。
 * 有效期与验证码 TTL 对齐：Halo 的 ProfileCompletionFilter 会拦截未验证邮箱用户的
 * 全部 PAT 请求，票据在被互踢/吊销后仍独立有效，短有效期收敛这段滥用窗口
 * （过期后经 {@code auth/-/email-verify-ticket} 凭密码重新获取，不重签 PAT）。
 *
 * <p>token 格式：{@code base64(username).expiry.hex(signature)}。
 * 签名 = HMAC(secret, username + "." + expiry + "." + emailToVerify)：
 * 把「目标邮箱」绑进签名——发码时按请求邮箱重签/验签，用户中途改填其他邮箱后
 * 旧票据验签失败，防止「为 A 邮箱签发、验证时绑定到 B 邮箱」的偷换。
 *
 * <p>签名比对走 {@link MessageDigest#isEqual}（定长字节定常比较），消除
 * {@code String.equals} 的时序侧信道。每枚票据最多 5 次验证尝试，超限即作废。
 * 票据本身只能「启动」验证流程，完成绑定必须持有发往邮箱的 6 位验证码
 * （一次性、独立封顶），票据泄露不构成邮箱接管。
 *
 * <p>签名密钥来自 {@link PluginSecretProvider}（站点级随机密钥，与重置密码票据、
 * 相册/恋爱解锁 token 同源，源码不含有效密钥，杜绝离线伪造）。
 *
 * @author 小莫唐尼
 */
@Component
public class EmailVerifyTicketManager {

    public static final int TTL_MINUTES = 10;

    private static final long TTL_MILLIS = TTL_MINUTES * 60 * 1000L;

    /** 每枚票据最大验证尝试次数（含错误邮箱），超限即作废。 */
    private static final int MAX_ATTEMPTS = 5;

    private final PluginSecretProvider secretProvider;

    public EmailVerifyTicketManager(PluginSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    /** token → 已用尝试次数（内存态，重启清零；仅用于封顶爆破，不影响业务正确性）。 */
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    /**
     * 为未验证邮箱用户签发票据（登录响应）：签名不含邮箱（传 {@code null} 验签）。
     *
     * @param username 用户名（Halo User 的 metadata.name）
     */
    public String issue(String username) {
        return issueBound(username, null);
    }

    /**
     * 签发绑定目标邮箱的票据（发码成功后由服务端换取下发）：
     * 验证邮箱时须以同一邮箱验签，中途换邮箱旧票据失效。
     *
     * @param username      用户名
     * @param emailToVerify 绑定的目标邮箱
     */
    public String issueBound(String username, String emailToVerify) {
        long expiry = System.currentTimeMillis() + TTL_MILLIS;
        String usernamePart = b64(username);
        return usernamePart + "." + expiry + "." + sign(usernamePart, expiry, emailToVerify);
    }

    /**
     * 校验票据（发码/验码共用）；无效返回 {@code null}。
     *
     * @param token         客户端提交的票据
     * @param emailToVerify 发码时绑定的目标邮箱（签发登录票据时传 {@code null}）
     * @return 票据归属用户名，否则 null
     */
    public String verify(String token, String emailToVerify) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String usernamePart = parts[0];
            long expiry = Long.parseLong(parts[1]);
            String signature = parts[2];
            if (System.currentTimeMillis() >= expiry) {
                return null;
            }
            if (tooManyAttempts(token)) {
                return null;
            }
            String expected = sign(usernamePart, expiry, emailToVerify);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                recordAttempt(token);
                return null;
            }
            // 验签通过即清尝试计数（正常路径不累计）
            attempts.remove(token);
            return new String(Base64.getUrlDecoder().decode(usernamePart), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** 验证完成或签发新票据后作废旧票据（清尝试计数）。 */
    public void consume(String token) {
        if (token != null) {
            attempts.remove(token);
        }
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String usernamePart, long expiry, String emailToVerify) {
        String payload = usernamePart + "." + expiry + "." + (emailToVerify == null ? "" : emailToVerify);
        return Hmac.sha256Hex(secretProvider.requireSecret(), payload);
    }

    private boolean tooManyAttempts(String token) {
        AtomicInteger counter = attempts.get(token);
        return counter != null && counter.get() >= MAX_ATTEMPTS;
    }

    private void recordAttempt(String token) {
        attempts.computeIfAbsent(token, k -> new AtomicInteger(0)).incrementAndGet();
    }
}
