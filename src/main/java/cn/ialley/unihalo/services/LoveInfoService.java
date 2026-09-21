package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.LoveInfo;
import reactor.core.publisher.Mono;

import static cn.ialley.unihalo.constants.Constants.LOVE_INFO_SINGLETON_NAME;

/**
 * 恋爱信息服务（单例，固定 {@code name = "love-info"}）。
 *
 * @author 小莫唐尼
 */
public interface LoveInfoService {

    /** 单例固定资源名（对齐 Constants.LOVE_INFO_SINGLETON_NAME） */
    String LOVE_INFO_NAME = LOVE_INFO_SINGLETON_NAME;

    /**
     * 读取单例；不存在时返回空 Mono（调用方自行决定回落策略）。
     */
    Mono<LoveInfo> fetch();

    /**
     * 读取单例；不存在或读取失败时回落空 spec（字段全 null，由消费端回退默认文案）。
     */
    Mono<LoveInfo> fetchOrDefault();

    /**
     * 保存（upsert）：不存在则按固定 name 创建，存在则更新 spec。
     */
    Mono<LoveInfo> save(LoveInfo.LoveInfoSpec spec);
}
