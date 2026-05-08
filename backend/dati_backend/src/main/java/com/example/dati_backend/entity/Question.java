package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Question {
    private Long id;
    private Long categoryId;
    private String type;
    private String title;
    private String correctAnswer;
    private String analysis;
    private String sourceFile;
    private String status;
    private Integer answerCount;
    private Integer correctCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
