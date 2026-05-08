package com.example.dati_backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PracticeProgressResponse(
        Long id,
        Long userId,
        String mode,
        Long categoryId,
        Integer currentIndex,
        Long currentQuestionId,
        List<Long> questionIds,
        Map<String, PracticeProgressDraft> drafts,
        LocalDateTime updatedAt
) {
}
