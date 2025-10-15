package com.poker.controller;

import com.poker.entity.Club;
import com.poker.entity.ClubMember;
import com.poker.entity.User;
import com.poker.service.ClubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 俱乐部加入控制器
 */
@RestController
@RequestMapping("/api/club-join")
@RequiredArgsConstructor
@Slf4j
public class ClubJoinController {
    
    private final ClubService clubService;
    
    /**
     * 申请加入俱乐部
     */
    @PostMapping("/{clubId}/apply")
    public ResponseEntity<Map<String, Object>> applyToJoinClub(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            Club club = clubService.getClubById(clubId);
            
            // 检查用户是否已经是成员
            if (clubService.isClubMember(clubId, currentUser)) {
                throw new IllegalArgumentException("您已经是该俱乐部的成员");
            }
            
            // 用户申请加入俱乐部
            ClubMember member = clubService.applyToJoinClub(clubId, currentUser);
            
            response.put("success", true);
            response.put("message", "成功加入俱乐部");
            response.put("data", member);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("申请加入俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 退出俱乐部
     */
    @PostMapping("/{clubId}/leave")
    public ResponseEntity<Map<String, Object>> leaveClub(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            
            // 检查用户是否是成员
            if (!clubService.isClubMember(clubId, currentUser)) {
                throw new IllegalArgumentException("您不是该俱乐部的成员");
            }
            
            // 用户退出俱乐部
            clubService.leaveClub(clubId, currentUser);
            
            response.put("success", true);
            response.put("message", "成功退出俱乐部");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("退出俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查用户是否已加入俱乐部
     */
    @GetMapping("/{clubId}/status")
    public ResponseEntity<Map<String, Object>> getJoinStatus(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            boolean isMember = clubService.isClubMember(clubId, currentUser);
            boolean isAdmin = clubService.isClubAdmin(clubId, currentUser);
            
            Map<String, Object> status = new HashMap<>();
            status.put("isMember", isMember);
            status.put("isAdmin", isAdmin);
            status.put("canJoin", !isMember);
            
            response.put("success", true);
            response.put("data", status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取加入状态失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // TODO: 临时实现，后续需要集成认证系统
    private User getCurrentUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }
}
