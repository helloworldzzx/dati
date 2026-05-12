package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.dto.QuestionBatchDeleteRequest;
import com.example.dati_backend.dto.QuestionDetailResponse;
import com.example.dati_backend.dto.QuestionRequest;
import com.example.dati_backend.entity.Question;
import com.example.dati_backend.service.QuestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    @GetMapping("/api/questions")
    public ApiResponse<List<Question>> listQuestions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "ENABLED") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ApiResponse.ok(questionService.listQuestions(categoryId, type, status, page, size));
    }

    @GetMapping("/api/admin/questions")
    public ApiResponse<PageResult<Question>> pageAdminQuestions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "ENABLED") String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        return ApiResponse.ok(questionService.pageQuestions(categoryId, type, status, page, size));
    }

    @GetMapping("/api/questions/{id}")
    public ApiResponse<QuestionDetailResponse> getQuestion(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId
    ) {
        return ApiResponse.ok(questionService.getQuestionDetail(id, userId));
    }

    @PostMapping("/api/admin/questions")
    public ApiResponse<QuestionDetailResponse> createQuestion(@RequestBody QuestionRequest request) {
        return ApiResponse.ok(questionService.createQuestion(request));
    }

    @PutMapping("/api/admin/questions/{id}")
    public ApiResponse<QuestionDetailResponse> updateQuestion(
            @PathVariable Long id,
            @RequestBody QuestionRequest request
    ) {
        return ApiResponse.ok(questionService.updateQuestion(id, request));
    }

    @DeleteMapping("/api/admin/questions/{id}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ApiResponse.ok();
    }

    @PostMapping("/api/admin/questions/batch-delete")
    public ApiResponse<Void> deleteQuestions(@RequestBody QuestionBatchDeleteRequest request) {
        questionService.deleteQuestions(request == null ? null : request.ids());
        return ApiResponse.ok();
    }
}
