package com.example.dati_backend.config;

import com.example.dati_backend.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FirstLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SysUser user)) {
            return true;
        }
        if (!Boolean.TRUE.equals(user.getMustChangePassword()) || isAllowedPath(request.getRequestURI())) {
            return true;
        }

        response.setStatus(428);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Complete first login required\",\"data\":null}");
        return false;
    }

    private boolean isAllowedPath(String path) {
        return path.equals("/api/auth/me")
                || path.equals("/api/auth/complete-first-login")
                || path.equals("/api/auth/logout");
    }
}
