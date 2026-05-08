package com.example.dati_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserAnswerStat {
    private Long id;
    private Long userId;
    private Integer answerCount;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal accuracyRate;
    private Long totalDurationSeconds;
    private LocalDateTime lastAnsweredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
