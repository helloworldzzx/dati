package com.example.dati_backend.security;

import java.time.Instant;

public record JwtToken(
        String token,
        Instant expiresAt
) {
}
