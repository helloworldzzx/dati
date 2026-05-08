package com.example.dati_backend.dto;

import java.util.List;
import java.util.Map;

public record PracticeProgressRequest(
        String mode,
        Long categoryId,
        Integer currentIndex,
        Long currentQuestionId,
        List<Long> questionIds,
        Map<String, PracticeProgressDraft> drafts
) {
}
