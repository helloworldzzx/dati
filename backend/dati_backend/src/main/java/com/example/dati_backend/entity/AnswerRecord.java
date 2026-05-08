package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AnswerRecord {
    private Long id;
    private Long sessionId;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    private Boolean correct;
    private Integer durationSeconds;
    private LocalDateTime answeredAt;
}
