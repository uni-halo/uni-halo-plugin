package cn.ialley.unihalo.services.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import cn.ialley.unihalo.exception.NotFoundException;
import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.services.DrawService;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.vo.DrawVo;
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

import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.in;
import static run.halo.app.extension.index.query.Queries.isNull;
import static run.halo.app.extension.index.query.Queries.notEqual;

/**
 * 绘画作品业务服务实现。
 *
 * <p>越权防护硬约束：公开查询强制 status=APPROVED；UC 查询/删除强制 ownerName=当前用户，
 * 禁止信任任何客户端 ownerName 参数；创建时强制覆盖 ownerName/status。
 * 响应统一组装为 {@link DrawVo}，owner 实时解析 User（昵称/头像始终最新，
 * 语义同官方评论 OwnerInfo：用户不存在时回退用户名）。
 *
 * @author 小莫唐尼
 */
@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 200;

    private final ReactiveExtensionClient client;
    private final FeatureConfigService featureConfigService;

    @Override
    public Mono<ListResult<DrawVo>> listPublic(int page, int size) {
        return listByPage(ListOptions.builder()
                .andQuery(equal("spec.status", STATUS_APPROVED))
                .andQuery(notEqual("spec.hidden", "true"))
                .andQuery(isNull("metadata.deletionTimestamp"))
                .build(), page, size);
    }

    @Override
    public Mono<DrawVo> getPublic(String name) {
        return client.fetch(Draw.class, name)
                .filter(draw -> draw.getSpec() != null
                        && STATUS_APPROVED.equals(draw.getSpec().getStatus())
                        && !Boolean.TRUE.equals(draw.getSpec().getHidden()))
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(this::toVo);
    }

    @Override
    public Mono<ListResult<DrawVo>> listMine(String username, String status, int page, int size) {
        var builder = ListOptions.builder()
                .andQuery(equal("spec.ownerName", username))
                .andQuery(isNull("metadata.deletionTimestamp"));
        if (notBlank(status)) {
            builder.andQuery(equal("spec.status", status));
        }
        return listByPage(builder.build(), page, size);
    }

    @Override
    public Mono<DrawVo> getMine(String name, String username) {
        return client.fetch(Draw.class, name)
                .filter(draw -> draw.getSpec() != null
                        && username.equals(draw.getSpec().getOwnerName()))
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(this::toVo);
    }

    @Override
    public Mono<DrawVo> create(Draw input, User author) {
        return auditEnabled().flatMap(enabled -> Mono.defer(() -> {
            var spec = validate(input);
            spec.setOwnerName(author.getMetadata().getName());
            spec.setStatus(enabled ? STATUS_PENDING : STATUS_APPROVED);
            // 显隐由 setMineHidden/setHidden 维护，发布时忽略客户端传值
            spec.setHidden(null);
            spec.setReason(null);
            spec.setReviewedBy(null);
            spec.setReviewedAt(null);
            input.setMetadata(buildMetadata());
            return client.create(input).flatMap(this::toVo);
        }));
    }

    @Override
    public Mono<Void> deleteMine(String name, String username) {
        return getMineRaw(name, username)
                .flatMap(draw -> client.delete(draw))
                .then();
    }

    @Override
    public Mono<DrawVo> setMineHidden(String name, String username, boolean hidden) {
        return getMineRaw(name, username).flatMap(draw -> setVisibility(draw, hidden));
    }

    @Override
    public Mono<ListResult<DrawVo>> list(String status, String keyword, String sort, int page, int size) {
        var builder = ListOptions.builder()
                .andQuery(isNull("metadata.deletionTimestamp"));
        if (notBlank(status)) {
            builder.andQuery(equal("spec.status", status));
        }
        return client.listAll(Draw.class, builder.build(), Sort.unsorted())
                .filter(draw -> matches(draw, keyword))
                .collectList()
                .map(list -> {
                    list.sort(sortComparator(sort));
                    return new ListResult<>(page, size, list.size(), slice(list, page, size));
                })
                .flatMap(this::toVos);
    }

    @Override
    public Mono<DrawVo> getManage(String name) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(this::toVo);
    }

    @Override
    public Mono<Void> deleteManage(String name) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(draw -> client.delete(draw))
                .then();
    }

    @Override
    public Mono<DrawVo> setHidden(String name, boolean hidden) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(draw -> setVisibility(draw, hidden));
    }

    /**
     * 写入显隐标记并返回 VO（仅影响公开展示，不改审核状态）
     */
    private Mono<DrawVo> setVisibility(Draw draw, boolean hidden) {
        draw.getSpec().setHidden(hidden);
        return client.update(draw).flatMap(this::toVo);
    }

    @Override
    public Mono<DrawVo> approve(String name, String reviewer, String note) {
        return review(name, reviewer, STATUS_APPROVED, trimToNull(note));
    }

    @Override
    public Mono<DrawVo> reject(String name, String reviewer, String reason) {
        if (!notBlank(reason)) {
            return Mono.error(new IllegalArgumentException("拒绝原因不能为空"));
        }
        return review(name, reviewer, STATUS_REJECTED, reason.trim());
    }

    private Mono<DrawVo> review(String name, String reviewer, String status, String reason) {
        return client.fetch(Draw.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")))
                .flatMap(draw -> {
                    var spec = draw.getSpec();
                    if (spec == null || !notBlank(spec.getOwnerName())) {
                        return Mono.error(new IllegalArgumentException("绘画数据不完整"));
                    }
                    spec.setStatus(status);
                    spec.setReason(reason);
                    spec.setReviewedBy(reviewer);
                    spec.setReviewedAt(Instant.now().toString());
                    // 审核结果不发通知：结果直接展示在「我的绘画」列表（spec.reason/reviewedAt）
                    return client.update(draw).flatMap(this::toVo);
                });
    }

    private Mono<ListResult<DrawVo>> listByPage(ListOptions options, int page, int size) {
        return client.listAll(Draw.class, options,
                        Sort.by(Sort.Direction.DESC, "metadata.creationTimestamp"))
                .collectList()
                .map(list -> new ListResult<>(page, size, list.size(), list))
                .flatMap(this::toVos);
    }

    /**
     * 单条转 VO：owner 实时解析 User
     */
    private Mono<DrawVo> toVo(Draw draw) {
        return resolveOwners(List.of(draw)).map(owners -> owners.get(0));
    }

    /**
     * 批量转 VO：按 ownerName 去重后一次 in 查询 User，避免逐条查
     */
    private Mono<ListResult<DrawVo>> toVos(ListResult<Draw> result) {
        return resolveOwners(result.getItems())
                .map(vos -> new ListResult<>(result.getPage(), result.getSize(),
                        result.getTotal(), vos));
    }

    private Mono<List<DrawVo>> resolveOwners(List<Draw> draws) {
        var usernames = draws.stream()
                .map(Draw::getSpec)
                .filter(spec -> spec != null && notBlank(spec.getOwnerName()))
                .map(spec -> spec.getOwnerName())
                .distinct()
                .toList();
        if (usernames.isEmpty()) {
            return Mono.just(draws.stream()
                    .map(draw -> DrawVo.from(draw, fallbackOwner(draw)))
                    .toList());
        }
        return client.listAll(User.class, ListOptions.builder()
                        .andQuery(in("metadata.name", usernames))
                        .build(), Sort.unsorted())
                .collectMap(user -> user.getMetadata().getName())
                .defaultIfEmpty(Map.of())
                .map(users -> draws.stream()
                        .map(draw -> DrawVo.from(draw, ownerOf(draw, users)))
                        .toList());
    }

    /**
     * 组装作者信息：命中 User 取最新昵称/头像；未命中回退用户名
     */
    private static Draw.DrawOwner ownerOf(Draw draw, Map<String, User> users) {
        var owner = new Draw.DrawOwner();
        var username = draw.getSpec() == null ? null : draw.getSpec().getOwnerName();
        owner.setUsername(username);
        var user = username == null ? null : users.get(username);
        if (user != null && user.getSpec() != null) {
            owner.setDisplayName(notBlank(user.getSpec().getDisplayName())
                    ? user.getSpec().getDisplayName() : username);
            owner.setAvatar(trimToNull(user.getSpec().getAvatar()));
        } else {
            owner.setDisplayName(username);
        }
        return owner;
    }

    private static Draw.DrawOwner fallbackOwner(Draw draw) {
        return ownerOf(draw, Map.of());
    }

    /**
     * 发布是否需要审核（featureConfig.pages.draw.auditEnabled，缺省 true；
     * 功能设置-绘画管理弹窗维护）
     */
    private Mono<Boolean> auditEnabled() {
        return featureConfigService.get()
                .map(config -> {
                    var pages = config.getSpec() == null ? null : config.getSpec().getPages();
                    var draw = pages == null ? null : pages.getDraw();
                    return draw == null || draw.getAuditEnabled() == null
                            || draw.getAuditEnabled();
                })
                .defaultIfEmpty(Boolean.TRUE)
                .onErrorReturn(Boolean.TRUE);
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

    private static Metadata buildMetadata() {
        Metadata metadata = new Metadata();
        metadata.setName(generateName());
        metadata.setCreationTimestamp(Instant.now());
        return metadata;
    }

    private Mono<Draw> getMineRaw(String name, String username) {
        return client.fetch(Draw.class, name)
                .filter(draw -> draw.getSpec() != null
                        && username.equals(draw.getSpec().getOwnerName()))
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")));
    }

    private static boolean matches(Draw draw, String keyword) {
        if (!notBlank(keyword)) {
            return true;
        }
        var spec = draw.getSpec();
        if (spec == null) {
            return false;
        }
        return contains(spec.getTitle(), keyword) || contains(spec.getOwnerName(), keyword);
    }

    /**
     * sort 参数解析为内存排序比较器：
     * <ul>
     *   <li>默认（空/非法）：待审核优先，提交时间倒序</li>
     *   <li>createdAt：提交时间倒序</li>
     *   <li>reviewedAt：审核时间倒序（未审核排后）</li>
     * </ul>
     */
    private static Comparator<Draw> sortComparator(String sort) {
        return switch (sort == null ? "" : sort) {
            case "createdAt" -> Comparator.comparing(
                    (Draw draw) -> draw.getMetadata().getCreationTimestamp(),
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "reviewedAt" -> Comparator.comparing(
                    (Draw draw) -> draw.getSpec() != null ? draw.getSpec().getReviewedAt() : null,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> pendingFirstDesc();
        };
    }

    /** 待审核优先（PENDING 在前），其余按创建时间倒序 */
    private static Comparator<Draw> pendingFirstDesc() {
        return Comparator
                .comparing((Draw draw) ->
                        draw.getSpec() != null && STATUS_PENDING.equals(draw.getSpec().getStatus()) ? 0 : 1)
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
