package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.FavoriteRequest;
import com.example.dati_backend.dto.PracticeProgressRequest;
import com.example.dati_backend.dto.PracticeProgressResponse;
import com.example.dati_backend.dto.PracticeSessionRequest;
import com.example.dati_backend.dto.SubmitAnswerRequest;
import com.example.dati_backend.entity.AnswerRecord;
import com.example.dati_backend.entity.PracticeSession;
import com.example.dati_backend.entity.Question;
import com.example.dati_backend.entity.UserQuestionStat;
import com.example.dati_backend.service.PracticeService;
import com.example.dati_backend.service.QuestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PracticeController {
    private final PracticeService practiceService;
    private final QuestionService questionService;

    @PostMapping("/api/practice/sessions")
    public ApiResponse<PracticeSession> startSession(@RequestBody PracticeSessionRequest request) {
        return ApiResponse.ok(practiceService.startSession(request));
    }

    @PatchMapping("/api/practice/sessions/{sessionId}/finish")
    public ApiResponse<Void> finishSession(@PathVariable Long sessionId) {
        practiceService.finishSession(sessionId);
        return ApiResponse.ok();
    }

    @PostMapping("/api/practice/answers")
    public ApiResponse<AnswerRecord> submitAnswer(@RequestBody SubmitAnswerRequest request) {
        return ApiResponse.ok(practiceService.submitAnswer(request));
    }

    @GetMapping("/api/users/{userId}/wrong-questions")
    public ApiResponse<List<Question>> wrongQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(questionService.listWrongQuestions(userId, page, size));
    }

    @GetMapping("/api/users/{userId}/favorite-questions")
    public ApiResponse<List<Question>> favoriteQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.ok(questionService.listFavoriteQuestions(userId, page, size));
    }

    @GetMapping("/api/users/{userId}/practice-progress")
    public ApiResponse<PracticeProgressResponse> getProgress(
            @PathVariable Long userId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Long categoryId
    ) {
        return ApiResponse.ok(practiceService.getProgress(userId, mode, categoryId));
    }

    @PutMapping("/api/users/{userId}/practice-progress")
    public ApiResponse<PracticeProgressResponse> saveProgress(
            @PathVariable Long userId,
            @RequestBody PracticeProgressRequest request
    ) {
        return ApiResponse.ok(practiceService.saveProgress(userId, request));
    }

    @PutMapping("/api/users/{userId}/questions/{questionId}/favorite")
    public ApiResponse<UserQuestionStat> updateFavorite(
            @PathVariable Long userId,
            @PathVariable Long questionId,
            @RequestBody(required = false) FavoriteRequest request
    ) {
        return ApiResponse.ok(practiceService.updateFavorite(userId, questionId, request));
    }
}
