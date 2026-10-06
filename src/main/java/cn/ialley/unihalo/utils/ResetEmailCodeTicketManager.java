package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * 忘记密码重置票据（HMAC-SHA256 无状态签名，10 分钟有效）。
 *
 * <p>token 格式：{@code base64(username).expiry.hex(signature)}。
 * 签名 = HMAC(secret, username + "." + expiry + "." + code + "." + passwordHash)：
 * 把「重置码 code」与「签发时刻的密码哈希 passwordHash」一并绑进签名，服务端校验时
 * 以客户端提交的 code 重算签名并比对，且 passwordHash 必须仍等于签发时刻的哈希——
 * 用户改密成功后旧哈希失效，截获的旧票据因哈希不符自动验签失败，关闭无状态票据的
 * 「用完不废」重放窗口。
 *
 * <p>签名比对走 {@link MessageDigest#isEqual}（定长字节定常比较），消除
 * {@code String.equals} 的时序侧信道。每枚票据最多 5 次验证（含错误码），超限即作废，
 * 防 6 位码爆破。
 *
 * <p>签名密钥来自 {@link PluginSecretProvider}（站点级随机密钥，与相册/恋爱解锁 token 同源，
 * 源码不含有效密钥，杜绝离线伪造）。
 *
 * @author 小莫唐尼
 */
@Component
public class ResetEmailCodeTicketManager {

    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    /** 每枚票据最大验证尝试次数（含错误码），超限即作废。 */
    private static final int MAX_ATTEMPTS = 5;

    private final PluginSecretProvider secretProvider;

    public ResetEmailCodeTicketManager(PluginSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    /** token → 已用尝试次数（内存态，重启清零；仅用于封顶爆破，不影响业务正确性）。 */
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    /**
     * 为重置请求签发票据。
     *
     * @param username     目标用户名（Halo User 的 metadata.name）
     * @param code         服务端生成的 6 位重置码（明文不入库，只参与签名）
     * @param passwordHash 签发时刻 spec.password（bcrypt 哈希），绑进签名以关闭重放窗口
     */
    public String issue(String username, String code, String passwordHash) {
        long expiry = System.currentTimeMillis() + TTL_MILLIS;
        String usernamePart = b64(username);
        return usernamePart + "." + expiry + "." + sign(usernamePart, expiry, code, passwordHash);
    }

    /**
     * 校验票据并解出用户名；无效（格式/签名/过期/尝试超限/账号不符）返回 {@code null}。
     *
     * @param token        签发票据
     * @param code         客户端提交的 6 位重置码
     * @param passwordHash 当前 spec.password（若已改密则与签发时刻不同，验签失败）
     * @return 票据归属用户名（与提交账号一致时方可用于重置），否则 null
     */
    public String verify(String token, String code, String passwordHash) {
        if (token == null || token.isBlank() || code == null) {
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
            String expected = sign(usernamePart, expiry, code, passwordHash);
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

    /** 重置成功后作废票据（清尝试计数；签名本身已因密码哈希变化而失效，此为双保险）。 */
    public void consume(String token) {
        if (token != null) {
            attempts.remove(token);
        }
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String usernamePart, long expiry, String code, String passwordHash) {
        String payload = usernamePart + "." + expiry + "." + (code == null ? "" : code)
                + "." + (passwordHash == null ? "" : passwordHash);
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
