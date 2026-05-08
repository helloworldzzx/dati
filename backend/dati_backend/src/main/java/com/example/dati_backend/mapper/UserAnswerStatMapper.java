package com.example.dati_backend.mapper;

import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.entity.UserAnswerStat;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAnswerStatMapper {

    @Select("""
            SELECT id, user_id, answer_count, correct_count, wrong_count,
                   accuracy_rate, total_duration_seconds, last_answered_at,
                   created_at, updated_at
            FROM user_answer_stat
            WHERE user_id = #{userId}
            """)
    UserAnswerStat findByUserId(Long userId);

    @Select("""
            SELECT s.user_id AS userId,
                   u.username AS username,
                   u.real_name AS realName,
                   s.answer_count AS answerCount,
                   s.correct_count AS correctCount,
                   s.wrong_count AS wrongCount,
                   s.accuracy_rate AS accuracyRate,
                   s.total_duration_seconds AS totalDurationSeconds,
                   s.last_answered_at AS lastAnsweredAt
            FROM user_answer_stat s
            INNER JOIN sys_user u ON u.id = s.user_id
            WHERE u.status = 'ENABLED'
            ORDER BY s.correct_count DESC,
                     s.answer_count DESC,
                     s.accuracy_rate DESC,
                     s.last_answered_at ASC
            LIMIT #{limit}
            """)
    List<RankingItem> listRanking(int limit);

    @Select("""
            SELECT s.user_id AS userId,
                   u.username AS username,
                   u.real_name AS realName,
                   s.answer_count AS answerCount,
                   s.correct_count AS correctCount,
                   s.wrong_count AS wrongCount,
                   s.accuracy_rate AS accuracyRate,
                   s.total_duration_seconds AS totalDurationSeconds,
                   s.last_answered_at AS lastAnsweredAt
            FROM user_answer_stat s
            INNER JOIN sys_user u ON u.id = s.user_id
            WHERE u.status = 'ENABLED'
            ORDER BY s.answer_count DESC,
                     s.accuracy_rate DESC,
                     s.correct_count DESC,
                     s.last_answered_at ASC
            LIMIT #{limit}
            """)
    List<RankingItem> listRankingByAnswerCount(int limit);

    @Select("""
            SELECT s.user_id AS userId,
                   u.username AS username,
                   u.real_name AS realName,
                   s.answer_count AS answerCount,
                   s.correct_count AS correctCount,
                   s.wrong_count AS wrongCount,
                   s.accuracy_rate AS accuracyRate,
                   s.total_duration_seconds AS totalDurationSeconds,
                   s.last_answered_at AS lastAnsweredAt
            FROM user_answer_stat s
            INNER JOIN sys_user u ON u.id = s.user_id
            WHERE u.status = 'ENABLED'
            ORDER BY s.accuracy_rate DESC,
                     s.answer_count DESC,
                     s.correct_count DESC,
                     s.last_answered_at ASC
            LIMIT #{limit}
            """)
    List<RankingItem> listRankingByAccuracy(int limit);

    @Insert("""
            INSERT INTO user_answer_stat (
              user_id, answer_count, correct_count, wrong_count, accuracy_rate,
              total_duration_seconds, last_answered_at
            ) VALUES (
              #{userId}, 1,
              CASE WHEN #{correct} = TRUE THEN 1 ELSE 0 END,
              CASE WHEN #{correct} = FALSE THEN 1 ELSE 0 END,
              CASE WHEN #{correct} = TRUE THEN 100.00 ELSE 0.00 END,
              #{durationSeconds}, NOW()
            )
            ON DUPLICATE KEY UPDATE
              accuracy_rate = ROUND(
                (correct_count + CASE WHEN #{correct} = TRUE THEN 1 ELSE 0 END) * 100 / (answer_count + 1),
                2
              ),
              answer_count = answer_count + 1,
              correct_count = correct_count + CASE WHEN #{correct} = TRUE THEN 1 ELSE 0 END,
              wrong_count = wrong_count + CASE WHEN #{correct} = FALSE THEN 1 ELSE 0 END,
              total_duration_seconds = total_duration_seconds + #{durationSeconds},
              last_answered_at = NOW()
            """)
    int upsertAnswer(
            @Param("userId") Long userId,
            @Param("correct") Boolean correct,
            @Param("durationSeconds") int durationSeconds
    );
}
