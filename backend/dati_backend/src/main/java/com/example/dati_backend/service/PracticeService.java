package com.example.dati_backend.service;

import com.example.dati_backend.dto.FavoriteRequest;
import com.example.dati_backend.dto.PracticeSessionRequest;
import com.example.dati_backend.dto.SubmitAnswerRequest;
import com.example.dati_backend.entity.AnswerRecord;
import com.example.dati_backend.entity.PracticeSession;
import com.example.dati_backend.entity.Question;
import com.example.dati_backend.entity.UserQuestionStat;
import com.example.dati_backend.mapper.AnswerRecordMapper;
import com.example.dati_backend.mapper.PracticeSessionMapper;
import com.example.dati_backend.mapper.QuestionMapper;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import com.example.dati_backend.mapper.UserQuestionStatMapper;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PracticeService {
    private final PracticeSessionMapper sessionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final UserQuestionStatMapper userQuestionStatMapper;
    private final UserAnswerStatMapper userAnswerStatMapper;

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
        if (request == null || request.userId() == null || request.questionId() == null) {
            throw new IllegalArgumentException("User id and question id are required");
        }
        Question question = questionMapper.findById(request.questionId());
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }

        Boolean correct = request.correct() != null ? request.correct() : judgeAnswer(question, request.userAnswer());
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
}
