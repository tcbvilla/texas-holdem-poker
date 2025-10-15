package com.poker.repository;

import com.poker.entity.Club;
import com.poker.entity.Room;
import com.poker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 房间数据访问层
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    /**
     * 根据房间号查找房间
     */
    Optional<Room> findByRoomCode(String roomCode);
    
    /**
     * 根据俱乐部查找所有房间
     */
    List<Room> findByClub(Club club);
    
    /**
     * 根据俱乐部ID查找所有房间
     */
    List<Room> findByClubId(Long clubId);
    
    /**
     * 根据创建者查找房间
     */
    List<Room> findByCreatedBy(User createdBy);
    
    /**
     * 根据状态查找房间
     */
    List<Room> findByStatus(Room.RoomStatus status);
    
    /**
     * 根据俱乐部和状态查找房间
     */
    List<Room> findByClubAndStatus(Club club, Room.RoomStatus status);
    
    /**
     * 查找等待中的房间（可用于加入）
     */
    @Query("SELECT r FROM Room r WHERE r.status = 'WAITING'")
    List<Room> findWaitingRooms();
    
    /**
     * 根据名称查找房间
     */
    Optional<Room> findByName(String name);
    
    /**
     * 检查房间号是否存在
     */
    boolean existsByRoomCode(String roomCode);
}
