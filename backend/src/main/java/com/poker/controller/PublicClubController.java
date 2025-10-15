package com.poker.controller;

import com.poker.dto.ClubDto;
import com.poker.entity.Club;
import com.poker.service.ClubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公开俱乐部控制器（所有用户都可以访问）
 */
@RestController
@RequestMapping("/api/public/clubs")
@RequiredArgsConstructor
@Slf4j
public class PublicClubController {
    
    private final ClubService clubService;
    
    /**
     * 获取所有俱乐部列表（公开）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllClubs() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Club> clubs = clubService.getAllClubs();
            List<ClubDto> clubDtos = clubs.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", clubDtos);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取俱乐部列表失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据ID获取俱乐部基本信息（公开）
     */
    @GetMapping("/{clubId}")
    public ResponseEntity<Map<String, Object>> getClubInfo(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Club club = clubService.getClubById(clubId);
            
            // 只返回基本信息，不包含成员列表等敏感信息
            Map<String, Object> clubInfo = new HashMap<>();
            clubInfo.put("id", club.getId());
            clubInfo.put("name", club.getName());
            clubInfo.put("description", club.getDescription());
            clubInfo.put("avatarUrl", club.getAvatarUrl());
            clubInfo.put("createdAt", club.getCreatedAt());
            
            response.put("success", true);
            response.put("data", clubInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取俱乐部信息失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 搜索俱乐部
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchClubs(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Club> clubs = clubService.searchClubs(keyword);
            
            response.put("success", true);
            response.put("data", clubs);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("搜索俱乐部失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 将Club实体转换为ClubDto
     */
    private ClubDto convertToDto(Club club) {
        ClubDto dto = new ClubDto();
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setDescription(club.getDescription());
        dto.setAvatarUrl(club.getAvatarUrl());
        dto.setSettings(club.getSettings());
        dto.setCreatedAt(club.getCreatedAt());
        dto.setUpdatedAt(club.getUpdatedAt());
        // 设置创建者用户名，避免循环引用
        if (club.getCreator() != null) {
            dto.setCreatorName(club.getCreator().getUsername());
        }
        return dto;
    }
}
