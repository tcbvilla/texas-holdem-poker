package com.poker.auth;

/**
 * 未登录或 token 失效异常
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
