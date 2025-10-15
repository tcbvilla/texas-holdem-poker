package com.poker.service;

import com.poker.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    User register(String username, String email, String password);
    
    /**
     * 用户登录
     */
    User login(String identifier, String password);
    
    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 根据邮箱获取用户
     */
    User getUserByEmail(String email);
    
    /**
     * 根据用户名或邮箱获取用户
     */
    User getUserByUsernameOrEmail(String identifier);
    
    /**
     * 检查用户名是否存在
     */
    boolean isUsernameExists(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean isEmailExists(String email);
    
    /**
     * 更新用户信息
     */
    User updateUser(Long userId, String username, String email, String avatarUrl);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 获取所有用户
     */
    java.util.List<User> getAllUsers();
}
