package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.UserQuestionStat;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserQuestionStatMapper {

    @Select("""
            SELECT id, user_id, question_id, answer_count, correct_count, wrong_count,
                   last_answer, last_is_correct AS lastCorrect,
                   is_favorite AS favorite, favorite_at, last_answered_at,
                   created_at, updated_at
            FROM user_question_stat
            WHERE user_id = #{userId} AND question_id = #{questionId}
            """)
    UserQuestionStat findByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Select("""
            SELECT id, user_id, question_id, answer_count, correct_count, wrong_count,
                   last_answer, last_is_correct AS lastCorrect,
                   is_favorite AS favorite, favorite_at, last_answered_at,
                   created_at, updated_at
            FROM user_question_stat
            WHERE user_id = #{userId} AND wrong_count > 0
            ORDER BY last_answered_at DESC
            """)
    List<UserQuestionStat> listWrongByUser(Long userId);

    @Select("""
            SELECT id, user_id, question_id, answer_count, correct_count, wrong_count,
                   last_answer, last_is_correct AS lastCorrect,
                   is_favorite AS favorite, favorite_at, last_answered_at,
                   created_at, updated_at
            FROM user_question_stat
            WHERE user_id = #{userId} AND is_favorite = TRUE
            ORDER BY favorite_at DESC
            """)
    List<UserQuestionStat> listFavoriteByUser(Long userId);

    @Insert("""
            INSERT INTO user_question_stat (
              user_id, question_id, answer_count, correct_count, wrong_count,
              last_answer, last_is_correct, last_answered_at
            ) VALUES (
              #{userId}, #{questionId}, 1,
              CASE WHEN #{lastCorrect} = TRUE THEN 1 ELSE 0 END,
              CASE WHEN #{lastCorrect} = FALSE THEN 1 ELSE 0 END,
              #{lastAnswer}, #{lastCorrect}, NOW()
            )
            ON DUPLICATE KEY UPDATE
              answer_count = answer_count + 1,
              correct_count = correct_count + CASE WHEN #{lastCorrect} = TRUE THEN 1 ELSE 0 END,
              wrong_count = wrong_count + CASE WHEN #{lastCorrect} = FALSE THEN 1 ELSE 0 END,
              last_answer = #{lastAnswer},
              last_is_correct = #{lastCorrect},
              last_answered_at = NOW()
            """)
    int upsertAnswer(UserQuestionStat stat);

    @Insert("""
            INSERT INTO user_question_stat (user_id, question_id, is_favorite, favorite_at)
            VALUES (
              #{userId}, #{questionId}, #{favorite},
              CASE WHEN #{favorite} = TRUE THEN NOW() ELSE NULL END
            )
            ON DUPLICATE KEY UPDATE
              is_favorite = #{favorite},
              favorite_at = CASE
                WHEN #{favorite} = TRUE THEN COALESCE(favorite_at, NOW())
                ELSE NULL
              END
            """)
    int upsertFavorite(
            @Param("userId") Long userId,
            @Param("questionId") Long questionId,
            @Param("favorite") Boolean favorite
    );
}
