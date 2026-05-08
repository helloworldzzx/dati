package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.UserPracticeProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPracticeProgressMapper {

    @Select("""
            SELECT id, user_id, scope_key, mode, category_id, current_index,
                   current_question_id, question_ids_json, draft_answers_json,
                   created_at, updated_at
            FROM user_practice_progress
            WHERE user_id = #{userId}
              AND scope_key = #{scopeKey}
            """)
    UserPracticeProgress findByScope(@Param("userId") Long userId, @Param("scopeKey") String scopeKey);

    @Insert("""
            INSERT INTO user_practice_progress (
              user_id, scope_key, mode, category_id, current_index, current_question_id,
              question_ids_json, draft_answers_json
            ) VALUES (
              #{userId}, #{scopeKey}, #{mode}, #{categoryId}, #{currentIndex}, #{currentQuestionId},
              #{questionIdsJson}, #{draftAnswersJson}
            )
            ON DUPLICATE KEY UPDATE
              mode = #{mode},
              category_id = #{categoryId},
              current_index = #{currentIndex},
              current_question_id = #{currentQuestionId},
              question_ids_json = #{questionIdsJson},
              draft_answers_json = #{draftAnswersJson},
              updated_at = NOW()
            """)
    int upsert(UserPracticeProgress progress);
}
