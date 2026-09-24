package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 恋爱相册解锁 token（HMAC-SHA256）签发与校验测试。
 */
@DisplayName("恋爱相册解锁 token")
@ExtendWith(MockitoExtension.class)
class AlbumTokenManagerTest {

    private static final byte[] SECRET = new byte[32];

    @Mock
    private PluginSecretProvider secretProvider;

    private AlbumTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < SECRET.length; i++) {
            SECRET[i] = (byte) i;
        }
        tokenManager = new AlbumTokenManager(secretProvider);
    }

    @Test
    @DisplayName("签发 token → 同相册校验通过")
    void issuedTokenVerifiesForSameAlbum() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("trip-2024");
        assertThat(tokenManager.verify("trip-2024", token)).isTrue();
    }

    @Test
    @DisplayName("token 与相册绑定 → 拿 A 相册的 token 访问 B 相册被拒")
    void tokenBoundToAlbum() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("album-a");
        assertThat(tokenManager.verify("album-b", token)).isFalse();
    }

    @Test
    @DisplayName("签名段被篡改 → 拒绝")
    void tamperedSignatureRejected() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("album-a");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "."
                + ("f".repeat(parts[2].length()));
        assertThat(tokenManager.verify("album-a", tampered)).isFalse();
    }

    @Test
    @DisplayName("有效期被篡改延长 → 签名不匹配，拒绝")
    void tamperedExpiryRejected() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("album-a");
        String[] parts = token.split("\\.");
        long newExpiry = Long.parseLong(parts[1]) + 3_600_000L;
        String tampered = parts[0] + "." + newExpiry + "." + parts[2];
        assertThat(tokenManager.verify("album-a", tampered)).isFalse();
    }

    @Test
    @DisplayName("超过 30 分钟有效期（签名合法）→ 过期拒绝")
    void expiredTokenRejected() {
        // 过期检查先于签名校验，无需密钥
        String album = "album-a";
        long expiredAt = System.currentTimeMillis() - 31 * 60 * 1000L;
        String expired = encode(album) + "." + expiredAt + "."
                + hexSign(album + "." + expiredAt);
        assertThat(tokenManager.verify(album, expired)).isFalse();
    }

    @Test
    @DisplayName("畸形 token（null/空白/段数错误/非法 Base64）→ 一律拒绝")
    void malformedTokensRejected() {
        // 结构畸形在校验签名前即被拒绝，无需密钥
        assertThat(tokenManager.verify(null, "x.y.z")).isFalse();
        assertThat(tokenManager.verify("album-a", null)).isFalse();
        assertThat(tokenManager.verify("album-a", "  ")).isFalse();
        assertThat(tokenManager.verify("album-a", "only-two-parts")).isFalse();
        assertThat(tokenManager.verify("album-a", "a.b.c.d")).isFalse();
        assertThat(tokenManager.verify("album-a", "not-base64!.123.abc")).isFalse();
    }

    @Test
    @DisplayName("中文相册名（base64url 编码）→ 签发校验闭环通过")
    void unicodeAlbumNameRoundTrip() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String album = "我们的故事 / 2024";
        String token = tokenManager.issue(album);
        assertThat(tokenManager.verify(album, token)).isTrue();
    }

    @Test
    @DisplayName("密钥未初始化（fail closed）→ 签发直接抛错，不产出可用 token")
    void missingSecretFailsClosed() {
        when(secretProvider.requireSecret())
                .thenThrow(new IllegalStateException("插件签名密钥未初始化"));
        assertThatThrownBy(() -> tokenManager.issue("album-a"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 与被测类相同的 HMAC-SHA256 hex 签名，用于构造受控 token。 */
    private static String hexSign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
