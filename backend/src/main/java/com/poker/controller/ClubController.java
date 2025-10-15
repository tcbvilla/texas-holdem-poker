package com.poker.controller;

import com.poker.entity.Club;
import com.poker.entity.ClubMember;
import com.poker.entity.User;
import com.poker.service.ClubService;
import com.poker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 俱乐部管理控制器
 */
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Slf4j
public class ClubController {
    
    private final ClubService clubService;
    private final UserService userService;
    
    /**
     * 创建俱乐部
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClub(@RequestBody CreateClubRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 从认证信息中获取当前用户
            User currentUser = getCurrentUser(); // 临时实现
            
            Club club = clubService.createClub(
                    request.getName(),
                    request.getDescription(),
                    currentUser
            );
            
            response.put("success", true);
            response.put("message", "俱乐部创建成功");
            response.put("data", club);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取俱乐部详情
     */
    @GetMapping("/{clubId}")
    public ResponseEntity<Map<String, Object>> getClub(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Club club = clubService.getClubById(clubId);
            
            response.put("success", true);
            response.put("data", club);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户创建的俱乐部
     */
    @GetMapping("/my-created")
    public ResponseEntity<Map<String, Object>> getMyCreatedClubs() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            List<Club> clubs = clubService.getClubsByCreator(currentUser);
            
            response.put("success", true);
            response.put("data", clubs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户创建的俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户加入的俱乐部
     */
    @GetMapping("/my-joined")
    public ResponseEntity<Map<String, Object>> getMyJoinedClubs() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            List<Club> clubs = clubService.getClubsByMember(currentUser);
            
            response.put("success", true);
            response.put("data", clubs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户加入的俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 邀请成员加入俱乐部
     */
    @PostMapping("/{clubId}/members")
    public ResponseEntity<Map<String, Object>> inviteMember(@PathVariable Long clubId, 
                                                           @RequestBody InviteMemberRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            User targetUser = getUserById(request.getUserId()); // TODO: 实现用户查找
            
            ClubMember member = clubService.inviteMember(clubId, targetUser, currentUser);
            
            response.put("success", true);
            response.put("message", "成员邀请成功");
            response.put("data", member);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("邀请成员失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 移除俱乐部成员
     */
    @DeleteMapping("/{clubId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMember(@PathVariable Long clubId, 
                                                           @PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            clubService.removeMember(clubId, userId, currentUser);
            
            response.put("success", true);
            response.put("message", "成员移除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("移除成员失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除俱乐部
     */
    @DeleteMapping("/{clubId}")
    public ResponseEntity<Map<String, Object>> deleteClub(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            clubService.deleteClub(clubId, currentUser);
            
            response.put("success", true);
            response.put("message", "俱乐部删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("删除俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取俱乐部成员列表
     */
    @GetMapping("/{clubId}/members")
    public ResponseEntity<Map<String, Object>> getClubMembers(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ClubMember> members = clubService.getClubMembers(clubId);
            
            response.put("success", true);
            response.put("data", members);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取俱乐部成员失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // TODO: 临时实现，后续需要集成认证系统
    private User getCurrentUser() {
        // 临时返回第一个用户，后续需要从认证信息中获取
        return userService.getAllUsers().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("没有找到用户，请先注册"));
    }
    
    private User getUserById(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setEmail("user" + userId + "@example.com");
        return user;
    }
    
    /**
     * 创建俱乐部请求
     */
    public static class CreateClubRequest {
        private String name;
        private String description;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 邀请成员请求
     */
    public static class InviteMemberRequest {
        private Long userId;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
