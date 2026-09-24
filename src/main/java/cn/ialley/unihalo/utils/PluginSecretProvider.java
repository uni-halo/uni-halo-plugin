package cn.ialley.unihalo.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 插件内部 HMAC 签名密钥提供器（相册/恋爱模块解锁 token 共用）。
 *
 * 密钥于插件启动时生成（SecureRandom 32 字节）并持久化到插件私有 ConfigMap
 * {@value #SECRET_CONFIGMAP_NAME}：重启沿用、多实例共享；源码与公开配置出口均
 * 不可见，开源仓库不含有效密钥，杜绝离线伪造解锁签名。换密钥后旧 token
 * 最长 30 分钟自然失效，无迁移负担。
 *
 * @author 小莫唐尼
 */
@Component
public class PluginSecretProvider {

    /** 密钥持久化载体：插件私有 ConfigMap（公开配置出口白名单不含此名）。 */
    public static final String SECRET_CONFIGMAP_NAME = "uni-halo-internal-secret";

    private static final String SECRET_DATA_KEY = "token-hmac-key";
    private static final int SECRET_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ReactiveExtensionClient client;

    private volatile byte[] secret;

    public PluginSecretProvider(ReactiveExtensionClient client) {
        this.client = client;
    }

    /**
     * 启动期初始化：读取既有密钥，缺失则生成并持久化（调用方同步等待）。
     */
    public Mono<Void> initialize() {
        return ensureSecret()
                .doOnNext(value -> this.secret = value)
                .then();
    }

    /**
     * 取签名密钥；未初始化即抛错（fail closed，不回退任何内置值）。
     */
    public byte[] requireSecret() {
        byte[] value = this.secret;
        if (value == null) {
            throw new IllegalStateException("插件签名密钥未初始化");
        }
        return value;
    }

    private Mono<byte[]> ensureSecret() {
        return fetchSecret()
                .switchIfEmpty(Mono.defer(this::upsertSecret));
    }

    /**
     * 写入新生成的密钥：已存在记录（含历史坏数据）则覆盖更新，新建冲突
     * （多实例同时启动）则回读对方写入的密钥。
     */
    private Mono<byte[]> upsertSecret() {
        byte[] generated = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(generated);
        String encoded = Base64.getEncoder().encodeToString(generated);
        Mono<Void> write = client.fetch(ConfigMap.class, SECRET_CONFIGMAP_NAME)
                .flatMap(existing -> {
                    existing.setData(Map.of(SECRET_DATA_KEY, encoded));
                    return client.update(existing).then();
                })
                .switchIfEmpty(Mono.defer(() -> client.create(buildConfigMap(encoded)).then()));
        // 写入竞争失败可容忍：只要能读到任一有效密钥即成功
        return write.onErrorResume(Exception.class, e -> Mono.empty())
                .then(fetchSecret())
                .switchIfEmpty(Mono.error(new IllegalStateException("签名密钥写入后读取失败")));
    }

    /**
     * 读取持久化密钥；记录缺失或数据损坏（缺键/非法 Base64）一律按缺失处理，
     * 由上层走重建写入（覆盖更新即可修复坏数据）。
     */
    private Mono<byte[]> fetchSecret() {
        return client.fetch(ConfigMap.class, SECRET_CONFIGMAP_NAME)
                .mapNotNull(configMap -> configMap.getData() == null
                        ? null
                        : configMap.getData().get(SECRET_DATA_KEY))
                .handle((encoded, sink) -> {
                    if (encoded != null && !encoded.isBlank()) {
                        try {
                            sink.next(Base64.getDecoder().decode(encoded));
                        } catch (IllegalArgumentException ignored) {
                            // 数据损坏按缺失处理，走重建
                        }
                    }
                });
    }

    private static ConfigMap buildConfigMap(String encoded) {
        ConfigMap configMap = new ConfigMap();
        Metadata metadata = new Metadata();
        metadata.setName(SECRET_CONFIGMAP_NAME);
        configMap.setMetadata(metadata);
        configMap.setData(Map.of(SECRET_DATA_KEY, encoded));
        return configMap;
    }
}
