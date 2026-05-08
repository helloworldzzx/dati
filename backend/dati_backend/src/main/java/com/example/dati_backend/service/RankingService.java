package com.example.dati_backend.service;

import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final UserAnswerStatMapper userAnswerStatMapper;

    public List<RankingItem> ranking(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return userAnswerStatMapper.listRanking(safeLimit);
    }
}
