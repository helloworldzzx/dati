package com.example.dati_backend.dto;

import java.util.List;

public record UserImportResult(
        Integer totalCount,
        Integer successCount,
        Integer failCount,
        List<UserImportError> errors
) {
}
