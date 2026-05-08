package com.example.dati_backend.dto;

public record SubmitAnswerRequest(
        Long sessionId,
        Long userId,
        Long questionId,
        String userAnswer,
        Boolean correct,
        Integer durationSeconds
) {
}
