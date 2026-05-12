package com.example.dati_backend.service;

import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final UserAnswerStatMapper userAnswerStatMapper;

    public List<RankingItem> ranking(Integer limit) {
        return ranking(limit, null);
    }

    public List<RankingItem> ranking(Integer limit, String sort) {
        int safeLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return switch (normalizeSort(sort)) {
            case "answerCount" -> userAnswerStatMapper.listRankingByAnswerCount(safeLimit);
            case "accuracy" -> userAnswerStatMapper.listRankingByAccuracy(safeLimit);
            default -> userAnswerStatMapper.listRanking(safeLimit);
        };
    }

    public PageResult<RankingItem> pageRanking(Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        return new PageResult<>(
                userAnswerStatMapper.listRankingPage(safeSize, (safePage - 1) * safeSize),
                userAnswerStatMapper.countRanking(),
                safePage,
                safeSize
        );
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
}
