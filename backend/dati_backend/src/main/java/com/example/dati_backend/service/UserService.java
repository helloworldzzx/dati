package com.example.dati_backend.service;

import com.example.dati_backend.dto.UserRequest;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.mapper.SysUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<SysUser> listUsers() {
        return userMapper.listAll();
    }

    public SysUser getUser(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    @Transactional
    public SysUser createUser(UserRequest request) {
        if (request == null || !StringUtils.hasText(request.username())) {
            throw new IllegalArgumentException("Username is required");
        }

        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(
                StringUtils.hasText(request.password()) ? request.password() : DEFAULT_PASSWORD
        ));
        user.setRealName(trimToNull(request.realName()));
        user.setRole(defaultText(request.role(), "USER").toUpperCase());
        user.setStatus(defaultText(request.status(), "ENABLED").toUpperCase());
        user.setMustChangePassword(true);
        userMapper.insert(user);
        return getUser(user.getId());
    }

    @Transactional
    public SysUser updateUser(Long id, UserRequest request) {
        SysUser user = getUser(id);
        if (request == null) {
            return user;
        }
        if (StringUtils.hasText(request.username())) {
            user.setUsername(request.username().trim());
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(false);
        }
        if (request.realName() != null) {
            user.setRealName(trimToNull(request.realName()));
        }
        if (StringUtils.hasText(request.role())) {
            user.setRole(request.role().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.status())) {
            user.setStatus(request.status().trim().toUpperCase());
        }
        userMapper.update(user);
        return getUser(id);
    }

    @Transactional
    public void disableUser(Long id) {
        getUser(id);
        userMapper.updateStatus(id, "DISABLED");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
