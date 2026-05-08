package com.example.dati_backend.dto;

import com.example.dati_backend.entity.QuestionImportBatch;
import com.example.dati_backend.entity.QuestionImportError;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionImportResult(
        Long id,
        String fileName,
        Integer totalCount,
        Integer successCount,
        Integer failCount,
        String status,
        Long importedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<QuestionImportError> errors
) {
    public static QuestionImportResult from(QuestionImportBatch batch, List<QuestionImportError> errors) {
        return new QuestionImportResult(
                batch.getId(),
                batch.getFileName(),
                batch.getTotalCount(),
                batch.getSuccessCount(),
                batch.getFailCount(),
                batch.getStatus(),
                batch.getImportedBy(),
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                errors == null ? List.of() : errors
        );
    }
}
