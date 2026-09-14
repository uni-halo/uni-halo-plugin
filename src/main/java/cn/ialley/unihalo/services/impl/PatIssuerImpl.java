package cn.ialley.unihalo.services.impl;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.services.PatIssuer;
import cn.ialley.unihalo.vo.IssuedToken;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.security.PersonalAccessToken;
import run.halo.app.security.authentication.CryptoService;

/**
 * 自签 PAT 实现（方案 A）。
 *
 * <p>Halo 的 PAT 本质是一个 RS256 JWT，claims 契约见
 * {@code PatServiceImpl#generateToken}：{@code iss} / {@code jti}(= spec.tokenId) /
 * {@code sub}(= username) / {@code iat} / {@code pat_name}，可选 {@code exp}。
 * 签名密钥来自 {@link CryptoService#getJwk()}（{@code RsaKeyService} 暴露的 JWK 含私钥），
 * 因此本实现签出的令牌与官方「个人中心 → 个人令牌」创建的完全等价。</p>
 *
 * <p>两点关键约束：</p>
 * <ol>
 *   <li>JWS 头的 {@code kid} 必须取 {@code jwk.getKeyID()}（而非
 *       {@code cryptoService.getKeyId()}），否则 Halo 的 JWKSource 按 kid 匹配不到密钥，
 *       验签直接失败；</li>
 *   <li>过期只由 JWT {@code exp} 保证——{@code PatAuthenticationManager} 不校验
 *       {@code spec.expiresAt}，因此调用方务必传入 expiresAt。</li>
 * </ol>
 *
 * <p>启动时做一次自签自检（签完立即用公钥验签），失败则 {@link #available()} 返回
 * false，登录能力应据此 fail closed。</p>
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatIssuerImpl implements PatIssuer, InitializingBean {

    private static final String PAT_DISPLAY_NAME = "uni-halo mobile";

    private final ReactiveExtensionClient client;
    private final CryptoService cryptoService;
    private final ExternalUrlSupplier externalUrl;

    private volatile boolean available;

    @Override
    public void afterPropertiesSet() {
        this.available = selfCheck();
        if (this.available) {
            log.info("【UniHalo】PAT 自签自检通过，移动端登录能力可用");
        } else {
            log.error("【UniHalo】PAT 自签自检失败，移动端登录能力已关闭"
                    + "（Halo 可能不再通过 CryptoService 暴露 RSA 私钥，需切换到反射调用 PatService）");
        }
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public Mono<IssuedToken> issue(String username, Set<String> roles, Instant expiresAt) {
        if (!available) {
            return Mono.error(new IllegalStateException("PAT 签发能力不可用"));
        }
        var pat = new PersonalAccessToken();
        pat.setMetadata(new Metadata());
        pat.getMetadata().setGenerateName("pat-" + username + "-");
        pat.getMetadata().setLabels(new HashMap<>(
                Map.of(Constants.PAT_MANAGED_BY_LABEL, Constants.PAT_MANAGED_BY_VALUE)));
        pat.getSpec().setName(PAT_DISPLAY_NAME);
        pat.getSpec().setUsername(username);
        pat.getSpec().setRoles(new ArrayList<>(roles));
        pat.getSpec().setExpiresAt(expiresAt);
        pat.getSpec().setTokenId(UUID.randomUUID().toString());
        return client.create(pat).flatMap(this::sign);
    }

    @Override
    public Mono<Void> revoke(String patName, String username) {
        return client.fetch(PersonalAccessToken.class, patName)
                .filter(pat -> username.equals(pat.getSpec().getUsername()))
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("令牌不存在或不属于当前用户")))
                .flatMap(pat -> {
                    pat.getSpec().setRevoked(true);
                    pat.getSpec().setRevokesAt(Instant.now());
                    return client.update(pat);
                })
                .then();
    }

    private Mono<IssuedToken> sign(PersonalAccessToken pat) {
        return Mono.deferContextual(contextView -> {
            try {
                var jwk = (RSAKey) cryptoService.getJwk();
                var claimsBuilder = new JWTClaimsSet.Builder()
                        .issuer(resolveIssuer(contextView))
                        .jwtID(pat.getSpec().getTokenId())
                        .subject(pat.getSpec().getUsername())
                        .issueTime(Date.from(Instant.now()))
                        .claim("pat_name", pat.getMetadata().getName());
                var expiresAt = pat.getSpec().getExpiresAt();
                if (expiresAt != null) {
                    claimsBuilder.expirationTime(Date.from(expiresAt));
                }
                var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(jwk.getKeyID())
                        .build();
                var signed = new SignedJWT(header, claimsBuilder.build());
                signed.sign(new RSASSASigner(jwk.toRSAPrivateKey()));
                List<String> roles = pat.getSpec().getRoles() == null
                        ? List.of() : pat.getSpec().getRoles();
                return Mono.just(new IssuedToken(
                        PersonalAccessToken.PAT_TOKEN_PREFIX + signed.serialize(),
                        pat.getMetadata().getName(),
                        pat.getSpec().getUsername(),
                        Set.copyOf(roles),
                        expiresAt));
            } catch (JOSEException e) {
                return Mono.error(new IllegalStateException("PAT 签名失败", e));
            }
        });
    }

    private String resolveIssuer(ContextView contextView) {
        return ServerWebExchangeContextFilter.getExchange(contextView)
                .map(exchange -> externalUrl.getURL(exchange.getRequest()))
                .map(URL::toString)
                .orElseGet(() -> {
                    var raw = externalUrl.getRaw();
                    return raw != null ? raw.toString() : externalUrl.get().toString();
                });
    }

    /**
     * 启动自检：确认 CryptoService 暴露的 JWK 含 RSA 私钥，且自签的 JWT 能被其公钥验过。
     */
    private boolean selfCheck() {
        try {
            var jwk = cryptoService.getJwk();
            if (!(jwk instanceof RSAKey rsaKey)) {
                log.error("【UniHalo】CryptoService 返回的 JWK 不是 RSAKey：{}",
                        jwk == null ? "null" : jwk.getClass().getName());
                return false;
            }
            if (rsaKey.toRSAPrivateKey() == null) {
                log.error("【UniHalo】CryptoService 的 JWK 不含 RSA 私钥，无法自签 PAT");
                return false;
            }
            var claims = new JWTClaimsSet.Builder()
                    .jwtID("self-check")
                    .subject("__self_check__")
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                    .claim("pat_name", "self-check")
                    .build();
            var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build();
            var signed = new SignedJWT(header, claims);
            signed.sign(new RSASSASigner(rsaKey.toRSAPrivateKey()));
            return signed.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()));
        } catch (Exception e) {
            log.error("【UniHalo】PAT 自签自检异常", e);
            return false;
        }
    }
}
