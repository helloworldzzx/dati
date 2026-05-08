package com.example.dati_backend.dto;

public record UserRequest(
        String username,
        String phone,
        String password,
        String realName,
        String role,
        String status,
        Boolean mustChangePassword
) {
}
