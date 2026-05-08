package com.example.dati_backend.dto;

import java.util.List;

public record QuestionRequest(
        Long categoryId,
        String type,
        String title,
        String correctAnswer,
        String analysis,
        String sourceFile,
        String status,
        Long createdBy,
        List<QuestionOptionRequest> options
) {
}
