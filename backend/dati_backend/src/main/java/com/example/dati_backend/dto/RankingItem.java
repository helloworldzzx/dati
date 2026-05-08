package com.example.dati_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RankingItem {
    private Long userId;
    private String username;
    private String realName;
    private Integer answerCount;
    private Integer correctCount;
    private Integer wrongCount;
    private BigDecimal accuracyRate;
    private Long totalDurationSeconds;
    private LocalDateTime lastAnsweredAt;
}
