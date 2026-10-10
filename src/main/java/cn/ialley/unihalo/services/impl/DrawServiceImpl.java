package cn.ialley.unihalo.services.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.exception.NotFoundException;
import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.services.DrawService;
import cn.ialley.unihalo.utils.NotificationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.JsonNode;

import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.isNull;

/**
 * 绘画作品业务服务实现。
 *
 * <p>越权防护硬约束：公开查询强制 status=APPROVED；UC 查询/删除强制 owner=当前用户，
 * 禁止信任任何客户端 owner 参数；创建时强制覆盖 owner/ownerDisplayName/status。
 *
 * @author 小莫唐尼
 */
@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final String DRAW_CONFIG_GROUP = "drawConfig";

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 200;

    private final ReactiveExtensionClient client;
    private final ReactiveSettingFetcher settingFetcher;
    private final NotificationHelper notificationHelper;

    @Override
    public Mono<ListResult<Draw>> listPublic(int page, int size) {
        return listByPage(ListOptions.builder()
                .andQuery(equal("spec.status", STATUS_APPROVED))
                .andQuery(isNull("metadata.deletionTimestamp"))
                .build(), page, size);
    }

    @Override
    public Mono<Draw> getPublic(String name) {
        return client.fetch(Draw.class, name)
                .filter(draw -> STATUS_APPROVED.equals(draw.getSpec().getStatus()))
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")));
    }

    @Override
    public Mono<ListResult<Draw>> listMine(String username, String status, int page, int size) {
        var builder = ListOptions.builder()
                .andQuery(equal("spec.owner", username))
                .andQuery(isNull("metadata.deletionTimestamp"));
        if (notBlank(status)) {
            builder.andQuery(equal("spec.status", status));
        }
        return listByPage(builder.build(), page, size);
    }

    @Override
    public Mono<Draw> getMine(String name, String username) {
        return client.fetch(Draw.class, name)
                .filter(draw -> username.equals(draw.getSpec().getOwner()))
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")));
    }

    @Override
    public Mono<Draw> create(Draw input, User author) {
        return auditEnabled().flatMap(enabled -> Mono.defer(() -> {
            var spec = validate(input);
            var username = author.getMetadata().getName();
            spec.setOwner(username);
            spec.setOwnerDisplayName(resolveDisplayName(author, username));
            spec.setStatus(enabled ? STATUS_PENDING : STATUS_APPROVED);
            spec.setReason(null);
            spec.setReviewedBy(null);
            spec.setReviewedAt(null);
            input.setMetadata(buildMetadata());
            return client.create(input);
        }));
    }

    @Override
    public Mono<Void> deleteMine(String name, String username) {
        return getMine(name, username)
                .flatMap(draw -> client.delete(draw))
                .then();
    }

    @Override
    public Mono<ListResult<Draw>> list(String status, String keyword, int page, int size) {
        var builder = ListOptions.builder()
                .andQuery(isNull("metadata.deletionTimestamp"));
        if (notBlank(status)) {
            builder.andQuery(equal("spec.status", status));
        }
        return client.listAll(Draw.class, builder.build(), Sort.unsorted())
                .filter(draw -> matches(draw, keyword))
                .collectList()
                .map(list -> {
                    list.sort(pendingFirstDesc());
                    return new ListResult<>(page, size, list.size(), slice(list, page, size));
                });
    }

    @Override
    public Mono<Draw> getManage(String name) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")));
    }

    @Override
    public Mono<Void> deleteManage(String name) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(draw -> client.delete(draw))
                .then();
    }

    @Override
    public Mono<Draw> approve(String name, String reviewer, String note) {
        return review(name, reviewer, STATUS_APPROVED, trimToNull(note));
    }

    @Override
    public Mono<Draw> reject(String name, String reviewer, String reason) {
        if (!notBlank(reason)) {
            return Mono.error(new IllegalArgumentException("拒绝原因不能为空"));
        }
        return review(name, reviewer, STATUS_REJECTED, reason.trim());
    }

    private Mono<Draw> review(String name, String reviewer, String status, String reason) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(draw -> {
                    var spec = draw.getSpec();
                    spec.setStatus(status);
                    spec.setReason(reason);
                    spec.setReviewedBy(reviewer);
                    spec.setReviewedAt(Instant.now().toString());
                    return client.update(draw)
                            .flatMap(updated -> notificationHelper
                                    .emitDrawReviewResult(spec.getOwner(), spec.getTitle(),
                                            status, reason)
                                    .thenReturn(updated));
                });
    }

    private Mono<ListResult<Draw>> listByPage(ListOptions options, int page, int size) {
        return client.listAll(Draw.class, options,
                        Sort.by(Sort.Direction.DESC, "metadata.creationTimestamp"))
                .collectList()
                .map(list -> new ListResult<>(page, size, list.size(), slice(list, page, size)));
    }

    /**
     * 发布是否需要审核（drawConfig.auditEnabled，缺省 true）
     */
    private Mono<Boolean> auditEnabled() {
        return settingFetcher.getSettingValue(DRAW_CONFIG_GROUP)
                .map(DrawServiceImpl::readAuditEnabled)
                .defaultIfEmpty(Boolean.TRUE)
                .onErrorReturn(Boolean.TRUE);
    }

    private static boolean readAuditEnabled(JsonNode node) {
        if (node == null) {
            return true;
        }
        JsonNode value;
        try {
            value = node.get("auditEnabled");
        } catch (Exception e) {
            return true;
        }
        if (value == null) {
            return true;
        }
        return value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean(value.asString());
    }

    /**
     * 发布前置校验：title/cover 必填、长度限制，返回可写的 spec
     */
    private Draw.DrawSpec validate(Draw input) {
        if (input == null || input.getSpec() == null) {
            throw new IllegalArgumentException("绘画数据不能为空");
        }
        var spec = input.getSpec();
        if (!notBlank(spec.getTitle())) {
            throw new IllegalArgumentException("画作标题不能为空");
        }
        if (spec.getTitle().trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("画作标题不能超过 " + TITLE_MAX_LENGTH + " 字");
        }
        if (!notBlank(spec.getCover())) {
            throw new IllegalArgumentException("画作图片不能为空");
        }
        spec.setTitle(spec.getTitle().trim());
        spec.setDescription(trimToLength(spec.getDescription(), DESCRIPTION_MAX_LENGTH));
        spec.setCover(spec.getCover().trim());
        spec.setAttachmentName(trimToNull(spec.getAttachmentName()));
        return spec;
    }

    private static String resolveDisplayName(User author, String username) {
        if (author.getSpec() != null && notBlank(author.getSpec().getDisplayName())) {
            return author.getSpec().getDisplayName();
        }
        return username;
    }

    private static Metadata buildMetadata() {
        Metadata metadata = new Metadata();
        metadata.setName(generateName());
        metadata.setCreationTimestamp(Instant.now());
        return metadata;
    }

    private static boolean matches(Draw draw, String keyword) {
        if (!notBlank(keyword)) {
            return true;
        }
        var spec = draw.getSpec();
        if (spec == null) {
            return false;
        }
        return contains(spec.getTitle(), keyword)
                || contains(spec.getOwnerDisplayName(), keyword);
    }

    /** 待审核优先（PENDING 在前），其余按创建时间倒序 */
    private static Comparator<Draw> pendingFirstDesc() {
        return Comparator
                .comparing((Draw draw) ->
                        STATUS_PENDING.equals(draw.getSpec().getStatus()) ? 0 : 1)
                .thenComparing(draw -> draw.getMetadata().getCreationTimestamp(),
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private static <T extends AbstractExtension> List<T> slice(List<T> list, int page, int size) {
        int from = Math.min((page - 1) * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    private static String trimToLength(String value, int maxLength) {
        var trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String generateName() {
        return "draw-" + System.currentTimeMillis() + "-"
                + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
    }
}
