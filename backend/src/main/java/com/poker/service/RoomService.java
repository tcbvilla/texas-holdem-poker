package com.poker.service;

import com.poker.entity.Club;
import com.poker.entity.Room;
import com.poker.entity.User;

import java.util.List;

/**
 * 房间服务接口
 */
public interface RoomService {
    
    /**
     * 创建房间
     */
    Room createRoom(String name, String description, Club club, User creator,
                   Integer smallBlind, Integer bigBlind, Long defaultChips,
                   Long minBuyin, Long maxBuyin, Integer maxSeats,
                   Integer durationMinutes, Integer actionTimeSeconds);
    
    /**
     * 根据房间号获取房间
     */
    Room getRoomByCode(String roomCode);
    
    /**
     * 根据ID获取房间
     */
    Room getRoomById(Long roomId);
    
    /**
     * 获取俱乐部的所有房间
     */
    List<Room> getRoomsByClub(Club club);
    
    /**
     * 获取等待中的房间
     */
    List<Room> getWaitingRooms();
    
    /**
     * 开始房间游戏
     */
    void startRoom(Long roomId, User operator);
    
    /**
     * 结束房间游戏
     */
    void endRoom(Long roomId, User operator);
    
    /**
     * 取消房间
     */
    void cancelRoom(Long roomId, User operator);

    /**
     * Mark room as running when the first hand starts (no permission check).
     */
    void markRoomRunning(Long roomId);

    /**
     * Auto-cancel room when the table becomes empty (no permission check).
     */
    void autoCancelRoom(Long roomId);

    /**
     * Auto-end room when duration expires (no permission check).
     */
    void autoEndRoom(Long roomId);

    /**
     * Persist settlement snapshot JSON when a table session closes.
     */
    void saveSettlement(Long roomId, com.poker.dto.RoomSettlementSnapshot snapshot);

    /**
     * Load settlement snapshot for a closed room session.
     */
    com.poker.dto.RoomSettlementSnapshot getSettlement(Long roomId);

    /**
     * Reset a closed room to WAITING and clear settlement snapshot.
     */
    void restartRoom(Long roomId, User operator);
    
    /**
     * 检查用户是否有权限操作房间
     */
    boolean canOperateRoom(Room room, User user);
    
    /**
     * 生成唯一的房间号
     */
    String generateRoomCode();
}
