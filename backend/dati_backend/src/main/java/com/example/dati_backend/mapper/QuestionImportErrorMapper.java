package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.QuestionImportError;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface QuestionImportErrorMapper {

    @Insert("""
            INSERT INTO question_import_error (batch_id, row_no, error_message, raw_data)
            VALUES (#{batchId}, #{rowNo}, #{errorMessage}, #{rawData})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionImportError error);
}
