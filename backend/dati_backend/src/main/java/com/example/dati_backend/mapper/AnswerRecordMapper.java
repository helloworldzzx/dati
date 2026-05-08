package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.AnswerRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnswerRecordMapper {

    @Select("""
            SELECT id, session_id, user_id, question_id, user_answer,
                   is_correct AS correct, duration_seconds, answered_at
            FROM answer_record
            WHERE id = #{id}
            """)
    AnswerRecord findById(Long id);

    @Select("""
            SELECT id, session_id, user_id, question_id, user_answer,
                   is_correct AS correct, duration_seconds, answered_at
            FROM answer_record
            WHERE user_id = #{userId}
            ORDER BY answered_at DESC
            LIMIT #{limit}
            """)
    List<AnswerRecord> listRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Insert("""
            INSERT INTO answer_record (
              session_id, user_id, question_id, user_answer, is_correct, duration_seconds
            ) VALUES (
              #{sessionId}, #{userId}, #{questionId}, #{userAnswer}, #{correct}, #{durationSeconds}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnswerRecord record);
}
