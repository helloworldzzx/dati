package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuestionCategory {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortNo;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
