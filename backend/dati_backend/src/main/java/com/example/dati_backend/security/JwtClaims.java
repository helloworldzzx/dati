package com.example.dati_backend.security;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String username,
        String role,
        Instant expiresAt
) {
}
