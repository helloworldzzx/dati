package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.ChangePasswordRequest;
import com.example.dati_backend.dto.CompleteFirstLoginRequest;
import com.example.dati_backend.dto.LoginRequest;
import com.example.dati_backend.dto.LoginResponse;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> me(@AuthenticationPrincipal SysUser currentUser) {
        return ApiResponse.ok(authService.currentUser(currentUser));
    }

    @PostMapping("/complete-first-login")
    public ApiResponse<SysUser> completeFirstLogin(
            @AuthenticationPrincipal SysUser currentUser,
            @RequestBody CompleteFirstLoginRequest request
    ) {
        return ApiResponse.ok(authService.completeFirstLogin(currentUser, request));
    }

    @PostMapping("/change-password")
    public ApiResponse<SysUser> changePassword(
            @AuthenticationPrincipal SysUser currentUser,
            @RequestBody ChangePasswordRequest request
    ) {
        return ApiResponse.ok(authService.changePassword(currentUser, request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
