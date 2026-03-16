package com.beauty.knowledge.common.util;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;

public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof Number number) {
            return number.longValue();
        }
        if (principal instanceof UserDetails userDetails) {
            return parseUserId(userDetails.getUsername());
        }
        if (principal instanceof String principalStr) {
            return parseUserId(principalStr);
        }
        if (principal instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            if (userId == null) {
                userId = map.get("id");
            }
            if (userId == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "User id not found in authentication");
            }
            return parseUserId(String.valueOf(userId));
        }

        throw new BusinessException(ResultCode.UNAUTHORIZED, "Unsupported authentication principal");
    }

    public static String getCurrentUserRole() {
        Authentication authentication = getAuthentication();

        Object principal = authentication.getPrincipal();
        if (principal instanceof Map<?, ?> map && map.get("role") != null) {
            return String.valueOf(map.get("role"));
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            String authority = authorities.iterator().next().getAuthority();
            if (authority.startsWith("ROLE_")) {
                return authority.substring(5).toLowerCase();
            }
            return authority.toLowerCase();
        }

        throw new BusinessException(ResultCode.UNAUTHORIZED, "User role not found in authentication");
    }

    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(getCurrentUserRole());
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return authentication;
    }

    private static Long parseUserId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid user id in authentication");
        }
    }
}
