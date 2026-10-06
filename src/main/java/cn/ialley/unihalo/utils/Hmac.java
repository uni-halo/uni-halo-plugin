package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 签名工具（插件内无状态 token / 票据共用）。
 * 密钥由调用方从 PluginSecretProvider 取得并传入，本类不持有密钥。
 */
public final class Hmac {

    /** HMAC 算法名（HmacSHA256）。 */
    public static final String SHA256 = "HmacSHA256";

    private Hmac() {
    }

    /**
     * 以 HMAC-SHA256 对 payload 签名并返回小写十六进制串。
     *
     * @param key     签名密钥（原始字节，来自 PluginSecretProvider）
     * @param payload 待签名明文
     * @return 小写十六进制签名
     */
    public static String sha256Hex(byte[] key, String payload) {
        try {
            Mac mac = Mac.getInstance(SHA256);
            mac.init(new SecretKeySpec(key, SHA256));
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
