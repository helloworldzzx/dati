package com.example.dati_backend.dto;

import java.util.List;

public record QuestionBatchDeleteRequest(
        List<Long> ids
) {
}
