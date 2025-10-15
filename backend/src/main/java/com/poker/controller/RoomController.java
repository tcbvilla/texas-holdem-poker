package com.poker.controller;

import com.poker.dto.RoomDto;
import com.poker.entity.Club;
import com.poker.entity.Room;
import com.poker.entity.User;
import com.poker.service.ClubService;
import com.poker.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 房间管理控制器
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    
    private final RoomService roomService;
    private final ClubService clubService;
    
    /**
     * 创建房间
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            Club club = clubService.getClubById(request.getClubId());
            
            // 检查用户是否有权限在此俱乐部创建房间
            if (!clubService.isClubMember(request.getClubId(), currentUser)) {
                throw new IllegalArgumentException("您不是该俱乐部的成员，无法创建房间");
            }
            
            Room room = roomService.createRoom(
                    request.getName(),
                    request.getDescription(),
                    club,
                    currentUser,
                    request.getSmallBlind(),
                    request.getBigBlind(),
                    request.getDefaultChips(),
                    request.getMinBuyin(),
                    request.getMaxBuyin(),
                    request.getMaxSeats(),
                    request.getDurationMinutes(),
                    request.getActionTimeSeconds()
            );
            
            response.put("success", true);
            response.put("message", "房间创建成功");
            response.put("data", convertToDto(room));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("创建房间失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据房间号获取房间
     */
    @GetMapping("/code/{roomCode}")
    public ResponseEntity<Map<String, Object>> getRoomByCode(@PathVariable String roomCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Room room = roomService.getRoomByCode(roomCode);
            
            response.put("success", true);
            response.put("data", convertToDto(room));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取房间失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据ID获取房间
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable Long roomId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Room room = roomService.getRoomById(roomId);
            
            response.put("success", true);
            response.put("data", convertToDto(room));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取房间失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取俱乐部的所有房间
     */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<Map<String, Object>> getRoomsByClub(@PathVariable Long clubId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Club club = clubService.getClubById(clubId);
            List<Room> rooms = roomService.getRoomsByClub(club);
            List<RoomDto> roomDtos = rooms.stream()
                    .map(room -> convertToDto(room))
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", roomDtos);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取俱乐部房间失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取等待中的房间
     */
    @GetMapping("/waiting")
    public ResponseEntity<Map<String, Object>> getWaitingRooms() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Room> rooms = roomService.getWaitingRooms();
            List<RoomDto> roomDtos = rooms.stream()
                    .map(room -> convertToDto(room))
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", roomDtos);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取等待中的房间失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 开始房间游戏
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<Map<String, Object>> startRoom(@PathVariable Long roomId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            roomService.startRoom(roomId, currentUser);
            
            response.put("success", true);
            response.put("message", "房间游戏已开始");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("开始房间游戏失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 结束房间游戏
     */
    @PostMapping("/{roomId}/end")
    public ResponseEntity<Map<String, Object>> endRoom(@PathVariable Long roomId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            roomService.endRoom(roomId, currentUser);
            
            response.put("success", true);
            response.put("message", "房间游戏已结束");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("结束房间游戏失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 取消房间
     */
    @PostMapping("/{roomId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelRoom(@PathVariable Long roomId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            roomService.cancelRoom(roomId, currentUser);
            
            response.put("success", true);
            response.put("message", "房间已取消");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("取消房间失败", e);
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
    
    /**
     * 创建房间请求
     */
    public static class CreateRoomRequest {
        private Long clubId;
        private String name;
        private String description;
        private Integer smallBlind;
        private Integer bigBlind;
        private Long defaultChips;
        private Long minBuyin;
        private Long maxBuyin;
        private Integer maxSeats;
        private Integer durationMinutes;
        private Integer actionTimeSeconds;
        
        // Getters and Setters
        public Long getClubId() { return clubId; }
        public void setClubId(Long clubId) { this.clubId = clubId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getSmallBlind() { return smallBlind; }
        public void setSmallBlind(Integer smallBlind) { this.smallBlind = smallBlind; }
        
        public Integer getBigBlind() { return bigBlind; }
        public void setBigBlind(Integer bigBlind) { this.bigBlind = bigBlind; }
        
        public Long getDefaultChips() { return defaultChips; }
        public void setDefaultChips(Long defaultChips) { this.defaultChips = defaultChips; }
        
        public Long getMinBuyin() { return minBuyin; }
        public void setMinBuyin(Long minBuyin) { this.minBuyin = minBuyin; }
        
        public Long getMaxBuyin() { return maxBuyin; }
        public void setMaxBuyin(Long maxBuyin) { this.maxBuyin = maxBuyin; }
        
        public Integer getMaxSeats() { return maxSeats; }
        public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }
        
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        
        public Integer getActionTimeSeconds() { return actionTimeSeconds; }
        public void setActionTimeSeconds(Integer actionTimeSeconds) { this.actionTimeSeconds = actionTimeSeconds; }
    }
    
    /**
     * 将Room实体转换为RoomDto
     */
    private RoomDto convertToDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setRoomCode(room.getRoomCode());
        dto.setName(room.getName());
        dto.setDescription(room.getDescription());
        dto.setSmallBlind(room.getSmallBlind());
        dto.setBigBlind(room.getBigBlind());
        dto.setDefaultChips(room.getDefaultChips());
        dto.setMinBuyin(room.getMinBuyin());
        dto.setMaxBuyin(room.getMaxBuyin());
        dto.setMaxSeats(room.getMaxSeats());
        dto.setDurationMinutes(room.getDurationMinutes());
        dto.setActionTimeSeconds(room.getActionTimeSeconds());
        dto.setStatus(room.getStatus().toString());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setStartedAt(room.getStartedAt());
        dto.setEndedAt(room.getEndedAt());
        
        // 设置俱乐部名称，避免循环引用
        if (room.getClub() != null) {
            dto.setClubName(room.getClub().getName());
        }
        
        // 设置创建者用户名，避免循环引用
        if (room.getCreatedBy() != null) {
            dto.setCreatedByName(room.getCreatedBy().getUsername());
        }
        
        return dto;
    }
}
