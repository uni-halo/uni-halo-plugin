package cn.ialley.unihalo.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.ialley.unihalo.exception.NotFoundException;
import cn.ialley.unihalo.scheme.Draw;
import cn.ialley.unihalo.scheme.DrawGuess;
import cn.ialley.unihalo.scheme.FeatureConfig;
import cn.ialley.unihalo.services.DrawGuessService;
import cn.ialley.unihalo.services.FeatureConfigService;
import cn.ialley.unihalo.vo.GuessBoardVo;
import cn.ialley.unihalo.vo.GuessRankItem;
import cn.ialley.unihalo.vo.GuessResultVo;
import cn.ialley.unihalo.vo.PuzzleInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.in;

/**
 * 猜画竞猜业务服务实现。
 *
 * <p>一致性设计：DrawGuess 以 {@code drawguess-{drawName}-{usernameHash}} 固定命名，
 * 同一用户同一画仅一条记录——猜错为更新（保留最后一次答案），猜中置 correct（一次），
 * 并发提交被 name 唯一性兜底；猜中榜按 solvedAt 排序；计数读时聚合，主表零写回。
 *
 * <p>积分不回收：画作删除/隐藏不清理 DrawGuess，历史得分永久有效；
 * 隐藏/待审核/已驳回画作不可再猜（服务端拒绝）。
 *
 * @author 小莫唐尼
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrawGuessServiceImpl implements DrawGuessService {

    public static final int ANSWER_MAX_LENGTH_DEFAULT = 20;
    public static final int RANK_TOP_N_DEFAULT = 20;
    public static final int MIN_REVEAL_WINDOW_HOURS_DEFAULT = 1;
    public static final int REVEAL_POSTPONE_HOURS_DEFAULT = 24;

    private final ReactiveExtensionClient client;
    private final FeatureConfigService featureConfigService;

    // ===== 作答 =====

    @Override
    public Mono<GuessResultVo> guess(String drawName, String username, String answer) {
        return guessEnabled()
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("猜画玩法未开启")))
                .then(fetchDraw(drawName))
                .flatMap(this::revealIfDue)
                .flatMap(draw -> {
                    var spec = draw.getSpec();
                    if (!DrawServiceImpl.STATUS_APPROVED.equals(spec.getStatus())
                            || Boolean.TRUE.equals(spec.getHidden())) {
                        return Mono.error(new IllegalArgumentException("该画作当前不可竞猜"));
                    }
                    if (!hasAnswer(spec)) {
                        return Mono.error(new IllegalArgumentException("该画作不是竞猜作品"));
                    }
                    if (Draw.PUZZLE_STATUS_REVEALED.equals(spec.getPuzzleStatus())) {
                        return Mono.error(new IllegalArgumentException("竞猜已揭晓"));
                    }
                    if (username.equals(spec.getOwnerName())) {
                        return Mono.error(new IllegalArgumentException("不能猜自己的画"));
                    }
                    var normalized = normalize(answer);
                    if (normalized.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("答案不能为空"));
                    }
                    var correct = normalized.equals(normalize(spec.getPuzzleAnswer()));
                    var close = !correct && isClose(normalized, normalize(spec.getPuzzleAnswer()));
                    return upsertGuess(drawName, username, answer, correct, close)
                            .thenReturn(correct
                                    ? GuessResultVo.correct(spec.getPuzzleAnswer())
                                    : GuessResultVo.of(close ? GuessResultVo.RESULT_CLOSE
                                            : GuessResultVo.RESULT_WRONG, null));
                });
    }

    /**
     * 固定命名 upsert：同一用户同一画一条记录，猜错更新、猜中置 correct（并发由 name 唯一性兜底）
     */
    private Mono<DrawGuess> upsertGuess(String drawName, String username, String rawAnswer,
            boolean correct, boolean close) {
        var now = Instant.now();
        return client.fetch(DrawGuess.class, recordName(drawName, username))
                .flatMap(existing -> {
                    if (Boolean.TRUE.equals(existing.getSpec().getCorrect())) {
                        return Mono.error(new IllegalArgumentException("你已猜中这幅画"));
                    }
                    var spec = existing.getSpec();
                    spec.setAnswer(trimToLength(rawAnswer, ANSWER_MAX_LENGTH_DEFAULT * 3));
                    spec.setClose(close);
                    if (correct) {
                        spec.setCorrect(true);
                        spec.setSolvedAt(now.toString());
                    }
                    return client.update(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    var guess = new DrawGuess();
                    var metadata = new Metadata();
                    metadata.setName(recordName(drawName, username));
                    metadata.setCreationTimestamp(now);
                    guess.setMetadata(metadata);
                    var spec = new DrawGuess.DrawGuessSpec();
                    spec.setDrawName(drawName);
                    spec.setUsername(username);
                    spec.setAnswer(trimToLength(rawAnswer, ANSWER_MAX_LENGTH_DEFAULT * 3));
                    spec.setCorrect(correct);
                    spec.setClose(close);
                    if (correct) {
                        spec.setSolvedAt(now.toString());
                    }
                    guess.setSpec(spec);
                    return client.create(guess)
                            // 并发首次作答时 name 冲突：按提交失败处理，用户重试即命中更新分支
                            .onErrorResume(e -> Mono.error(new IllegalArgumentException(
                                    "提交冲突，请稍后重试")));
                }));
    }

    // ===== 竞猜信息（榜单） =====

    @Override
    public Mono<GuessBoardVo> board(String drawName) {
        return fetchDraw(drawName)
                .flatMap(this::revealIfDue)
                .flatMap(draw -> listGuesses(drawName).collectList()
                        .flatMap(guesses -> {
                            var solved = guesses.stream()
                                    .filter(g -> Boolean.TRUE.equals(g.getSpec().getCorrect()))
                                    .sorted(Comparator.comparing(
                                            (DrawGuess g) -> g.getSpec().getSolvedAt(),
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .toList();
                            return resolveUsers(solved.stream()
                                            .map(g -> g.getSpec().getUsername()).distinct().toList())
                                    .map(users -> GuessBoardVo.of(draw, guesses.size(), solved.size(),
                                            solved.stream()
                                                    .map(g -> GuessBoardVo.itemOf(
                                                            g.getSpec().getUsername(),
                                                            displayNameOf(users, g.getSpec().getUsername()),
                                                            avatarOf(users, g.getSpec().getUsername()),
                                                            g.getSpec().getSolvedAt()))
                                                    .toList()));
                        }));
    }

    // ===== 积分排行榜 =====

    @Override
    public Mono<List<GuessRankItem>> rank(int top) {
        return rankTopN().flatMap(topN -> {
            var limit = top > 0 ? Math.min(top, topN) : topN;
            return client.listAll(DrawGuess.class, ListOptions.builder()
                            .andQuery(equal("spec.correct", "true"))
                            .build(), Sort.unsorted())
                    .collectList()
                    .flatMap(guesses -> {
                        Map<String, Long> scores = new LinkedHashMap<>();
                        for (DrawGuess guess : guesses) {
                            scores.merge(guess.getSpec().getUsername(), 1L, Long::sum);
                        }
                        List<Map.Entry<String, Long>> entries = new ArrayList<>(scores.entrySet());
                        entries.sort(Comparator
                                .comparing((Map.Entry<String, Long> entry) -> entry.getValue(),
                                        Comparator.reverseOrder())
                                .thenComparing(Map.Entry::getKey));
                        List<Map.Entry<String, Long>> topEntries =
                                entries.subList(0, Math.min(limit, entries.size()));
                        return resolveUsers(topEntries.stream()
                                        .map(Map.Entry::getKey).toList())
                                .map(users -> {
                                    var items = new ArrayList<GuessRankItem>();
                                    for (int i = 0; i < topEntries.size(); i++) {
                                        var entry = topEntries.get(i);
                                        var item = new GuessRankItem();
                                        item.setRank(i + 1);
                                        item.setUsername(entry.getKey());
                                        item.setScore(entry.getValue());
                                        item.setDisplayName(displayNameOf(users, entry.getKey()));
                                        item.setAvatar(avatarOf(users, entry.getKey()));
                                        items.add(item);
                                    }
                                    return items;
                                });
                    });
        });
    }

    // ===== 竞猜管理（出题人 / 管理端） =====

    @Override
    public Mono<PuzzleInfoVo> myPuzzle(String drawName, String username) {
        return fetchDraw(drawName)
                .flatMap(draw -> {
                    var spec = draw.getSpec();
                    if (spec == null || !username.equals(spec.getOwnerName())) {
                        return Mono.error(new NotFoundException("绘画不存在"));
                    }
                    return puzzleInfo(draw);
                });
    }

    @Override
    public Mono<PuzzleInfoVo> managePuzzle(String drawName) {
        return fetchDraw(drawName).flatMap(this::puzzleInfo);
    }

    private Mono<PuzzleInfoVo> puzzleInfo(Draw draw) {
        var drawName = draw.getMetadata().getName();
        return listGuesses(drawName).collectList()
                .flatMap(guesses -> resolveUsers(guesses.stream()
                                .map(g -> g.getSpec().getUsername()).distinct().toList())
                        .map(users -> {
                            // 猜中在前（按猜中时间），未猜中按最近作答时间倒序
                            var sorted = guesses.stream()
                                    .sorted(Comparator
                                            .comparing((DrawGuess g) ->
                                                    Boolean.TRUE.equals(g.getSpec().getCorrect()) ? 0 : 1)
                                            .thenComparing(g -> g.getSpec().getSolvedAt(),
                                                    Comparator.nullsLast(Comparator.naturalOrder()))
                                            .thenComparing(g -> g.getMetadata().getCreationTimestamp(),
                                                    Comparator.nullsLast(Comparator.reverseOrder())))
                                    .toList();
                            var solved = guesses.stream()
                                    .filter(g -> Boolean.TRUE.equals(g.getSpec().getCorrect())).count();
                            return PuzzleInfoVo.of(draw, sorted.stream()
                                            .map(g -> PuzzleInfoVo.recordOf(g,
                                                    displayNameOf(users, g.getSpec().getUsername()),
                                                    avatarOf(users, g.getSpec().getUsername())))
                                            .toList(),
                                    guesses.size(), solved);
                        }));
    }

    // ===== 揭晓 =====

    @Override
    public Mono<Void> revealByOwner(String drawName, String username) {
        return fetchDraw(drawName)
                .flatMap(draw -> {
                    var spec = draw.getSpec();
                    if (spec == null || !username.equals(spec.getOwnerName())) {
                        return Mono.error(new NotFoundException("绘画不存在"));
                    }
                    return doReveal(draw, username);
                });
    }

    @Override
    public Mono<Void> revealByManager(String drawName, String reviewer) {
        return fetchDraw(drawName).flatMap(draw -> doReveal(draw, reviewer));
    }

    private Mono<Void> doReveal(Draw draw, String revealer) {
        var spec = draw.getSpec();
        if (spec == null || !hasAnswer(spec)) {
            return Mono.error(new IllegalArgumentException("该画作不是竞猜作品"));
        }
        if (Draw.PUZZLE_STATUS_REVEALED.equals(spec.getPuzzleStatus())) {
            return Mono.empty();
        }
        spec.setPuzzleStatus(Draw.PUZZLE_STATUS_REVEALED);
        spec.setRevealedAt(Instant.now().toString());
        spec.setRevealedBy(revealer);
        return client.update(draw).then();
    }

    @Override
    public Mono<Draw> revealIfDue(Draw draw) {
        var spec = draw.getSpec();
        if (spec == null || !hasAnswer(spec)
                || !Draw.PUZZLE_STATUS_GUESSING.equals(spec.getPuzzleStatus())
                || spec.getAutoRevealAt() == null) {
            return Mono.just(draw);
        }
        Instant due;
        try {
            due = Instant.parse(spec.getAutoRevealAt());
        } catch (Exception e) {
            return Mono.just(draw);
        }
        if (!Instant.now().isAfter(due)) {
            return Mono.just(draw);
        }
        spec.setPuzzleStatus(Draw.PUZZLE_STATUS_REVEALED);
        spec.setRevealedAt(Instant.now().toString());
        spec.setRevealedBy(Draw.PUZZLE_REVEALER_SYSTEM);
        return client.update(draw)
                // 乐观锁冲突等失败静默忽略：下次读取再触发，幂等
                .onErrorResume(e -> {
                    log.debug("【UniHalo】惰性揭晓 {} 更新失败（并发或已翻转），下次读取重试",
                            draw.getMetadata().getName());
                    return Mono.just(draw);
                });
    }

    // ===== 配置 =====

    private Mono<Boolean> guessEnabled() {
        return drawConfig().map(draw -> draw.getGuessEnabled() == null || draw.getGuessEnabled());
    }

    private Mono<Integer> rankTopN() {
        return drawConfig().map(draw -> draw.getGuessRankTopN() != null
                && draw.getGuessRankTopN() > 0 ? draw.getGuessRankTopN() : RANK_TOP_N_DEFAULT);
    }

    private Mono<FeatureConfig.DrawPage> drawConfig() {
        return featureConfigService.get()
                .map(config -> config.getSpec() == null || config.getSpec().getPages() == null
                        ? null : config.getSpec().getPages().getDraw())
                .defaultIfEmpty(new FeatureConfig.DrawPage())
                .onErrorReturn(new FeatureConfig.DrawPage());
    }

    /**
     * 审核通过时的自动揭晓顺延：已过或剩余不足最小竞猜窗口则顺延（供 DrawServiceImpl.approve 调用）
     */
    public Mono<Draw> postponeAutoRevealOnApprove(Draw draw) {
        var spec = draw.getSpec();
        if (spec == null || !hasAnswer(spec) || spec.getAutoRevealAt() == null) {
            return Mono.just(draw);
        }
        Instant due;
        try {
            due = Instant.parse(spec.getAutoRevealAt());
        } catch (Exception e) {
            return Mono.just(draw);
        }
        return Mono.zip(drawConfig().map(config -> config.getGuessMinRevealWindowHours() != null
                                        && config.getGuessMinRevealWindowHours() >= 0
                                        ? config.getGuessMinRevealWindowHours()
                                        : MIN_REVEAL_WINDOW_HOURS_DEFAULT),
                        drawConfig().map(config -> config.getGuessRevealPostponeHours() != null
                                        && config.getGuessRevealPostponeHours() > 0
                                        ? config.getGuessRevealPostponeHours()
                                        : REVEAL_POSTPONE_HOURS_DEFAULT))
                .flatMap(tuple -> {
                    var window = tuple.getT1();
                    var postpone = tuple.getT2();
                    var earliest = Instant.now().plusSeconds(window * 3600L);
                    if (due.isAfter(earliest)) {
                        return Mono.just(draw);
                    }
                    spec.setAutoRevealAt(Instant.now().plusSeconds(postpone * 3600L).toString());
                    return Mono.just(draw);
                });
    }

    // ===== 工具 =====

    private Mono<Draw> fetchDraw(String drawName) {
        return client.fetch(Draw.class, drawName)
                .switchIfEmpty(Mono.error(new NotFoundException("绘画不存在")));
    }

    private Flux<DrawGuess> listGuesses(String drawName) {
        return client.listAll(DrawGuess.class, ListOptions.builder()
                .andQuery(equal("spec.drawName", drawName))
                .build(), Sort.unsorted());
    }

    private Mono<Map<String, User>> resolveUsers(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Mono.just(Map.of());
        }
        return client.listAll(User.class, ListOptions.builder()
                        .andQuery(in("metadata.name", usernames))
                        .build(), Sort.unsorted())
                .collectMap(user -> user.getMetadata().getName(), user -> user)
                .defaultIfEmpty(Map.of());
    }

    private static String displayNameOf(Map<String, User> users, String username) {
        var user = users.get(username);
        if (user != null && user.getSpec() != null
                && user.getSpec().getDisplayName() != null
                && !user.getSpec().getDisplayName().isBlank()) {
            return user.getSpec().getDisplayName();
        }
        return username;
    }

    private static String avatarOf(Map<String, User> users, String username) {
        var user = users.get(username);
        return user != null && user.getSpec() != null ? user.getSpec().getAvatar() : null;
    }

    private static boolean hasAnswer(Draw.DrawSpec spec) {
        return spec.getPuzzleAnswer() != null && !spec.getPuzzleAnswer().isBlank();
    }

    /**
     * 谜底记录名：固定命名保证同一用户同一画唯一；用户名做安全字符处理 + 摘要防碰撞
     */
    private static String recordName(String drawName, String username) {
        return "drawguess-" + drawName + "-" + shortHash(username);
    }

    private static String shortHash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    /**
     * 答案归一化：去首尾空格、转小写、全角转半角
     */
    static String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        var builder = new StringBuilder(answer.trim().length());
        for (char c : answer.trim().toCharArray()) {
            if (c == '\u3000') {
                c = ' ';
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                c = (char) (c - 0xFEE0);
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }

    /**
     * 最优字符串对齐距离（含相邻换位）≤1 判定「很接近」；按字符计，适配中文
     */
    static boolean isClose(String a, String b) {
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return false;
        }
        return osaDistance(a, b) <= 1;
    }

    private static int osaDistance(String a, String b) {
        var m = a.length();
        var n = b.length();
        if (Math.abs(m - n) > 1) {
            return 2;
        }
        var prev2 = new int[n + 1];
        var prev = new int[n + 1];
        var curr = new int[n + 1];
        for (var j = 0; j <= n; j++) {
            prev[j] = j;
        }
        for (var i = 1; i <= m; i++) {
            curr[0] = i;
            for (var j = 1; j <= n; j++) {
                var cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                var value = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    value = Math.min(value, prev2[j - 2] + 1);
                }
                curr[j] = value;
            }
            System.arraycopy(prev, 0, prev2, 0, n + 1);
            System.arraycopy(curr, 0, prev, 0, n + 1);
        }
        return prev[n];
    }

    private static String trimToLength(String value, int maxLength) {
        var trimmed = value == null || value.isBlank() ? null : value.trim();
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
