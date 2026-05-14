package com.example.dati_backend;

import com.example.dati_backend.dto.PracticeProgressRequest;
import com.example.dati_backend.dto.PracticeProgressResponse;
import com.example.dati_backend.entity.UserPracticeProgress;
import com.example.dati_backend.mapper.UserPracticeProgressMapper;
import com.example.dati_backend.service.PracticeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatiBackendApplicationTests {
    private InMemoryProgressMapper progressMapper;
    private PracticeService practiceService;

    @BeforeEach
    void setUp() {
        progressMapper = new InMemoryProgressMapper();
        practiceService = new PracticeService(
                null,
                null,
                null,
                null,
                null,
                null,
                progressMapper,
                null,
                new ObjectMapper()
        );
    }

    @Test
    void practiceProgressDoesNotLeakBetweenAllQuestionsAndCategories() {
        practiceService.saveProgress(1L, progressRequest(null, 0, 100L));
        practiceService.saveProgress(1L, progressRequest(6L, 5, 106L));
        practiceService.saveProgress(1L, progressRequest(9L, 8, 209L));

        PracticeProgressResponse allProgress = practiceService.getProgress(1L, "PRACTICE", null);
        PracticeProgressResponse firstCategoryProgress = practiceService.getProgress(1L, "PRACTICE", 6L);
        PracticeProgressResponse secondCategoryProgress = practiceService.getProgress(1L, "PRACTICE", 9L);

        org.junit.jupiter.api.Assertions.assertNotNull(allProgress);
        org.junit.jupiter.api.Assertions.assertNotNull(firstCategoryProgress);
        org.junit.jupiter.api.Assertions.assertNotNull(secondCategoryProgress);
        org.junit.jupiter.api.Assertions.assertEquals(0, allProgress.currentIndex());
        org.junit.jupiter.api.Assertions.assertEquals(5, firstCategoryProgress.currentIndex());
        org.junit.jupiter.api.Assertions.assertEquals(8, secondCategoryProgress.currentIndex());
        org.junit.jupiter.api.Assertions.assertNull(allProgress.categoryId());
        org.junit.jupiter.api.Assertions.assertEquals(6L, firstCategoryProgress.categoryId());
        org.junit.jupiter.api.Assertions.assertEquals(9L, secondCategoryProgress.categoryId());
    }

    private PracticeProgressRequest progressRequest(Long categoryId, int currentIndex, Long questionId) {
        return new PracticeProgressRequest(
                "PRACTICE",
                categoryId,
                currentIndex,
                questionId,
                List.of(questionId),
                Map.of()
        );
    }

    private static final class InMemoryProgressMapper implements UserPracticeProgressMapper {
        private final AtomicLong ids = new AtomicLong(1);
        private final Map<String, UserPracticeProgress> rows = new LinkedHashMap<>();

        @Override
        public UserPracticeProgress findByScope(Long userId, String scopeKey) {
            return rows.get(key(userId, scopeKey));
        }

        @Override
        public List<UserPracticeProgress> listByUserAndMode(Long userId, String mode) {
            return rows.values()
                    .stream()
                    .filter((item) -> item.getUserId().equals(userId))
                    .filter((item) -> mode == null || mode.equals(item.getMode()))
                    .sorted(Comparator.comparing(UserPracticeProgress::getUpdatedAt).reversed())
                    .toList();
        }

        @Override
        public int upsert(UserPracticeProgress progress) {
            String key = key(progress.getUserId(), progress.getScopeKey());
            UserPracticeProgress saved = copy(progress);
            UserPracticeProgress existing = rows.get(key);
            LocalDateTime now = LocalDateTime.now();
            saved.setId(existing == null ? ids.getAndIncrement() : existing.getId());
            saved.setCreatedAt(existing == null ? now : existing.getCreatedAt());
            saved.setUpdatedAt(now);
            rows.put(key, saved);
            return 1;
        }

        private String key(Long userId, String scopeKey) {
            return userId + ":" + scopeKey;
        }

        private UserPracticeProgress copy(UserPracticeProgress source) {
            UserPracticeProgress target = new UserPracticeProgress();
            target.setId(source.getId());
            target.setUserId(source.getUserId());
            target.setScopeKey(source.getScopeKey());
            target.setMode(source.getMode());
            target.setCategoryId(source.getCategoryId());
            target.setCurrentIndex(source.getCurrentIndex());
            target.setCurrentQuestionId(source.getCurrentQuestionId());
            target.setQuestionIdsJson(source.getQuestionIdsJson());
            target.setDraftAnswersJson(source.getDraftAnswersJson());
            target.setCreatedAt(source.getCreatedAt());
            target.setUpdatedAt(source.getUpdatedAt());
            return target;
        }
    }
}
