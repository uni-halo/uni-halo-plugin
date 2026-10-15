package cn.ialley.unihalo.services.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import cn.ialley.unihalo.exception.NotFoundException;
import cn.ialley.unihalo.scheme.Faq;
import cn.ialley.unihalo.services.FaqService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Condition;

import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.empty;
import static run.halo.app.extension.index.query.Queries.equal;

/**
 * 常见问题（FAQ）服务实现。
 *
 * @author 小莫唐尼
 */
@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<Faq>> list(String keyword, String category, Boolean visible, int page, int size) {
        return client.listAll(Faq.class, ListOptions.builder()
                        .fieldQuery(buildFieldQuery(category, visible))
                        .build(), resolveSort())
                .filter(faq -> matchesKeyword(faq, keyword))
                .collectList()
                .map(list -> new ListResult<>(page, size, list.size(), slice(list, page, size)));
    }

    @Override
    public Mono<ListResult<Faq>> listPublic(String keyword, String category, int page, int size) {
        return client.listAll(Faq.class, ListOptions.builder()
                        .fieldQuery(buildFieldQuery(category, true))
                        .build(), resolveSort())
                .filter(faq -> matchesKeyword(faq, keyword))
                .collectList()
                .map(list -> {
                    // size<=0 返回全量（app 端一次取全部）
                    if (size <= 0) {
                        return new ListResult<>(1, list.size(), list.size(), list);
                    }
                    return new ListResult<>(page, size, list.size(), slice(list, page, size));
                });
    }

    /**
     * 组合显隐与分类的索引字段查询条件（均为可选项，无条件时查全部）。
     */
    private static Condition buildFieldQuery(String category, Boolean visible) {
        List<Condition> conditions = new ArrayList<>();
        if (visible != null) {
            conditions.add(equal("spec.visible", visible));
        }
        if (category != null && !category.isBlank()) {
            conditions.add(equal("spec.category", category));
        }
        if (conditions.isEmpty()) {
            return empty();
        }
        return and(conditions.get(0), conditions.subList(1, conditions.size()).toArray(Condition[]::new));
    }

    /**
     * 排序：priority 升序（越小越靠前），同权重按创建时间倒序。
     */
    private static Sort resolveSort() {
        return Sort.by(Sort.Order.asc("spec.priority"),
                Sort.Order.desc("metadata.creationTimestamp"));
    }

    @Override
    public Mono<Faq> getByName(String name) {
        return client.fetch(Faq.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("常见问题不存在")));
    }

    @Override
    public Mono<Faq> getPublicByName(String name) {
        return getByName(name)
                .filter(faq -> faq.getSpec() != null
                        && Boolean.TRUE.equals(faq.getSpec().getVisible()))
                .switchIfEmpty(Mono.error(new NotFoundException("常见问题不存在或未公开")));
    }

    @Override
    public Mono<Faq> create(Faq faq) {
        return validate(faq).then(Mono.defer(() -> {
            Metadata metadata = new Metadata();
            metadata.setName(generateName());
            metadata.setCreationTimestamp(Instant.now());
            faq.setMetadata(metadata);
            return client.create(faq);
        }));
    }

    @Override
    public Mono<Faq> update(Faq faq) {
        String name = faq.getMetadata().getName();
        return client.fetch(Faq.class, name)
                .switchIfEmpty(Mono.error(new NotFoundException("常见问题不存在")))
                .flatMap(existing -> validate(faq).thenReturn(existing))
                .flatMap(existing -> {
                    existing.setSpec(faq.getSpec());
                    return client.update(existing);
                });
    }

    @Override
    public Mono<Void> delete(String name) {
        return client.fetch(Faq.class, name)
                .flatMap(client::delete)
                .then();
    }

    /**
     * 校验与规整：title 必填；visible 空则默认 true；priority 空则默认 0；
     * contact 类型链接不保留 url。
     */
    private Mono<Void> validate(Faq faq) {
        var spec = faq.getSpec();
        if (spec == null || spec.getTitle() == null || spec.getTitle().isBlank()) {
            return Mono.error(new IllegalArgumentException("问题标题不能为空"));
        }
        if (spec.getVisible() == null) {
            spec.setVisible(true);
        }
        if (spec.getPriority() == null) {
            spec.setPriority(0);
        }
        if (spec.getLinks() != null) {
            spec.getLinks().forEach(link -> {
                if (link != null && "contact".equals(link.getType())) {
                    link.setUrl(null);
                }
            });
        }
        return Mono.empty();
    }

    private static boolean matchesKeyword(Faq faq, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        if (faq.getSpec() == null) {
            return false;
        }
        String title = faq.getSpec().getTitle();
        String description = faq.getSpec().getDescription();
        String category = faq.getSpec().getCategory();
        return (title != null && title.contains(keyword))
                || (description != null && description.contains(keyword))
                || (category != null && category.contains(keyword));
    }

    private static <T> List<T> slice(List<T> list, int page, int size) {
        int from = Math.min(Math.max(page - 1, 0) * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    private static String generateName() {
        return "faq-" + System.currentTimeMillis() + "-"
                + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
    }
}
