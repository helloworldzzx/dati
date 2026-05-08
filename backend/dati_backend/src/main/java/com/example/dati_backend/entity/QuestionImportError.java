package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuestionImportError {
    private Long id;
    private Long batchId;
    private Integer rowNo;
    private String errorMessage;
    private String rawData;
    private LocalDateTime createdAt;
}
