package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.UserRequest;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<List<SysUser>> listUsers() {
        return ApiResponse.ok(userService.listUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> getUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @PostMapping
    public ApiResponse<SysUser> createUser(@RequestBody UserRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ApiResponse.ok();
    }
}
