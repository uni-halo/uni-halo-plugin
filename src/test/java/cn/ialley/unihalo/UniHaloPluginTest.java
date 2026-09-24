package cn.ialley.unihalo;

import cn.ialley.unihalo.utils.PluginSecretProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.PluginContext;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniHaloPluginTest {

    @Mock
    PluginContext context;

    @Mock
    SchemeManager schemeManager;

    @Mock
    PluginSecretProvider secretProvider;

    @InjectMocks
    UniHaloPlugin plugin;

    @Test
    void contextLoads() {
        when(secretProvider.initialize()).thenReturn(Mono.empty());
        plugin.start();
        plugin.stop();
    }
}
