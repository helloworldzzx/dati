package com.example.dati_backend.mapper;

import com.example.dati_backend.entity.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper {

    @Select("""
            SELECT id, username, phone, password_hash, real_name, role, status,
                   must_change_password, created_at, updated_at
            FROM sys_user
            ORDER BY id DESC
            """)
    List<SysUser> listAll();

    @Select("""
            SELECT id, username, phone, password_hash, real_name, role, status,
                   must_change_password, created_at, updated_at
            FROM sys_user
            WHERE id = #{id}
            """)
    SysUser findById(Long id);

    @Select("""
            SELECT id, username, phone, password_hash, real_name, role, status,
                   must_change_password, created_at, updated_at
            FROM sys_user
            WHERE username = #{account} OR phone = #{account}
            LIMIT 1
            """)
    SysUser findByAccount(String account);

    @Insert("""
            INSERT INTO sys_user (
              username, phone, password_hash, real_name, role, status, must_change_password
            ) VALUES (
              #{username}, #{phone}, #{passwordHash}, #{realName}, #{role}, #{status}, #{mustChangePassword}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("""
            UPDATE sys_user
            SET username = #{username},
                phone = #{phone},
                password_hash = #{passwordHash},
                real_name = #{realName},
                role = #{role},
                status = #{status},
                must_change_password = #{mustChangePassword}
            WHERE id = #{id}
            """)
    int update(SysUser user);

    @Update("""
            UPDATE sys_user
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("""
            UPDATE sys_user
            SET phone = #{phone},
                password_hash = #{passwordHash},
                must_change_password = #{mustChangePassword}
            WHERE id = #{id}
            """)
    int updatePhoneAndPassword(
            @Param("id") Long id,
            @Param("phone") String phone,
            @Param("passwordHash") String passwordHash,
            @Param("mustChangePassword") Boolean mustChangePassword
    );

    @Update("""
            UPDATE sys_user
            SET password_hash = #{passwordHash},
                must_change_password = #{mustChangePassword}
            WHERE id = #{id}
            """)
    int updatePassword(
            @Param("id") Long id,
            @Param("passwordHash") String passwordHash,
            @Param("mustChangePassword") Boolean mustChangePassword
    );
}
