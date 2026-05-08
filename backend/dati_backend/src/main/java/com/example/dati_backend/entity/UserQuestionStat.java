package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserQuestionStat {
    private Long id;
    private Long userId;
    private Long questionId;
    private Integer answerCount;
    private Integer correctCount;
    private Integer wrongCount;
    private String lastAnswer;
    private Boolean lastCorrect;
    private Boolean favorite;
    private LocalDateTime favoriteAt;
    private LocalDateTime lastAnsweredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
