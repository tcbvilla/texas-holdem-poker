package com.poker.controller;

import com.poker.entity.User;
import com.poker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );
            
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", createUserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户注册失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.login(
                    request.getIdentifier(),
                    request.getPassword()
            );
            
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", createUserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("用户登录失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 从认证信息中获取当前用户ID
            Long currentUserId = getCurrentUserId();
            User user = userService.getUserById(currentUserId);
            
            response.put("success", true);
            response.put("data", createUserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long currentUserId = getCurrentUserId();
            User user = userService.updateUser(
                    currentUserId,
                    request.getUsername(),
                    request.getEmail(),
                    request.getAvatarUrl()
            );
            
            response.put("success", true);
            response.put("message", "用户信息更新成功");
            response.put("data", createUserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long currentUserId = getCurrentUserId();
            userService.changePassword(
                    currentUserId,
                    request.getOldPassword(),
                    request.getNewPassword()
            );
            
            response.put("success", true);
            response.put("message", "密码修改成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("修改密码失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists = userService.isUsernameExists(username);
            
            response.put("success", true);
            response.put("available", !exists);
            response.put("message", exists ? "用户名已存在" : "用户名可用");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查用户名失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists = userService.isEmailExists(email);
            
            response.put("success", true);
            response.put("available", !exists);
            response.put("message", exists ? "邮箱已存在" : "邮箱可用");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查邮箱失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建用户响应对象（不包含敏感信息）
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("avatarUrl", user.getAvatarUrl());
        userResponse.put("createdAt", user.getCreatedAt());
        userResponse.put("updatedAt", user.getUpdatedAt());
        return userResponse;
    }
    
    // TODO: 临时实现，后续需要集成认证系统
    private Long getCurrentUserId() {
        return 1L; // 临时返回固定用户ID
    }
    
    /**
     * 注册请求
     */
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * 登录请求
     */
    public static class LoginRequest {
        private String identifier; // 用户名或邮箱
        private String password;
        
        // Getters and Setters
        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * 更新资料请求
     */
    public static class UpdateProfileRequest {
        private String username;
        private String email;
        private String avatarUrl;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
    
    /**
     * 修改密码请求
     */
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
        
        // Getters and Setters
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
