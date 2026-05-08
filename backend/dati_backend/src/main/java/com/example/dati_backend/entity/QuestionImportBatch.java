package com.example.dati_backend.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuestionImportBatch {
    private Long id;
    private String fileName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private Long importedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
