package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserPracticeProgress {
    private Long id;
    private Long userId;
    private String scopeKey;
    private String mode;
    private Long categoryId;
    private Integer currentIndex;
    private Long currentQuestionId;
    private String questionIdsJson;
    private String draftAnswersJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
