package com.poker.auth;

import com.poker.entity.User;
import com.poker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录用户解析服务
 * 从当前请求的 Authorization: Bearer <token> 头中解析用户
 */
@Component
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserService userService;

    /**
     * 获取当前登录用户，未登录抛出异常
     */
    public User getCurrentUser() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("未登录或登录已过期，请重新登录");
        }
        return userService.getUserById(userId);
    }

    /**
     * 获取当前登录用户ID，未登录返回 null
     */
    public Long getCurrentUserIdOrNull() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return tokenService.getUserId(header.substring(7));
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
