package cn.ialley.unihalo.services;

import cn.ialley.unihalo.scheme.QRCodeInfo;
import reactor.core.publisher.Mono;

/**
 * 二维码信息服务。
 *
 * @author lywq
 */
public interface QRCodeInfoService {

    Mono<QRCodeInfo> fetchByKey(String key);

    Mono<QRCodeInfo> fetchByPostId(String postId);

    Mono<QRCodeInfo> save(QRCodeInfo qrCodeInfo);

    Mono<QRCodeInfo> update(QRCodeInfo qrCodeInfo);

    Mono<QRCodeInfo> delete(QRCodeInfo qrCodeInfo);
}