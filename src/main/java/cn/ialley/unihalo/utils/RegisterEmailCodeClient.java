package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * 注册邮箱验证码发送客户端：代理转发 Halo 匿名端点 {@code POST /signup/send-email-code}。
 *
 * <p>该端点在站点根下、不在 Halo 的 CSRF 豁免名单内，小程序无法参与 CSRF 校验，
 * 由插件服务端完成握手后代发：GET /signup 取 XSRF-TOKEN Cookie → 按 Spring Security
 * Xor 规则构造掩码 token → 连同 Cookie 与 X-XSRF-TOKEN 头一起 POST。
 */
@Component
public class RegisterEmailCodeClient {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WebClient webClient;

    public RegisterEmailCodeClient() {
        // 不注入容器中的 WebClient.Builder（与 WechatServiceImpl 同策略，规避插件容器差异）
        this.webClient = WebClient.builder().build();
    }

    /**
     * 发送注册验证码。
     *
     * @param baseUrl  站点根地址（ExternalUrlSupplier 统一解析：外部地址配置优先，否则按请求推导）
     * @param email    收码邮箱
     * @param clientIp 客户端源地址（socket 派生，透传 X-Forwarded-For 供官方按 IP 限流）
     */
    public Mono<Void> send(String baseUrl, String email, String clientIp) {
        return webClient.get().uri(baseUrl + "/signup")
            .exchangeToMono(resp -> {
                ResponseCookie cookie = resp.cookies().getFirst(CSRF_COOKIE);
                if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
                    return Mono.error(new IllegalStateException("获取站点 CSRF token 失败"));
                }
                return Mono.just(new String[] {cookie.getValue(), maskToken(cookie.getValue())});
            })
            .flatMap(pair -> webClient.post().uri(baseUrl + "/signup/send-email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    headers.set("Cookie", CSRF_COOKIE + "=" + pair[0]);
                    headers.set(CSRF_HEADER, pair[1]);
                    if (clientIp != null && !clientIp.isBlank()) {
                        headers.set("X-Forwarded-For", clientIp);
                    }
                })
                .bodyValue(Map.of("email", email))
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return Mono.<Void>empty();
                    }
                    return resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new IllegalStateException(
                            "发送注册验证码失败: HTTP " + resp.statusCode().value()
                                    + (body.isBlank() ? "" : " " + abbreviate(body)))));
                }))
            .timeout(Duration.ofSeconds(15))
            .onErrorMap(WebClientResponseException.class,
                e -> new IllegalStateException("发送注册验证码失败: HTTP " + e.getStatusCode().value(), e));
    }

    /**
     * 按 Spring Security XorServerCsrfTokenRequestAttributeHandler 的算法构造掩码 token：
     * {@code base64url(randomBytes ++ (randomBytes ^ raw))}，解码后长度须为原始 token 的两倍。
     */
    private static String maskToken(String rawToken) {
        byte[] raw = rawToken.getBytes(StandardCharsets.UTF_8);
        byte[] random = new byte[raw.length];
        SECURE_RANDOM.nextBytes(random);
        byte[] combined = new byte[raw.length * 2];
        System.arraycopy(random, 0, combined, 0, raw.length);
        for (int i = 0; i < raw.length; i++) {
            combined[raw.length + i] = (byte) (random[i] ^ raw[i]);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    }

    private static String abbreviate(String body) {
        var cleaned = body.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 120 ? cleaned : cleaned.substring(0, 120) + "...";
    }
}
