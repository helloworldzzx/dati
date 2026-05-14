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
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.entity.UserQuestionStat;
import com.example.dati_backend.service.PracticeService;
import com.example.dati_backend.service.QuestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<PracticeSession> startSession(
            @AuthenticationPrincipal SysUser currentUser,
            @RequestBody PracticeSessionRequest request
    ) {
        Long userId = accessibleUserId(request == null ? null : request.userId(), currentUser);
        PracticeSessionRequest safeRequest = new PracticeSessionRequest(
                userId,
                request == null ? null : request.categoryId(),
                request == null ? null : request.mode(),
                request == null ? null : request.totalCount()
        );
        return ApiResponse.ok(practiceService.startSession(safeRequest));
    }

    @PatchMapping("/api/practice/sessions/{sessionId}/finish")
    public ApiResponse<Void> finishSession(
            @AuthenticationPrincipal SysUser currentUser,
            @PathVariable Long sessionId
    ) {
        practiceService.finishSession(sessionId, currentUserId(currentUser), isAdmin(currentUser));
        return ApiResponse.ok();
    }

    @PostMapping("/api/practice/answers")
    public ApiResponse<AnswerRecord> submitAnswer(
            @AuthenticationPrincipal SysUser currentUser,
            @RequestBody SubmitAnswerRequest request
    ) {
        Long userId = accessibleUserId(request == null ? null : request.userId(), currentUser);
        SubmitAnswerRequest safeRequest = new SubmitAnswerRequest(
                request == null ? null : request.sessionId(),
                userId,
                request == null ? null : request.questionId(),
                request == null ? null : request.userAnswer(),
                null,
                request == null ? null : request.durationSeconds()
        );
        return ApiResponse.ok(practiceService.submitAnswer(safeRequest, isAdmin(currentUser)));
    }

    @GetMapping("/api/users/{userId}/wrong-questions")
    public ApiResponse<List<Question>> wrongQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        return ApiResponse.ok(questionService.listWrongQuestions(accessibleUserId(userId, currentUser), page, size));
    }

    @GetMapping("/api/users/{userId}/favorite-questions")
    public ApiResponse<List<Question>> favoriteQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        return ApiResponse.ok(questionService.listFavoriteQuestions(accessibleUserId(userId, currentUser), page, size));
    }

    @GetMapping("/api/users/{userId}/practice-progress")
    public ApiResponse<PracticeProgressResponse> getProgress(
            @PathVariable Long userId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        return ApiResponse.ok(practiceService.getProgress(accessibleUserId(userId, currentUser), mode, categoryId));
    }

    @PutMapping("/api/users/{userId}/practice-progress")
    public ApiResponse<PracticeProgressResponse> saveProgress(
            @PathVariable Long userId,
            @RequestBody PracticeProgressRequest request,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        return ApiResponse.ok(practiceService.saveProgress(accessibleUserId(userId, currentUser), request));
    }

    @PutMapping("/api/users/{userId}/questions/{questionId}/favorite")
    public ApiResponse<UserQuestionStat> updateFavorite(
            @PathVariable Long userId,
            @PathVariable Long questionId,
            @RequestBody(required = false) FavoriteRequest request,
            @AuthenticationPrincipal SysUser currentUser
    ) {
        return ApiResponse.ok(practiceService.updateFavorite(accessibleUserId(userId, currentUser), questionId, request));
    }

    private Long accessibleUserId(Long requestedUserId, SysUser currentUser) {
        Long currentUserId = currentUserId(currentUser);
        if (isAdmin(currentUser)) {
            return requestedUserId == null ? currentUserId : requestedUserId;
        }
        if (requestedUserId == null || requestedUserId.equals(currentUserId)) {
            return currentUserId;
        }
        throw new AccessDeniedException("Cannot access another user's data");
    }

    private Long currentUserId(SysUser currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Not logged in");
        }
        return currentUser.getId();
    }

    private boolean isAdmin(SysUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }
}
