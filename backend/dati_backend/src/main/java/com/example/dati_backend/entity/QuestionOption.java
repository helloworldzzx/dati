package com.example.dati_backend.entity;

import lombok.Data;

@Data
public class QuestionOption {
    private Long id;
    private Long questionId;
    private String optionKey;
    private String optionContent;
    private Boolean correct;
    private Integer sortNo;
}
