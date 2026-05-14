package com.example.dati_backend.service;

import com.example.dati_backend.dto.ChangePasswordRequest;
import com.example.dati_backend.dto.CompleteFirstLoginRequest;
import com.example.dati_backend.dto.LoginRequest;
import com.example.dati_backend.dto.LoginResponse;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.mapper.SysUserMapper;
import com.example.dati_backend.security.JwtToken;
import com.example.dati_backend.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.account()) || !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Account and password are required");
        }
        SysUser user = userMapper.findByAccount(request.account().trim());
        if (user == null || !"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Invalid account or password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid account or password");
        }
        JwtToken token = jwtTokenService.createToken(user);
        return new LoginResponse(token.token(), token.expiresAt(), user);
    }

    public SysUser currentUser(SysUser currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Not logged in");
        }
        SysUser user = userMapper.findById(currentUser.getId());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    @Transactional
    public SysUser completeFirstLogin(SysUser currentUser, CompleteFirstLoginRequest request) {
        SysUser user = currentUser(currentUser);
        if (request == null) {
            throw new IllegalArgumentException("New password is required");
        }
        validateNewPassword(request.newPassword());
        if (StringUtils.hasText(request.phone())) {
            userMapper.updatePhoneAndPassword(
                    user.getId(),
                    request.phone().trim(),
                    passwordEncoder.encode(request.newPassword()),
                    false
            );
        } else {
            userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()), false);
        }
        return userMapper.findById(user.getId());
    }

    @Transactional
    public SysUser changePassword(SysUser currentUser, ChangePasswordRequest request) {
        SysUser user = currentUser(currentUser);
        if (request == null || !StringUtils.hasText(request.oldPassword()) || !StringUtils.hasText(request.newPassword())) {
            throw new IllegalArgumentException("Old password and new password are required");
        }
        validateNewPassword(request.newPassword());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()), false);
        return userMapper.findById(user.getId());
    }

    private void validateNewPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("New password is required");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
    }
}
