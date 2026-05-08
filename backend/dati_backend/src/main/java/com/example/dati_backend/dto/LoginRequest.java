package com.example.dati_backend.dto;

public record LoginRequest(
        String account,
        String password
) {
}
