package cn.ialley.unihalo.utils;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import cn.ialley.unihalo.vo.LoginConfig;
import cn.ialley.unihalo.vo.WechatCredential;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Secret;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;

/**
 * 登录配置与微信凭据读取。
 *
 * 读取设置页 loginConfig 组的 {@code client} 子对象（两个登录开关、Secret 名称、
 * 令牌有效期）；注册策略不在此配置，沿用 Halo 系统设置。两个开关全关即登录能力
 * 整体不可用（fail closed）。微信凭据按 {@code wechatSecretName} 从 Secret 读取：
 * 优先 {@code stringData}，回落 {@code data}（base64）；未配置或字段缺失一律报错，
 * 由调用方关闭微信登录。
 *
 * @author 小莫唐尼
 */
@Service
@RequiredArgsConstructor
public class LoginConfigResolver {

    private static final String GROUP = "loginConfig";
    private static final String SUB_LOGIN = "client";

    private final ReactiveSettingFetcher settingFetcher;
    private final ReactiveExtensionClient client;

    public Mono<LoginConfig> config() {
        Mono<JsonNode> groupMono = settingFetcher.getSettingValue(GROUP);
        if (groupMono == null) {
            return Mono.just(LoginConfig.defaults());
        }
        return groupMono
                .map(node -> {
                    if (node == null) {
                        return LoginConfig.defaults();
                    }
                    var login = child(node, SUB_LOGIN);
                    return new LoginConfig(
                            bool(login, "passwordLoginEnabled", true),
                            bool(login, "wechatLoginEnabled", false),
                            text(login, "wechatSecretName"),
                            text(login, "wechatUsernamePrefix"),
                            text(login, "wechatUsernameType"),
                            intValue(login, "tokenTtlDays", 30));
                })
                .defaultIfEmpty(LoginConfig.defaults());
    }

    public Mono<WechatCredential> wechatCredential(String secretName) {
        if (secretName == null || secretName.isBlank()) {
            return Mono.error(new IllegalStateException("未配置微信小程序密钥"));
        }
        return client.fetch(Secret.class, secretName)
                .switchIfEmpty(Mono.error(
                        new IllegalStateException("微信小程序密钥资源不存在")))
                .map(secret -> {
                    var appId = read(secret, "appId");
                    var appSecret = read(secret, "appSecret");
                    if (appId == null || appId.isBlank() || appSecret == null
                            || appSecret.isBlank()) {
                        throw new IllegalStateException("微信小程序密钥缺少 appId 或 appSecret");
                    }
                    return new WechatCredential(appId, appSecret);
                });
    }

    private static String read(Secret secret, String key) {
        var stringData = secret.getStringData();
        if (stringData != null) {
            var value = stringData.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        var data = secret.getData();
        if (data != null && data.get(key) != null) {
            return new String(data.get(key), StandardCharsets.UTF_8);
        }
        return null;
    }

    private static JsonNode child(JsonNode node, String name) {
        var child = node.get(name);
        return child == null || child.isNull() ? null : child;
    }

    private static boolean bool(JsonNode node, String field, boolean fallback) {
        if (node == null) {
            return fallback;
        }
        var value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean(value.asString());
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        var value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        var text = value.asString();
        return text == null || text.isBlank() ? null : text;
    }

    private static int intValue(JsonNode node, String field, int fallback) {
        if (node == null) {
            return fallback;
        }
        var value = node.get(field);
        if (value == null || value.isNull() || !value.isNumber()) {
            return fallback;
        }
        int parsed = value.asInt();
        return parsed <= 0 ? fallback : parsed;
    }
}
