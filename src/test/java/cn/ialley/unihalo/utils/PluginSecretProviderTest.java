package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 插件签名密钥提供器测试：读取复用 / 缺失生成 / 坏数据重建 / fail closed。
 */
@DisplayName("插件签名密钥提供器")
@ExtendWith(MockitoExtension.class)
class PluginSecretProviderTest {

    @Mock
    private ReactiveExtensionClient client;

    private PluginSecretProvider secretProvider;

    private static final String NAME = PluginSecretProvider.SECRET_CONFIGMAP_NAME;

    @Test
    @DisplayName("未 initialize 就取密钥（fail closed）→ 抛 IllegalStateException，不回退内置值")
    void requireSecretBeforeInitializeFailsClosed() {
        secretProvider = new PluginSecretProvider(client);
        assertThatThrownBy(secretProvider::requireSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未初始化");
    }

    @Test
    @DisplayName("首次启动无密钥 → 生成 32 字节随机密钥并持久化到插件私有 ConfigMap")
    void missingSecretIsGeneratedAndPersisted() {
        // 所有 fetch 调用延迟到订阅期求值（源码 write.then(fetchSecret()) 在
        // 组装期即调用 fetch）：create 发生前视为缺失，之后回读生成的 ConfigMap
        AtomicReference<ConfigMap> createdRef = new AtomicReference<>();
        when(client.fetch(ConfigMap.class, NAME)).thenAnswer(inv -> Mono.defer(() -> {
            ConfigMap created = createdRef.get();
            return created == null ? Mono.empty() : Mono.just(created);
        }));
        when(client.create(any(ConfigMap.class))).thenAnswer(inv -> {
            ConfigMap created = inv.getArgument(0);
            createdRef.set(created);
            return Mono.just(created);
        });

        secretProvider = new PluginSecretProvider(client);
        secretProvider.initialize().block();

        verify(client).create(any(ConfigMap.class));
        byte[] secret = secretProvider.requireSecret();
        assertThat(secret).hasSize(32);
    }

    @Test
    @DisplayName("已存在有效密钥 → 原样复用（重启沿用 / 多实例共享），不产生写操作")
    void existingValidSecretIsReused() {
        byte[] stored = new byte[32];
        for (int i = 0; i < stored.length; i++) {
            stored[i] = (byte) (i + 5);
        }
        when(client.fetch(ConfigMap.class, NAME))
                .thenReturn(Mono.just(configMapWith(Base64.getEncoder().encodeToString(stored))));

        secretProvider = new PluginSecretProvider(client);
        secretProvider.initialize().block();

        verify(client, never()).create(any(ConfigMap.class));
        verify(client, never()).update(any(ConfigMap.class));
        assertThat(secretProvider.requireSecret()).isEqualTo(stored);
    }

    @Test
    @DisplayName("存储数据损坏（非法 Base64）→ 按缺失处理，覆盖更新写入新密钥")
    void corruptedSecretIsRebuilt() {
        ConfigMap corrupted = configMapWith("!!!not-valid-base64!!!");
        when(client.fetch(ConfigMap.class, NAME))
                .thenReturn(Mono.just(corrupted), Mono.just(corrupted));
        when(client.update(any(ConfigMap.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        secretProvider = new PluginSecretProvider(client);
        secretProvider.initialize().block();

        verify(client).update(any(ConfigMap.class));
        assertThat(secretProvider.requireSecret()).hasSize(32);
    }

    @Test
    @DisplayName("数据键为空白字符串 → 视同缺失，走覆盖重建")
    void blankDataKeyIsTreatedAsMissing() {
        ConfigMap blank = configMapWith("   ");
        when(client.fetch(ConfigMap.class, NAME))
                .thenReturn(Mono.just(blank), Mono.just(blank));
        when(client.update(any(ConfigMap.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        secretProvider = new PluginSecretProvider(client);
        secretProvider.initialize().block();

        verify(client).update(any(ConfigMap.class));
        assertThat(secretProvider.requireSecret()).hasSize(32);
    }

    private static ConfigMap configMapWith(String encoded) {
        ConfigMap configMap = new ConfigMap();
        Metadata metadata = new Metadata();
        metadata.setName(NAME);
        configMap.setMetadata(metadata);
        configMap.setData(Map.of("token-hmac-key", encoded));
        return configMap;
    }
}
