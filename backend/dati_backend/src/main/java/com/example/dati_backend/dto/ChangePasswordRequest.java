package com.example.dati_backend.dto;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
