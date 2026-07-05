package com.poker.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存 Token 服务
 * 登录后颁发随机 Token，前端通过 Authorization: Bearer <token> 携带
 * 服务重启后 Token 失效，用户需重新登录
 */
@Component
@Slf4j
public class TokenService {

    private static final long TOKEN_TTL_SECONDS = 7 * 24 * 3600L;

    private static class TokenInfo {
        final Long userId;
        final Instant expiresAt;

        TokenInfo(Long userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    /**
     * 为用户颁发新 Token
     */
    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenInfo(userId, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        log.info("Issued token for user {}", userId);
        return token;
    }

    /**
     * 根据 Token 获取用户ID，无效或过期返回 null
     */
    public Long resolveUserId(String token) {
        return getUserId(token);
    }

    /**
     * 根据 Token 获取用户ID，无效或过期返回 null
     */
    public Long getUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        TokenInfo info = tokens.get(token);
        if (info == null) {
            return null;
        }
        if (info.expiresAt.isBefore(Instant.now())) {
            tokens.remove(token);
            return null;
        }
        return info.userId;
    }

    /**
     * 使 Token 失效（登出）
     */
    public void revokeToken(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
