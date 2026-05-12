package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.dto.UserImportResult;
import com.example.dati_backend.dto.UserRequest;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ApiResponse.ok(userService.pageUsers(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> getUser(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @PostMapping
    public ApiResponse<SysUser> createUser(@RequestBody UserRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(userService.importUsers(file));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] content = userService.buildImportTemplate();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("user-import-template.xlsx")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
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
