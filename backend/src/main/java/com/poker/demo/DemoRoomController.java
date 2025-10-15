package com.poker.demo;

import com.poker.game.core.GameEngine;
import com.poker.game.core.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内存房间管理控制器（演示用）
 * 提供房间创建、成员管理、游戏控制等API
 */
@RestController
@RequestMapping("/api/demo/room")
@RequiredArgsConstructor
@Slf4j
public class DemoRoomController {

    private final ObjectProvider<GameRoom> gameRoomProvider;
    private final Map<String, GameRoom> activeRooms = new HashMap<>();

    /**
     * 创建房间
     */
    @PostMapping("/create")
    public Map<String, Object> createRoom() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = gameRoomProvider.getObject();
            activeRooms.put(room.getRoomId(), room);
            
            result.put("success", true);
            result.put("message", "房间创建成功");
            result.put("roomId", room.getRoomId());
            result.put("roomInfo", room.getRoomInfo());
            
            log.info("创建房间：{}", room.getRoomId());
            
        } catch (Exception e) {
            log.error("创建房间失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取房间信息
     */
    @GetMapping("/{roomId}/info")
    public Map<String, Object> getRoomInfo(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            result.put("success", true);
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("获取房间信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 加入房间
     */
    @PostMapping("/{roomId}/join")
    public Map<String, Object> joinRoom(
            @PathVariable String roomId,
            @RequestParam int playerId,
            @RequestParam String playerName,
            @RequestParam(defaultValue = "1000") BigDecimal initialChips) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.joinRoom(playerId, playerName, initialChips);
            
            result.put("success", true);
            result.put("message", "加入房间成功");
            result.put("roomInfo", room.getRoomInfo());
            
            log.info("玩家{}加入房间{}", playerName, roomId);
            
        } catch (Exception e) {
            log.error("加入房间失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 入座
     */
    @PostMapping("/{roomId}/seat")
    public Map<String, Object> takeSeat(
            @PathVariable String roomId,
            @RequestParam int playerId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.takeSeat(playerId);
            
            result.put("success", true);
            result.put("message", "入座成功");
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("入座失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 离开座位
     */
    @PostMapping("/{roomId}/leave-seat")
    public Map<String, Object> leaveSeat(
            @PathVariable String roomId,
            @RequestParam int playerId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.leaveSeat(playerId);
            
            result.put("success", true);
            result.put("message", "离开座位成功");
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("离开座位失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 离开房间
     */
    @PostMapping("/{roomId}/leave")
    public Map<String, Object> leaveRoom(
            @PathVariable String roomId,
            @RequestParam int playerId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.leaveRoom(playerId);
            
            result.put("success", true);
            result.put("message", "离开房间成功");
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("离开房间失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 开始新游戏
     */
    @PostMapping("/{roomId}/start-game")
    public Map<String, Object> startNewGame(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") int smallBlind,
            @RequestParam(defaultValue = "20") int bigBlind) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.startNewGame(smallBlind, bigBlind);
            
            result.put("success", true);
            result.put("message", "游戏开始成功");
            result.put("gameInfo", room.getCurrentGameInfo());
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("开始游戏失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取当前游戏状态
     */
    @GetMapping("/{roomId}/game/status")
    public Map<String, Object> getGameStatus(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            GameEngine.GameInfo gameInfo = room.getCurrentGameInfo();
            if (gameInfo == null) {
                result.put("success", false);
                result.put("error", "没有进行中的游戏");
                return result;
            }
            
            result.put("success", true);
            result.put("gameInfo", gameInfo);
            result.put("canStartNextHand", room.canStartNextHand());
            
        } catch (Exception e) {
            log.error("获取游戏状态失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 处理玩家行动
     */
    @PostMapping("/{roomId}/game/action")
    public Map<String, Object> processPlayerAction(
            @PathVariable String roomId,
            @RequestParam int playerId,
            @RequestParam String action,
            @RequestParam(required = false) BigDecimal amount) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.processPlayerAction(playerId, action, amount);
            
            result.put("success", true);
            result.put("message", "行动处理成功");
            result.put("gameInfo", room.getCurrentGameInfo());
            
        } catch (Exception e) {
            log.error("处理玩家行动失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 开始下一局
     */
    @PostMapping("/{roomId}/game/next-hand")
    public Map<String, Object> startNextHand(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            if (!room.canStartNextHand()) {
                result.put("success", false);
                result.put("error", "当前无法开始下一局");
                return result;
            }
            
            room.startNextHand();
            
            result.put("success", true);
            result.put("message", "开始下一局成功");
            result.put("gameInfo", room.getCurrentGameInfo());
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("开始下一局失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 补码
     */
    @PostMapping("/{roomId}/rebuy")
    public Map<String, Object> rebuy(
            @PathVariable String roomId,
            @RequestParam int playerId,
            @RequestParam BigDecimal amount) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.rebuy(playerId, amount);
            
            result.put("success", true);
            result.put("message", "补码成功，将在下一局开始时生效");
            result.put("roomInfo", room.getRoomInfo());
            
        } catch (Exception e) {
            log.error("补码失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 关闭房间
     */
    @PostMapping("/{roomId}/close")
    public Map<String, Object> closeRoom(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GameRoom room = activeRooms.get(roomId);
            if (room == null) {
                result.put("success", false);
                result.put("error", "房间不存在");
                return result;
            }
            
            room.closeRoom();
            activeRooms.remove(roomId);
            
            result.put("success", true);
            result.put("message", "房间已关闭");
            
        } catch (Exception e) {
            log.error("关闭房间失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取所有活跃房间列表
     */
    @GetMapping("/list")
    public Map<String, Object> getActiveRooms() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> rooms = activeRooms.values().stream()
                    .map(room -> {
                        Map<String, Object> roomSummary = new HashMap<>();
                        roomSummary.put("roomId", room.getRoomId());
                        roomSummary.put("memberCount", room.getRoomMembers().size());
                        roomSummary.put("seatedCount", room.getSeatedMembers().size());
                        roomSummary.put("gameInProgress", room.isGameInProgress());
                        return roomSummary;
                    })
                    .collect(Collectors.toList());
            
            result.put("success", true);
            result.put("rooms", rooms);
            result.put("totalCount", rooms.size());
            
        } catch (Exception e) {
            log.error("获取房间列表失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 创建测试房间（快速创建带玩家的房间）
     */
    @PostMapping("/create-test")
    public Map<String, Object> createTestRoom(
            @RequestParam(defaultValue = "10") int smallBlind,
            @RequestParam(defaultValue = "20") int bigBlind) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 创建房间
            GameRoom room = gameRoomProvider.getObject();
            activeRooms.put(room.getRoomId(), room);
            
            // 添加测试玩家
            room.joinRoom(1, "玩家1", BigDecimal.valueOf(1000));
            room.joinRoom(2, "玩家2", BigDecimal.valueOf(1000));
            room.joinRoom(3, "玩家3", BigDecimal.valueOf(1000));
            room.joinRoom(4, "玩家4", BigDecimal.valueOf(1000));
            room.joinRoom(5, "玩家5", BigDecimal.valueOf(1000));
            
            // 所有玩家入座
            for (int i = 1; i <= 5; i++) {
                room.takeSeat(i);
            }
            
            // 开始游戏
            room.startNewGame(smallBlind, bigBlind);
            
            result.put("success", true);
            result.put("message", "测试房间创建成功");
            result.put("roomId", room.getRoomId());
            result.put("roomInfo", room.getRoomInfo());
            result.put("gameInfo", room.getCurrentGameInfo());
            
            log.info("创建测试房间：{}", room.getRoomId());
            
        } catch (Exception e) {
            log.error("创建测试房间失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
