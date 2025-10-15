package com.poker.service.impl;

import com.poker.entity.Club;
import com.poker.entity.Room;
import com.poker.entity.User;
import com.poker.repository.RoomRepository;
import com.poker.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * 房间服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomServiceImpl implements RoomService {
    
    private final RoomRepository roomRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public Room createRoom(String name, String description, Club club, User creator,
                          Integer smallBlind, Integer bigBlind, Long defaultChips,
                          Long minBuyin, Long maxBuyin, Integer maxSeats,
                          Integer durationMinutes, Integer actionTimeSeconds) {
        log.info("创建房间: name={}, club={}, creator={}", name, club.getName(), creator.getUsername());
        
        // 验证参数
        validateRoomParameters(smallBlind, bigBlind, defaultChips, minBuyin, maxBuyin, maxSeats);
        
        // 生成唯一房间号
        String roomCode = generateRoomCode();
        
        // 创建房间
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setClub(club);
        room.setName(name);
        room.setDescription(description);
        room.setSmallBlind(smallBlind);
        room.setBigBlind(bigBlind);
        room.setDefaultChips(defaultChips);
        room.setMinBuyin(minBuyin);
        room.setMaxBuyin(maxBuyin);
        room.setMaxSeats(maxSeats);
        room.setDurationMinutes(durationMinutes);
        room.setActionTimeSeconds(actionTimeSeconds);
        room.setStatus(Room.RoomStatus.WAITING);
        room.setCreatedBy(creator);
        
        room = roomRepository.save(room);
        
        log.info("房间创建成功: id={}, roomCode={}, name={}", room.getId(), room.getRoomCode(), room.getName());
        return room;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Room getRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在: " + roomCode));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("房间不存在: " + roomId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByClub(Club club) {
        return roomRepository.findByClub(club);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Room> getWaitingRooms() {
        return roomRepository.findWaitingRooms();
    }
    
    @Override
    public void startRoom(Long roomId, User operator) {
        log.info("开始房间游戏: roomId={}, operator={}", roomId, operator.getUsername());
        
        Room room = getRoomById(roomId);
        
        // 检查权限
        if (!canOperateRoom(room, operator)) {
            throw new IllegalArgumentException("没有权限操作此房间");
        }
        
        // 检查房间状态
        if (room.getStatus() != Room.RoomStatus.WAITING) {
            throw new IllegalArgumentException("只有等待中的房间才能开始游戏");
        }
        
        // 更新房间状态
        room.setStatus(Room.RoomStatus.RUNNING);
        room.setStartedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);
        
        log.info("房间游戏开始: roomId={}, roomCode={}", roomId, room.getRoomCode());
    }
    
    @Override
    public void endRoom(Long roomId, User operator) {
        log.info("结束房间游戏: roomId={}, operator={}", roomId, operator.getUsername());
        
        Room room = getRoomById(roomId);
        
        // 检查权限
        if (!canOperateRoom(room, operator)) {
            throw new IllegalArgumentException("没有权限操作此房间");
        }
        
        // 检查房间状态
        if (room.getStatus() != Room.RoomStatus.RUNNING) {
            throw new IllegalArgumentException("只有进行中的房间才能结束游戏");
        }
        
        // 更新房间状态
        room.setStatus(Room.RoomStatus.FINISHED);
        room.setEndedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);
        
        log.info("房间游戏结束: roomId={}, roomCode={}", roomId, room.getRoomCode());
    }
    
    @Override
    public void cancelRoom(Long roomId, User operator) {
        log.info("取消房间: roomId={}, operator={}", roomId, operator.getUsername());
        
        Room room = getRoomById(roomId);
        
        // 检查权限
        if (!canOperateRoom(room, operator)) {
            throw new IllegalArgumentException("没有权限操作此房间");
        }
        
        // 检查房间状态
        if (room.getStatus() == Room.RoomStatus.FINISHED) {
            throw new IllegalArgumentException("已结束的房间不能取消");
        }
        
        // 更新房间状态
        room.setStatus(Room.RoomStatus.CANCELLED);
        room.setEndedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);
        
        log.info("房间已取消: roomId={}, roomCode={}", roomId, room.getRoomCode());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canOperateRoom(Room room, User user) {
        // 房间创建者可以操作
        if (room.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        
        // 俱乐部管理员可以操作
        // TODO: 这里需要注入ClubService来检查管理员权限
        // 暂时只允许创建者操作
        
        return false;
    }
    
    @Override
    public String generateRoomCode() {
        String roomCode;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            // 生成8位随机数字
            int code = secureRandom.nextInt(90000000) + 10000000; // 10000000-99999999
            roomCode = String.valueOf(code);
            attempts++;
            
            if (attempts >= maxAttempts) {
                throw new RuntimeException("生成房间号失败，请重试");
            }
        } while (roomRepository.existsByRoomCode(roomCode));
        
        return roomCode;
    }
    
    /**
     * 验证房间参数
     */
    private void validateRoomParameters(Integer smallBlind, Integer bigBlind, Long defaultChips,
                                      Long minBuyin, Long maxBuyin, Integer maxSeats) {
        if (smallBlind == null || smallBlind <= 0) {
            throw new IllegalArgumentException("小盲注必须大于0");
        }
        
        if (bigBlind == null || bigBlind <= 0) {
            throw new IllegalArgumentException("大盲注必须大于0");
        }
        
        if (bigBlind < smallBlind * 2) {
            throw new IllegalArgumentException("大盲注必须至少是小盲注的2倍");
        }
        
        if (defaultChips == null || defaultChips <= 0) {
            throw new IllegalArgumentException("默认筹码必须大于0");
        }
        
        if (minBuyin == null || minBuyin <= 0) {
            throw new IllegalArgumentException("最小买入必须大于0");
        }
        
        if (maxBuyin == null || maxBuyin <= 0) {
            throw new IllegalArgumentException("最大买入必须大于0");
        }
        
        if (maxBuyin < minBuyin) {
            throw new IllegalArgumentException("最大买入不能小于最小买入");
        }
        
        if (maxSeats == null || maxSeats < 2 || maxSeats > 9) {
            throw new IllegalArgumentException("最大座位数必须在2-9之间");
        }
    }
}
