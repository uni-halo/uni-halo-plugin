package cn.ialley.unihalo.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.vo.IssuedToken;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.security.PersonalAccessToken;
import run.halo.app.security.authentication.CryptoService;

/**
 * PAT 自签实现单元测试。
 *
 * <p>用本地生成的 RSA 密钥对模拟 {@link CryptoService}，校验：
 * claims 契约与 Halo 官方 {@code PatServiceImpl#generateToken} 一致、JWS 头的 kid 取自
 * JWK（而非 CryptoService#getKeyId）、签名可被对应公钥验过，以及缺少私钥时 fail closed。</p>
 *
 * @author 小莫唐尼
 */
class PatIssuerImplTest {

    private static final String EXTERNAL_URL = "http://localhost:8090";

    @Test
    @DisplayName("签发的 token 是 pat_ 前缀的 JWT，claims 正确且可验签")
    void issueShouldSignValidPat() throws Exception {
        var rsaKey = new RSAKeyGenerator(2048).generate();
        var created = new AtomicReference<PersonalAccessToken>();
        var issuer = newIssuer(rsaKey, created);
        issuer.afterPropertiesSet();

        assertThat(issuer.available()).isTrue();

        var expiresAt = Instant.now().plusSeconds(3600);
        IssuedToken issued = issuer.issue("zhangsan", Set.of("authenticated"), expiresAt).block();

        assertThat(issued).isNotNull();
        assertThat(issued.username()).isEqualTo("zhangsan");
        assertThat(issued.roles()).containsExactly("authenticated");
        assertThat(issued.token()).startsWith(PersonalAccessToken.PAT_TOKEN_PREFIX);

        var jwt = SignedJWT.parse(issued.token().substring(
                PersonalAccessToken.PAT_TOKEN_PREFIX.length()));
        var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("zhangsan");
        assertThat(claims.getJWTID()).isNotBlank();
        assertThat(claims.getIssuer()).isEqualTo(EXTERNAL_URL);
        assertThat(claims.getClaim("pat_name")).isEqualTo(issued.patName());
        assertThat(claims.getExpirationTime()).isNotNull();
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(rsaKey.getKeyID());
        assertThat(jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))).isTrue();

        // 归属标签：过期清理据此区分插件签发的令牌与用户手动创建的令牌
        assertThat(created.get().getMetadata().getLabels())
                .containsEntry(Constants.PAT_MANAGED_BY_LABEL, Constants.PAT_MANAGED_BY_VALUE);
        assertThat(created.get().getSpec().getTokenId()).isEqualTo(claims.getJWTID());
    }

    @Test
    @DisplayName("未设置 expiresAt 时不写入 exp 声明")
    void issueWithoutExpiration() throws Exception {
        var rsaKey = new RSAKeyGenerator(2048).generate();
        var issuer = newIssuer(rsaKey);
        issuer.afterPropertiesSet();

        IssuedToken issued = issuer.issue("lisi", Set.of(), null).block();

        var jwt = SignedJWT.parse(issued.token().substring(4));
        assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isNull();
        assertThat(jwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))).isTrue();
    }

    @Test
    @DisplayName("CryptoService 不含私钥时自检失败且拒绝签发（fail closed）")
    void shouldFailClosedWhenPrivateKeyAbsent() throws Exception {
        var rsaKey = new RSAKeyGenerator(2048).generate();
        var publicOnly = rsaKey.toPublicJWK();
        var issuer = newIssuer(publicOnly);
        issuer.afterPropertiesSet();

        assertThat(issuer.available()).isFalse();
        assertThatThrownBy(() -> issuer.issue("zhangsan", Set.of(), null).block())
                .isInstanceOf(IllegalStateException.class);
    }

    private static PatIssuerImpl newIssuer(RSAKey jwk) {
        return newIssuer(jwk, new AtomicReference<>());
    }

    private static PatIssuerImpl newIssuer(RSAKey jwk,
            AtomicReference<PersonalAccessToken> created) {
        @SuppressWarnings("unchecked")
        var client = (ReactiveExtensionClient) mock(ReactiveExtensionClient.class);
        when(client.create(any(PersonalAccessToken.class))).thenAnswer(invocation -> {
            var pat = invocation.<PersonalAccessToken>getArgument(0);
            // 模拟 Halo 依据 generateName 生成最终名称
            pat.getMetadata().setName(pat.getMetadata().getGenerateName() + "abc123");
            created.set(pat);
            return Mono.just(pat);
        });
        return new PatIssuerImpl(client, new StubCryptoService(jwk), new StubExternalUrlSupplier());
    }

    private record StubCryptoService(RSAKey jwk) implements CryptoService {

        @Override
        public Mono<byte[]> decrypt(byte[] encryptedMessage) {
            return Mono.error(new UnsupportedOperationException());
        }

        @Override
        public Mono<byte[]> readPublicKey() {
            try {
                return Mono.just(jwk.toRSAPublicKey().getEncoded());
            } catch (JOSEException e) {
                return Mono.error(e);
            }
        }

        @Override
        public String getKeyId() {
            return "stub-key-id";
        }

        @Override
        public JWK getJwk() {
            return jwk;
        }
    }

    private static final class StubExternalUrlSupplier implements ExternalUrlSupplier {

        @Override
        public URI get() {
            return URI.create(EXTERNAL_URL);
        }

        @Override
        public URL getURL(HttpRequest request) {
            return getRaw();
        }

        @Override
        public URL getRaw() {
            try {
                return URI.create(EXTERNAL_URL).toURL();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
