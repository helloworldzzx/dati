package com.example.dati_backend.dto;

public record PracticeSessionRequest(
        Long userId,
        Long categoryId,
        String mode,
        Integer totalCount
) {
}
