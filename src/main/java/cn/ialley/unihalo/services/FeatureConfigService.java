package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.FeatureConfig;
import reactor.core.publisher.Mono;

/**
 * 功能设置服务
 *
 * @author 小莫唐尼
 */
public interface FeatureConfigService {

    /**
     * 读取功能设置单例；不存在时返回默认结构（与存量配置合并）。
     */
    Mono<FeatureConfig> get();

    /**
     * 保存功能设置单例（不存在则创建；写入前与默认值/存量值做非空合并，防丢字段）。
     */
    Mono<FeatureConfig> save(FeatureConfig config);

    /**
     * 校验恋爱模块入口密码（ourStory/lovePhoto/loveDaily）。
     * 模块未设置密码或密码不匹配均返回 false（不暴露模块是否已设置密码）。
     * 读取原始单例（未经 {@link #get()} 脱敏），供公开解锁接口使用。
     */
    Mono<Boolean> verifyLoveModulePassword(String module, String password);

    /**
     * 恋爱模块入口是否设置了密码（锁定态；true 时公开数据接口要求携带有效 token）。
     */
    Mono<Boolean> isLoveModuleLocked(String module);
}
