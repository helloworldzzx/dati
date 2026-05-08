package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.PracticeSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PracticeSessionMapper {

    @Select("""
            SELECT id, user_id, category_id, mode, total_count, answered_count,
                   correct_count, started_at, finished_at
            FROM practice_session
            WHERE id = #{id}
            """)
    PracticeSession findById(Long id);

    @Insert("""
            INSERT INTO practice_session (user_id, category_id, mode, total_count)
            VALUES (#{userId}, #{categoryId}, #{mode}, #{totalCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PracticeSession session);

    @Update("""
            UPDATE practice_session
            SET answered_count = answered_count + 1,
                correct_count = correct_count + CASE WHEN #{correct} = TRUE THEN 1 ELSE 0 END
            WHERE id = #{sessionId}
            """)
    int increaseCounters(@Param("sessionId") Long sessionId, @Param("correct") Boolean correct);

    @Update("""
            UPDATE practice_session
            SET finished_at = NOW()
            WHERE id = #{sessionId}
            """)
    int finish(Long sessionId);
}
