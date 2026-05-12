package com.example.dati_backend.service;

import com.example.dati_backend.dto.QuestionDetailResponse;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.dto.QuestionOptionRequest;
import com.example.dati_backend.dto.QuestionRequest;
import com.example.dati_backend.entity.Question;
import com.example.dati_backend.entity.QuestionOption;
import com.example.dati_backend.mapper.QuestionCategoryMapper;
import com.example.dati_backend.mapper.QuestionMapper;
import com.example.dati_backend.mapper.QuestionOptionMapper;
import com.example.dati_backend.mapper.UserQuestionStatMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final UserQuestionStatMapper userQuestionStatMapper;

    public QuestionDetailResponse getQuestionDetail(Long id) {
        return getQuestionDetail(id, null);
    }

    public QuestionDetailResponse getQuestionDetail(Long id, Long userId) {
        Question question = getQuestion(id);
        return new QuestionDetailResponse(
                question,
                optionMapper.listByQuestionId(id),
                userId == null ? null : userQuestionStatMapper.findByUserAndQuestion(userId, id)
        );
    }

    public Question getQuestion(Long id) {
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new IllegalArgumentException("Question not found");
        }
        return question;
    }

    public List<Question> listQuestions(Long categoryId, String type, String status, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 2000);
        return questionMapper.list(categoryId, trimToNull(type), trimToNull(status), safeSize, (safePage - 1) * safeSize);
    }

    public PageResult<Question> pageQuestions(Long categoryId, String type, String status, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        String safeType = trimToNull(type);
        String safeStatus = trimToNull(status);
        return new PageResult<>(
                questionMapper.list(categoryId, safeType, safeStatus, safeSize, (safePage - 1) * safeSize),
                questionMapper.count(categoryId, safeType, safeStatus),
                safePage,
                safeSize
        );
    }

    public List<Question> listWrongQuestions(Long userId) {
        return questionMapper.listWrongByUser(userId);
    }

    public List<Question> listFavoriteQuestions(Long userId) {
        return questionMapper.listFavoriteByUser(userId);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        getQuestion(id);
        questionMapper.deleteById(id);
    }

    @Transactional
    public void deleteQuestions(List<Long> ids) {
        List<Long> safeIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (safeIds.isEmpty()) {
            throw new IllegalArgumentException("Question ids are required");
        }
        questionMapper.deleteBatch(safeIds);
    }

    @Transactional
    public QuestionDetailResponse createQuestion(QuestionRequest request) {
        if (request == null || request.categoryId() == null || !StringUtils.hasText(request.title())) {
            throw new IllegalArgumentException("Question category and title are required");
        }
        if (categoryMapper.findById(request.categoryId()) == null) {
            throw new IllegalArgumentException("Category not found");
        }

        Question question = new Question();
        question.setCategoryId(request.categoryId());
        question.setType(defaultText(request.type(), "SINGLE").toUpperCase());
        question.setTitle(request.title().trim());
        question.setCorrectAnswer(resolveCorrectAnswer(request.correctAnswer(), request.options()));
        question.setAnalysis(trimToNull(request.analysis()));
        question.setSourceFile(trimToNull(request.sourceFile()));
        question.setStatus(defaultText(request.status(), "ENABLED").toUpperCase());
        question.setCreatedBy(request.createdBy());
        questionMapper.insert(question);
        saveOptions(question.getId(), request.options());
        return getQuestionDetail(question.getId());
    }

    @Transactional
    public QuestionDetailResponse updateQuestion(Long id, QuestionRequest request) {
        Question question = getQuestion(id);
        if (request == null) {
            return getQuestionDetail(id);
        }
        if (request.categoryId() != null) {
            if (categoryMapper.findById(request.categoryId()) == null) {
                throw new IllegalArgumentException("Category not found");
            }
            question.setCategoryId(request.categoryId());
        }
        if (StringUtils.hasText(request.type())) {
            question.setType(request.type().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.title())) {
            question.setTitle(request.title().trim());
        }
        if (request.correctAnswer() != null || request.options() != null) {
            question.setCorrectAnswer(resolveCorrectAnswer(request.correctAnswer(), request.options()));
        }
        if (request.analysis() != null) {
            question.setAnalysis(trimToNull(request.analysis()));
        }
        if (request.sourceFile() != null) {
            question.setSourceFile(trimToNull(request.sourceFile()));
        }
        if (StringUtils.hasText(request.status())) {
            question.setStatus(request.status().trim().toUpperCase());
        }
        questionMapper.update(question);

        if (request.options() != null) {
            optionMapper.deleteByQuestionId(id);
            saveOptions(id, request.options());
        }
        return getQuestionDetail(id);
    }

    private void saveOptions(Long questionId, List<QuestionOptionRequest> optionRequests) {
        if (optionRequests == null || optionRequests.isEmpty()) {
            return;
        }
        for (int i = 0; i < optionRequests.size(); i++) {
            QuestionOptionRequest request = optionRequests.get(i);
            if (!StringUtils.hasText(request.optionKey()) || !StringUtils.hasText(request.optionContent())) {
                continue;
            }
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(request.optionKey().trim().toUpperCase());
            option.setOptionContent(request.optionContent().trim());
            option.setCorrect(Boolean.TRUE.equals(request.correct()));
            option.setSortNo(request.sortNo() == null ? i : request.sortNo());
            optionMapper.insert(option);
        }
    }

    private String resolveCorrectAnswer(String correctAnswer, List<QuestionOptionRequest> options) {
        if (StringUtils.hasText(correctAnswer)) {
            return correctAnswer.trim().toUpperCase();
        }
        if (options == null) {
            return null;
        }
        String answer = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.correct()))
                .sorted(Comparator.comparing(option -> option.sortNo() == null ? 0 : option.sortNo()))
                .map(QuestionOptionRequest::optionKey)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase())
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
        return StringUtils.hasText(answer) ? answer : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
