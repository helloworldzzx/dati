package com.example.dati_backend.dto;

public record QuestionOptionRequest(
        String optionKey,
        String optionContent,
        Boolean correct,
        Integer sortNo
) {
}
