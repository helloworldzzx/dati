package com.example.dati_backend.dto;

public record CompleteFirstLoginRequest(
        String phone,
        String newPassword
) {
}
