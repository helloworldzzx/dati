package com.example.dati_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.dati_backend.dto.FavoriteRequest;
import com.example.dati_backend.dto.PracticeProgressDraft;
import com.example.dati_backend.dto.PracticeProgressRequest;
import com.example.dati_backend.dto.PracticeProgressResponse;
import com.example.dati_backend.dto.PracticeSessionRequest;
import com.example.dati_backend.dto.SubmitAnswerRequest;
import com.example.dati_backend.entity.AnswerRecord;
import com.example.dati_backend.entity.PracticeSession;
import com.example.dati_backend.entity.Question;
import com.example.dati_backend.entity.UserPracticeProgress;
import com.example.dati_backend.entity.UserQuestionStat;
import com.example.dati_backend.mapper.AnswerRecordMapper;
import com.example.dati_backend.mapper.PracticeSessionMapper;
import com.example.dati_backend.mapper.QuestionMapper;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import com.example.dati_backend.mapper.UserPracticeProgressMapper;
import com.example.dati_backend.mapper.UserQuestionStatMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PracticeService {
    private final PracticeSessionMapper sessionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;
    private final UserQuestionStatMapper userQuestionStatMapper;
    private final UserAnswerStatMapper userAnswerStatMapper;
    private final UserPracticeProgressMapper userPracticeProgressMapper;
    private final RankingService rankingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PracticeSession startSession(PracticeSessionRequest request) {
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        PracticeSession session = new PracticeSession();
        session.setUserId(request.userId());
        session.setCategoryId(request.categoryId());
        session.setMode(StringUtils.hasText(request.mode()) ? request.mode().trim().toUpperCase() : "PRACTICE");
        session.setTotalCount(request.totalCount() == null ? 0 : request.totalCount());
        sessionMapper.insert(session);
        return sessionMapper.findById(session.getId());
    }

    @Transactional
    public AnswerRecord submitAnswer(SubmitAnswerRequest request) {
        return submitAnswer(request, false);
    }

    @Transactional
    public AnswerRecord submitAnswer(SubmitAnswerRequest request, boolean admin) {
        if (request == null || request.userId() == null || request.questionId() == null) {
            throw new IllegalArgumentException("User id and question id are required");
        }
        validateSessionOwner(request.sessionId(), request.userId(), admin);
        Question question = questionService.getQuestionForAnswer(request.questionId());
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }

        Boolean correct = judgeAnswer(question, request.userAnswer());
        int durationSeconds = request.durationSeconds() == null ? 0 : Math.max(request.durationSeconds(), 0);

        AnswerRecord record = new AnswerRecord();
        record.setSessionId(request.sessionId());
        record.setUserId(request.userId());
        record.setQuestionId(request.questionId());
        record.setUserAnswer(request.userAnswer());
        record.setCorrect(correct);
        record.setDurationSeconds(durationSeconds);
        answerRecordMapper.insert(record);

        questionMapper.increaseCounters(request.questionId(), correct);
        if (request.sessionId() != null) {
            sessionMapper.increaseCounters(request.sessionId(), correct);
        }

        UserQuestionStat questionStat = new UserQuestionStat();
        questionStat.setUserId(request.userId());
        questionStat.setQuestionId(request.questionId());
        questionStat.setLastAnswer(request.userAnswer());
        questionStat.setLastCorrect(correct);
        userQuestionStatMapper.upsertAnswer(questionStat);
        userAnswerStatMapper.upsertAnswer(request.userId(), correct, durationSeconds);
        rankingService.clearRankingCache();

        return answerRecordMapper.findById(record.getId());
    }

    @Transactional
    public UserQuestionStat updateFavorite(Long userId, Long questionId, FavoriteRequest request) {
        if (userId == null || questionId == null) {
            throw new IllegalArgumentException("User id and question id are required");
        }
        Boolean favorite = request == null || request.favorite() == null || request.favorite();
        userQuestionStatMapper.upsertFavorite(userId, questionId, favorite);
        return userQuestionStatMapper.findByUserAndQuestion(userId, questionId);
    }

    @Transactional
    public void finishSession(Long sessionId) {
        sessionMapper.finish(sessionId);
    }

    @Transactional
    public void finishSession(Long sessionId, Long currentUserId, boolean admin) {
        PracticeSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Practice session not found");
        }
        if (!admin && !session.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Cannot finish another user's session");
        }
        sessionMapper.finish(sessionId);
    }

    public PracticeProgressResponse getProgress(Long userId, String mode, Long categoryId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        String normalizedMode = normalizeMode(mode);
        Long normalizedCategoryId = normalizeCategoryId(categoryId);
        UserPracticeProgress progress = userPracticeProgressMapper.findByScope(
                userId,
                buildScopeKey(normalizedMode, normalizedCategoryId)
        );
        return progress == null ? null : toProgressResponse(progress);
    }

    public List<PracticeProgressResponse> listProgress(Long userId, String mode) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        String normalizedMode = StringUtils.hasText(mode) ? normalizeMode(mode) : null;
        return userPracticeProgressMapper.listByUserAndMode(userId, normalizedMode)
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Transactional
    public PracticeProgressResponse saveProgress(Long userId, PracticeProgressRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Progress request is required");
        }
        String normalizedMode = normalizeMode(request.mode());
        Long normalizedCategoryId = normalizeCategoryId(request.categoryId());

        UserPracticeProgress progress = new UserPracticeProgress();
        progress.setUserId(userId);
        progress.setScopeKey(buildScopeKey(normalizedMode, normalizedCategoryId));
        progress.setMode(normalizedMode);
        progress.setCategoryId(normalizedCategoryId);
        progress.setCurrentIndex(Math.max(request.currentIndex() == null ? 0 : request.currentIndex(), 0));
        progress.setCurrentQuestionId(request.currentQuestionId());
        progress.setQuestionIdsJson(toJson(request.questionIds() == null ? List.of() : request.questionIds()));
        progress.setDraftAnswersJson(toJson(request.drafts() == null ? Map.of() : request.drafts()));

        userPracticeProgressMapper.upsert(progress);
        return getProgress(userId, normalizedMode, normalizedCategoryId);
    }

    private Boolean judgeAnswer(Question question, String userAnswer) {
        if ("ANALYSIS".equalsIgnoreCase(question.getType()) || !StringUtils.hasText(question.getCorrectAnswer())) {
            return null;
        }
        String correctAnswer = normalizeAnswer(question.getCorrectAnswer());
        String submittedAnswer = normalizeAnswer(userAnswer);
        if (!StringUtils.hasText(submittedAnswer)) {
            return false;
        }
        return correctAnswer.equals(submittedAnswer);
    }

    private void validateSessionOwner(Long sessionId, Long userId, boolean admin) {
        if (sessionId == null || admin) {
            return;
        }
        PracticeSession session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Practice session not found");
        }
        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot submit answer to another user's session");
        }
    }

    private String normalizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        String value = answer.trim().toUpperCase(Locale.ROOT);
        String booleanValue = normalizeBooleanAnswer(value);
        if (booleanValue != null) {
            return booleanValue;
        }
        return Arrays.stream(value.split("[,;\\uFF0C\\uFF1B\\u3001\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String normalizeBooleanAnswer(String value) {
        return switch (value) {
            case "TRUE", "T", "1", "YES", "Y", "\u6B63\u786E", "\u5BF9" -> "TRUE";
            case "FALSE", "F", "0", "NO", "N", "\u9519\u8BEF", "\u9519" -> "FALSE";
            default -> null;
        };
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "PRACTICE";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WRONG", "WRONG_BOOK" -> "WRONG_BOOK";
            case "FAVORITE" -> "FAVORITE";
            default -> "PRACTICE";
        };
    }

    private Long normalizeCategoryId(Long categoryId) {
        return categoryId == null || categoryId <= 0 ? null : categoryId;
    }

    private String buildScopeKey(String mode, Long categoryId) {
        if ("PRACTICE".equals(mode) && categoryId != null) {
            return mode + ":CATEGORY:" + categoryId;
        }
        return mode + ":ALL";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid progress data");
        }
    }

    private PracticeProgressResponse toProgressResponse(UserPracticeProgress progress) {
        return new PracticeProgressResponse(
                progress.getId(),
                progress.getUserId(),
                progress.getMode(),
                progress.getCategoryId(),
                progress.getCurrentIndex(),
                progress.getCurrentQuestionId(),
                fromJson(progress.getQuestionIdsJson(), new TypeReference<List<Long>>() {}, List.of()),
                fromJson(
                        progress.getDraftAnswersJson(),
                        new TypeReference<Map<String, PracticeProgressDraft>>() {},
                        Map.of()
                ),
                progress.getUpdatedAt()
        );
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference, T defaultValue) {
        if (!StringUtils.hasText(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            return defaultValue;
        }
    }
}
