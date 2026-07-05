package com.poker.auth;

import com.poker.entity.User;

public final class AdminAuth {

    public static final String ADMIN_USERNAME = "admin";

    private AdminAuth() {
    }

    public static boolean isAdmin(User user) {
        return user != null && ADMIN_USERNAME.equals(user.getUsername());
    }

    public static void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new UnauthorizedException("需要管理员权限");
        }
    }
}
