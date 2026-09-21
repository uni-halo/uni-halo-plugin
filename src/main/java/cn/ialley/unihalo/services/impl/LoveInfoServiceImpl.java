package cn.ialley.unihalo.services.impl;

import cn.ialley.unihalo.scheme.LoveInfo;
import cn.ialley.unihalo.services.LoveInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 恋爱信息服务实现（单例 upsert）。
 *
 * 注意：兜底默认结构不得携带 metadata —— {@code save()} 以 metadata 判定
 * create/update，误带会对不存在的资源走 update。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveInfoServiceImpl implements LoveInfoService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<LoveInfo> fetch() {
        return client.fetch(LoveInfo.class, LOVE_INFO_NAME);
    }

    @Override
    public Mono<LoveInfo> fetchOrDefault() {
        return fetch()
                .onErrorResume(e -> {
                    log.warn("读取恋爱信息失败，按空配置渲染：{}", e.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(defaultLoveInfo());
    }

    @Override
    public Mono<LoveInfo> save(LoveInfo.LoveInfoSpec spec) {
        return fetch()
                .flatMap(existing -> {
                    existing.setSpec(spec);
                    return client.update(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    LoveInfo created = new LoveInfo();
                    created.setMetadata(new Metadata());
                    created.getMetadata().setName(LOVE_INFO_NAME);
                    created.setSpec(spec);
                    return client.create(created);
                }));
    }

    /** 空配置兜底（仅 spec，无 metadata） */
    private static LoveInfo defaultLoveInfo() {
        LoveInfo info = new LoveInfo();
        info.setSpec(new LoveInfo.LoveInfoSpec());
        return info;
    }
}
