package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.entity.QuestionImportBatch;
import com.example.dati_backend.service.QuestionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import-batches")
@RequiredArgsConstructor
public class QuestionImportController {
    private final QuestionImportService importService;

    @PostMapping
    public ApiResponse<QuestionImportBatch> createBatch(
            @RequestParam String fileName,
            @RequestParam(required = false) Long importedBy
    ) {
        return ApiResponse.ok(importService.createBatch(fileName, importedBy));
    }
}
