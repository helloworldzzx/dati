package com.example.dati_backend.config;

import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner bootstrapAdmin(
            @Value("${app.admin.bootstrap-enabled:true}") boolean enabled,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:123456}") String password,
            @Value("${app.admin.real-name:Administrator}") String realName
    ) {
        return args -> {
            if (!enabled || !StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                return;
            }
            if (userMapper.findByAccount(username.trim()) != null) {
                return;
            }

            SysUser admin = new SysUser();
            admin.setUsername(username.trim());
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRealName(realName);
            admin.setRole("ADMIN");
            admin.setStatus("ENABLED");
            admin.setMustChangePassword(true);
            userMapper.insert(admin);
        };
    }
}
