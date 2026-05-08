package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.QuestionCategory;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QuestionCategoryMapper {

    @Select("""
            SELECT id, parent_id, name, level, sort_no, status, created_at, updated_at
            FROM question_category
            ORDER BY level ASC, sort_no ASC, id ASC
            """)
    List<QuestionCategory> listAll();

    @Select("""
            SELECT id, parent_id, name, level, sort_no, status, created_at, updated_at
            FROM question_category
            WHERE id = #{id}
            """)
    QuestionCategory findById(Long id);

    @Select("""
            SELECT id, parent_id, name, level, sort_no, status, created_at, updated_at
            FROM question_category
            WHERE parent_id <=> #{parentId}
            ORDER BY sort_no ASC, id ASC
            """)
    List<QuestionCategory> listByParentId(Long parentId);

    @Select("""
            SELECT id, parent_id, name, level, sort_no, status, created_at, updated_at
            FROM question_category
            WHERE parent_id <=> #{parentId}
              AND name = #{name}
            LIMIT 1
            """)
    QuestionCategory findByParentAndName(@Param("parentId") Long parentId, @Param("name") String name);

    @Insert("""
            INSERT INTO question_category (parent_id, name, level, sort_no, status)
            VALUES (#{parentId}, #{name}, #{level}, #{sortNo}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionCategory category);

    @Update("""
            UPDATE question_category
            SET parent_id = #{parentId},
                name = #{name},
                level = #{level},
                sort_no = #{sortNo},
                status = #{status}
            WHERE id = #{id}
            """)
    int update(QuestionCategory category);

    @Update("""
            UPDATE question_category
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
