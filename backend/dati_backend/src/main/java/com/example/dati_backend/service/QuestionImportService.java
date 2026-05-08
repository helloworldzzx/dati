package com.example.dati_backend.service;

import com.example.dati_backend.entity.QuestionImportBatch;
import com.example.dati_backend.entity.QuestionImportError;
import com.example.dati_backend.mapper.QuestionImportBatchMapper;
import com.example.dati_backend.mapper.QuestionImportErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionImportService {
    private final QuestionImportBatchMapper batchMapper;
    private final QuestionImportErrorMapper errorMapper;

    @Transactional
    public QuestionImportBatch createBatch(String fileName, Long importedBy) {
        QuestionImportBatch batch = new QuestionImportBatch();
        batch.setFileName(fileName);
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        batch.setStatus("PROCESSING");
        batch.setImportedBy(importedBy);
        batchMapper.insert(batch);
        return batch;
    }

    @Transactional
    public void saveError(Long batchId, Integer rowNo, String message, String rawData) {
        QuestionImportError error = new QuestionImportError();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setErrorMessage(message);
        error.setRawData(rawData);
        errorMapper.insert(error);
    }
}
