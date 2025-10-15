package com.poker.service.impl;

import com.poker.entity.User;
import com.poker.repository.UserRepository;
import com.poker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public User register(String username, String email, String password) {
        log.info("用户注册: username={}, email={}", username, email);
        
        // 验证参数
        validateRegistrationParams(username, email, password);
        
        // 检查用户名是否已存在
        if (isUsernameExists(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (isEmailExists(email)) {
            throw new IllegalArgumentException("邮箱已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        user = userRepository.save(user);
        
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }
    
    @Override
    @Transactional(readOnly = true)
    public User login(String identifier, String password) {
        log.info("用户登录: identifier={}", identifier);
        
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名或邮箱不能为空");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // 查找用户
        User user = getUserByUsernameOrEmail(identifier);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }
        
        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserByUsernameOrEmail(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    public User updateUser(Long userId, String username, String email, String avatarUrl) {
        log.info("更新用户信息: userId={}, username={}, email={}", userId, username, email);
        
        User user = getUserById(userId);
        
        // 检查用户名是否被其他用户使用
        if (username != null && !username.equals(user.getUsername())) {
            if (isUsernameExists(username)) {
                throw new IllegalArgumentException("用户名已存在");
            }
            user.setUsername(username);
        }
        
        // 检查邮箱是否被其他用户使用
        if (email != null && !email.equals(user.getEmail())) {
            if (isEmailExists(email)) {
                throw new IllegalArgumentException("邮箱已存在");
            }
            user.setEmail(email);
        }
        
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        user = userRepository.save(user);
        
        log.info("用户信息更新成功: userId={}", userId);
        return user;
    }
    
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("修改密码: userId={}", userId);
        
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("原密码不能为空");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        
        User user = getUserById(userId);
        
        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }
        
        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("密码修改成功: userId={}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 验证注册参数
     */
    private void validateRegistrationParams(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        
        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("用户名长度必须在3-20位之间");
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("用户名只能包含字母、数字和下划线");
        }
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
    }
}
