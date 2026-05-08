package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.QuestionOption;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionOptionMapper {

    @Select("""
            SELECT id, question_id, option_key, option_content, is_correct AS correct, sort_no
            FROM question_option
            WHERE question_id = #{questionId}
            ORDER BY sort_no ASC, option_key ASC
            """)
    List<QuestionOption> listByQuestionId(Long questionId);

    @Insert("""
            INSERT INTO question_option (question_id, option_key, option_content, is_correct, sort_no)
            VALUES (#{questionId}, #{optionKey}, #{optionContent}, #{correct}, #{sortNo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionOption option);

    @Delete("""
            DELETE FROM question_option
            WHERE question_id = #{questionId}
            """)
    int deleteByQuestionId(Long questionId);
}
