package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;
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
 * 恋爱模块入口解锁 token（HMAC-SHA256）签发与校验测试。
 *
 * <p>与 {@link AlbumTokenManager} 同模式独立实现，此处只覆盖
 * 模块维度核心契约：签发/校验闭环、模块绑定、过期与畸形拒绝。
 */
@DisplayName("恋爱模块入口解锁 token")
@ExtendWith(MockitoExtension.class)
class LoveModuleTokenManagerTest {

    private static final byte[] SECRET = new byte[32];

    @Mock
    private PluginSecretProvider secretProvider;

    private LoveModuleTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < SECRET.length; i++) {
            SECRET[i] = (byte) (i + 1);
        }
        tokenManager = new LoveModuleTokenManager(secretProvider);
    }

    @Test
    @DisplayName("签发 token → 同模块校验通过（ourStory/lovePhoto/loveDaily）")
    void issuedTokenVerifiesForSameModule() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("lovePhoto");
        assertThat(tokenManager.verify("lovePhoto", token)).isTrue();
    }

    @Test
    @DisplayName("token 与模块绑定 → 拿故事模块的 token 访问清单模块被拒")
    void tokenBoundToModule() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("ourStory");
        assertThat(tokenManager.verify("loveDaily", token)).isFalse();
    }

    @Test
    @DisplayName("超过 30 分钟有效期（签名合法）→ 过期拒绝")
    void expiredTokenRejected() {
        // 过期检查先于签名校验，无需密钥
        String module = "loveDaily";
        long expiredAt = System.currentTimeMillis() - 31 * 60 * 1000L;
        String expired = encode(module) + "." + expiredAt + "."
                + hexSign(module + "." + expiredAt);
        assertThat(tokenManager.verify(module, expired)).isFalse();
    }

    @Test
    @DisplayName("畸形 token（null/空白/段数错误）→ 一律拒绝")
    void malformedTokensRejected() {
        // 结构畸形在校验签名前即被拒绝，无需密钥
        assertThat(tokenManager.verify(null, "x.y.z")).isFalse();
        assertThat(tokenManager.verify("lovePhoto", null)).isFalse();
        assertThat(tokenManager.verify("lovePhoto", "")).isFalse();
        assertThat(tokenManager.verify("lovePhoto", "two.parts")).isFalse();
    }

    @Test
    @DisplayName("密钥变更（与签发时不一致）→ 旧 token 全部失效")
    void secretMismatchRejects() {
        when(secretProvider.requireSecret()).thenReturn(SECRET);
        String token = tokenManager.issue("lovePhoto");
        byte[] otherSecret = new byte[32];
        otherSecret[0] = 9;
        when(secretProvider.requireSecret()).thenReturn(otherSecret);
        assertThat(tokenManager.verify("lovePhoto", token)).isFalse();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

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
