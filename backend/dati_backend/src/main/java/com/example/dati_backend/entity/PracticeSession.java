package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PracticeSession {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String mode;
    private Integer totalCount;
    private Integer answeredCount;
    private Integer correctCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
