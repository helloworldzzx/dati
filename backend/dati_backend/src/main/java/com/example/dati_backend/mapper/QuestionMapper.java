package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.Question;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QuestionMapper {

    @Select("""
            SELECT id, category_id, type, title, correct_answer, analysis, source_file,
                   status, answer_count, correct_count, created_by, created_at, updated_at
            FROM question
            WHERE id = #{id}
            """)
    Question findById(Long id);

    @Select("""
            <script>
            SELECT id, category_id, type, title, correct_answer, analysis, source_file,
                   status, answer_count, correct_count, created_by, created_at, updated_at
            FROM question
            WHERE 1 = 1
              <if test="categoryId != null">AND category_id = #{categoryId}</if>
              <if test="type != null and type != ''">AND type = #{type}</if>
              <if test="status != null and status != ''">AND status = #{status}</if>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Question> list(
            @Param("categoryId") Long categoryId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            SELECT q.id, q.category_id, q.type, q.title, q.correct_answer, q.analysis, q.source_file,
                   q.status, q.answer_count, q.correct_count, q.created_by, q.created_at, q.updated_at
            FROM question q
            INNER JOIN user_question_stat s ON s.question_id = q.id
            WHERE s.user_id = #{userId}
              AND s.wrong_count > 0
              AND q.status = 'ENABLED'
            ORDER BY s.last_answered_at DESC, q.id DESC
            """)
    List<Question> listWrongByUser(Long userId);

    @Select("""
            SELECT q.id, q.category_id, q.type, q.title, q.correct_answer, q.analysis, q.source_file,
                   q.status, q.answer_count, q.correct_count, q.created_by, q.created_at, q.updated_at
            FROM question q
            INNER JOIN user_question_stat s ON s.question_id = q.id
            WHERE s.user_id = #{userId}
              AND s.is_favorite = TRUE
              AND q.status = 'ENABLED'
            ORDER BY s.favorite_at DESC, q.id DESC
            """)
    List<Question> listFavoriteByUser(Long userId);

    @Insert("""
            INSERT INTO question (
              category_id, type, title, correct_answer, analysis, source_file, status, created_by
            ) VALUES (
              #{categoryId}, #{type}, #{title}, #{correctAnswer}, #{analysis}, #{sourceFile}, #{status}, #{createdBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    @Update("""
            UPDATE question
            SET category_id = #{categoryId},
                type = #{type},
                title = #{title},
                correct_answer = #{correctAnswer},
                analysis = #{analysis},
                source_file = #{sourceFile},
                status = #{status}
            WHERE id = #{id}
            """)
    int update(Question question);

    @Update("""
            UPDATE question
            SET answer_count = answer_count + 1,
                correct_count = correct_count + CASE WHEN #{correct} = TRUE THEN 1 ELSE 0 END
            WHERE id = #{questionId}
            """)
    int increaseCounters(@Param("questionId") Long questionId, @Param("correct") Boolean correct);
}
