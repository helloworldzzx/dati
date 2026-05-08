package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.QuestionImportBatch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QuestionImportBatchMapper {

    @Insert("""
            INSERT INTO question_import_batch (
              file_name, total_count, success_count, fail_count, status, imported_by
            ) VALUES (
              #{fileName}, #{totalCount}, #{successCount}, #{failCount}, #{status}, #{importedBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionImportBatch batch);

    @Update("""
            UPDATE question_import_batch
            SET total_count = #{totalCount},
                success_count = #{successCount},
                fail_count = #{failCount},
                status = #{status}
            WHERE id = #{id}
            """)
    int updateResult(QuestionImportBatch batch);

    @Update("""
            UPDATE question_import_batch
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
