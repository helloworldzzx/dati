package com.example.dati_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RankingService {
    private static final Duration RANKING_CACHE_TTL = Duration.ofSeconds(10);
    private static final String RANKING_CACHE_PREFIX = "dati:ranking:";

    private final UserAnswerStatMapper userAnswerStatMapper;
    private final RedisJsonCacheService cacheService;

    public List<RankingItem> ranking(Integer limit) {
        return ranking(limit, null);
    }

    public List<RankingItem> ranking(Integer limit, String sort) {
        int safeLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        String safeSort = normalizeSort(sort);
        String cacheKey = rankingCacheKey(safeLimit, safeSort);
        TypeReference<List<RankingItem>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    List<RankingItem> items = switch (safeSort) {
                        case "answerCount" -> userAnswerStatMapper.listRankingByAnswerCount(safeLimit);
                        case "accuracy" -> userAnswerStatMapper.listRankingByAccuracy(safeLimit);
                        default -> userAnswerStatMapper.listRanking(safeLimit);
                    };
                    cacheService.set(cacheKey, items, RANKING_CACHE_TTL);
                    return items;
                });
    }

    public PageResult<RankingItem> pageRanking(Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        String cacheKey = rankingPageCacheKey(safePage, safeSize);
        TypeReference<PageResult<RankingItem>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    PageResult<RankingItem> pageResult = new PageResult<>(
                            userAnswerStatMapper.listRankingPage(safeSize, (safePage - 1) * safeSize),
                            userAnswerStatMapper.countRanking(),
                            safePage,
                            safeSize
                    );
                    cacheService.set(cacheKey, pageResult, RANKING_CACHE_TTL);
                    return pageResult;
                });
    }

    public void clearRankingCache() {
        cacheService.deleteByPattern(RANKING_CACHE_PREFIX + "*");
    }

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "correct";
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "answer", "answers", "answer_count", "answercount", "count" -> "answerCount";
            case "accuracy", "accuracy_rate", "accuracyrate", "rate" -> "accuracy";
            default -> "correct";
        };
    }

    private String rankingCacheKey(int limit, String sort) {
        return RANKING_CACHE_PREFIX + "list:sort:" + sort + ":limit:" + limit;
    }

    private String rankingPageCacheKey(int page, int size) {
        return RANKING_CACHE_PREFIX + "admin:page:" + page + ":size:" + size;
    }
}
