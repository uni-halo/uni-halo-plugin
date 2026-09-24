package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * 微信补邮箱注册票据（HMAC-SHA256 无状态签名，30 分钟有效）。
 *
 * <p>token 格式：{@code base64url(openid).base64url(unionid 或 "-").expiry.hex(signature)}。
 * 最终注册时服务端还会用客户端新提交的 wx.login code 二次换取 openid 与票据比对，
 * 票据泄露不等于账号可冒注册。
 *
 * <p>签名密钥来自 {@link PluginSecretProvider}（站点级随机密钥，启动期生成并持久化
 * 到插件私有 ConfigMap），源码不含有效密钥，杜绝离线伪造票据。
 *
 * @author 小莫唐尼
 */
@Component
public class WechatRegisterTicketManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PluginSecretProvider secretProvider;

    public WechatRegisterTicketManager(PluginSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    /** unionid 缺省占位（开放平台未绑定时无 unionid）。 */
    private static final String UNIONID_ABSENT = "-";

    /**
     * 为微信身份签发补邮箱注册票据。
     *
     * @param openid  小程序内唯一标识（必填）
     * @param unionid 开放平台唯一标识，可为空
     */
    public String issue(String openid, String unionid) {
        long expiry = System.currentTimeMillis() + TTL_MILLIS;
        String openidPart = b64(openid);
        String unionidPart = b64(unionid == null || unionid.isBlank() ? UNIONID_ABSENT : unionid);
        return openidPart + "." + unionidPart + "." + expiry + "." + sign(openidPart, unionidPart, expiry);
    }

    /**
     * 校验票据并解出微信身份；无效（格式/签名/过期）返回 {@code null}。
     */
    public Ticket verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 4) {
                return null;
            }
            String openid = new String(
                    Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String unionid = new String(
                    Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long expiry = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() >= expiry) {
                return null;
            }
            String expected = sign(parts[0], parts[1], expiry);
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    parts[3].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return new Ticket(openid, UNIONID_ABSENT.equals(unionid) ? null : unionid);
        } catch (Exception e) {
            return null;
        }
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String openidPart, String unionidPart, long expiry) {
        String payload = openidPart + "." + unionidPart + "." + expiry;
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

    /**
     * 票据携带的微信身份。
     *
     * @param openid  小程序内唯一标识
     * @param unionid 开放平台唯一标识，签发时缺失则为 {@code null}
     */
    public record Ticket(String openid, String unionid) {
    }
}
