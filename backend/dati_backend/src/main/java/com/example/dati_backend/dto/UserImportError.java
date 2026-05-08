package com.example.dati_backend.dto;

public record UserImportError(
        Integer rowNo,
        String message
) {
}
