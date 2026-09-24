package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * 恋爱模块入口解锁 token 工具（HMAC-SHA256 无状态签名）。
 *
 * 与相册 {@link AlbumTokenManager} 同一模式，独立实现：
 * token 格式 base64url(scope).expiry.hex(signature)，scope 为恋爱模块入口名
 * （ourStory / lovePhoto / loveDaily），verify 校验模块匹配、签名一致且未过期
 * （默认有效期 30 分钟）。签名密钥由 {@link PluginSecretProvider} 启动期生成
 * 并持久化，源码不含有效密钥。
 *
 * @author 小莫唐尼
 */
@Component
public class LoveModuleTokenManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    private final PluginSecretProvider secretProvider;

    public LoveModuleTokenManager(PluginSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    /**
     * 为指定恋爱模块入口签发解锁 token（scope = ourStory/lovePhoto/loveDaily）。
     */
    public String issue(String module) {
        long expiry = System.currentTimeMillis() + TTL_MILLIS;
        String payload = module + "." + expiry;
        String encodedModule = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(module.getBytes(StandardCharsets.UTF_8));
        return encodedModule + "." + expiry + "." + sign(payload);
    }

    /**
     * 校验 token 是否有效且属于指定恋爱模块入口。
     */
    public boolean verify(String module, String token) {
        if (module == null || token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String decodedModule = new String(
                    Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            long expiry = Long.parseLong(parts[1]);
            if (!module.equals(decodedModule)) {
                return false;
            }
            if (System.currentTimeMillis() >= expiry) {
                return false;
            }
            String expected = sign(decodedModule + "." + expiry);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretProvider.requireSecret(), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 签名失败", e);
        }
    }
}
