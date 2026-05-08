package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.entity.QuestionImportBatch;
import com.example.dati_backend.service.QuestionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class QuestionImportController {
    private final QuestionImportService importService;

    @PostMapping("/api/admin/import-batches")
    public ApiResponse<QuestionImportBatch> createBatch(
            @RequestParam String fileName,
            @RequestParam(required = false) Long importedBy
    ) {
        return ApiResponse.ok(importService.createBatch(fileName, importedBy));
    }

    @PostMapping(value = "/api/admin/questions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<QuestionImportBatch> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long importedBy,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        Long operatorId = importedBy != null ? importedBy : currentUser.getId();
        return ApiResponse.ok(importService.importQuestions(file, operatorId));
    }

    @GetMapping("/api/admin/questions/import-template")
    public ResponseEntity<byte[]> importTemplate(@RequestParam(defaultValue = "all") String type) {
        byte[] content = importService.buildTemplate(type);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(importService.templateFileName(type))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
