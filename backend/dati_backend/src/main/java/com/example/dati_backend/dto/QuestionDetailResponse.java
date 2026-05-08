package com.example.dati_backend.dto;

import com.example.dati_backend.entity.Question;
import com.example.dati_backend.entity.QuestionOption;
import java.util.List;

public record QuestionDetailResponse(
        Question question,
        List<QuestionOption> options
) {
}
