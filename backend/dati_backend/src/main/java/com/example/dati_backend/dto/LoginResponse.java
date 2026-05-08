package com.example.dati_backend.dto;

import com.example.dati_backend.entity.SysUser;
import java.time.Instant;

public record LoginResponse(
        String token,
        Instant expiresAt,
        SysUser user
) {
}
