package com.example.dati_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.dati_backend.dto.QuestionContentResponse;
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
import java.time.Duration;
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
    private static final Duration QUESTION_DETAIL_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration QUESTION_LIST_CACHE_TTL = Duration.ofMinutes(1);
    private static final String QUESTION_DETAIL_CACHE_PREFIX = "dati:question:detail:";
    private static final String QUESTION_LIST_CACHE_PREFIX = "dati:question:list:";

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final UserQuestionStatMapper userQuestionStatMapper;
    private final RedisJsonCacheService cacheService;

    public QuestionDetailResponse getQuestionDetail(Long id) {
        return getQuestionDetail(id, null);
    }

    public QuestionDetailResponse getQuestionDetail(Long id, Long userId) {
        QuestionContentResponse content = getQuestionContent(id);
        return new QuestionDetailResponse(
                content.question(),
                content.options(),
                userId == null ? null : userQuestionStatMapper.findByUserAndQuestion(userId, id)
        );
    }

    public QuestionContentResponse getQuestionContent(Long id) {
        String cacheKey = questionDetailCacheKey(id);
        TypeReference<QuestionContentResponse> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    Question question = getQuestion(id);
                    QuestionContentResponse content = new QuestionContentResponse(
                            question,
                            optionMapper.listByQuestionId(id)
                    );
                    cacheService.set(cacheKey, content, QUESTION_DETAIL_CACHE_TTL);
                    return content;
                });
    }

    public Question getQuestionForAnswer(Long id) {
        return getQuestionContent(id).question();
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
        String safeType = trimToNull(type);
        String safeStatus = trimToNull(status);
        String cacheKey = questionListCacheKey(categoryId, safeType, safeStatus, safePage, safeSize);
        TypeReference<List<Question>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    List<Question> questions = questionMapper.list(categoryId, safeType, safeStatus, safeSize, (safePage - 1) * safeSize);
                    cacheService.set(cacheKey, questions, QUESTION_LIST_CACHE_TTL);
                    return questions;
                });
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

    public List<Question> listWrongQuestions(Long userId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        return questionMapper.listWrongByUser(userId, safeSize, (safePage - 1) * safeSize);
    }

    public PageResult<Question> pageWrongQuestions(Long userId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        return new PageResult<>(
                questionMapper.listWrongByUser(userId, safeSize, (safePage - 1) * safeSize),
                questionMapper.countWrongByUser(userId),
                safePage,
                safeSize
        );
    }

    public List<Question> listFavoriteQuestions(Long userId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        return questionMapper.listFavoriteByUser(userId, safeSize, (safePage - 1) * safeSize);
    }

    public PageResult<Question> pageFavoriteQuestions(Long userId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        return new PageResult<>(
                questionMapper.listFavoriteByUser(userId, safeSize, (safePage - 1) * safeSize),
                questionMapper.countFavoriteByUser(userId),
                safePage,
                safeSize
        );
    }

    @Transactional
    public void deleteQuestion(Long id) {
        getQuestion(id);
        questionMapper.deleteById(id);
        clearQuestionCache(id);
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
        safeIds.forEach(this::clearQuestionDetailCache);
        clearQuestionListCache();
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
        clearQuestionListCache();
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
        clearQuestionCache(id);
        return getQuestionDetail(id);
    }

    private void clearQuestionCache(Long questionId) {
        clearQuestionDetailCache(questionId);
        clearQuestionListCache();
    }

    private void clearQuestionDetailCache(Long questionId) {
        cacheService.delete(questionDetailCacheKey(questionId));
    }

    private void clearQuestionListCache() {
        cacheService.deleteByPattern(QUESTION_LIST_CACHE_PREFIX + "*");
    }

    private String questionDetailCacheKey(Long id) {
        return QUESTION_DETAIL_CACHE_PREFIX + id;
    }

    private String questionListCacheKey(Long categoryId, String type, String status, int page, int size) {
        return QUESTION_LIST_CACHE_PREFIX
                + "category:" + (categoryId == null ? "all" : categoryId)
                + ":type:" + (type == null ? "all" : type)
                + ":status:" + (status == null ? "all" : status)
                + ":page:" + page
                + ":size:" + size;
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
