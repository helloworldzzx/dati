package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String phone;
    private String passwordHash;
    private String realName;
    private String role;
    private String status;
    private Boolean mustChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
