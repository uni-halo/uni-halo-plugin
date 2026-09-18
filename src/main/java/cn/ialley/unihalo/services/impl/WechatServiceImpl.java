package cn.ialley.unihalo.services.impl;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.services.WechatService;
import tools.jackson.databind.ObjectMapper;

/**
 * 微信 code2Session 实现。
 *
 * 出站目标硬编码为 {@code api.weixin.qq.com}，不提供自定义地址；响应体大小与
 * 超时均设上限，避免长期占用事件循环。日志只记录 errcode，不记录完整响应体
 * （响应中可能包含会话密钥相关的敏感信息）。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    private static final String HOST = "api.weixin.qq.com";
    private static final String PATH = "/sns/jscode2session";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_BYTES = 256 * 1024;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatServiceImpl() {
        // 不注入容器中的 WebClient.Builder：本客户端只访问固定域名，
        // 自建实例可避免继承到全局的认证头或代理设置
        this.webClient = WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_BYTES))
                .build();
    }

    @Override
    public Mono<WechatSession> code2Session(String appId, String appSecret, String code) {
        if (code == null || code.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少登录凭证 code"));
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(HOST)
                        .path(PATH)
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .flatMap(this::parse);
    }

    private Mono<WechatSession> parse(String body) {
        try {
            var node = objectMapper.readTree(body);
            var errcode = node.get("errcode");
            if (errcode != null && !errcode.isNull() && errcode.asInt() != 0) {
                log.warn("【UniHalo】微信登录失败，errcode={}", errcode.asInt());
                return Mono.error(new IllegalArgumentException("微信登录失败，请重试"));
            }
            var openid = node.get("openid");
            if (openid == null || openid.isNull() || openid.asString().isBlank()) {
                return Mono.error(new IllegalArgumentException("微信登录失败，未获取到 openid"));
            }
            var unionid = node.get("unionid");
            return Mono.just(new WechatSession(openid.asString(),
                    unionid == null || unionid.isNull() ? null : unionid.asString()));
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("微信登录响应解析失败"));
        }
    }
}
