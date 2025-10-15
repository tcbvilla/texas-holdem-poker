package com.poker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间实体类
 */
@Entity
@Table(name = "rooms")
@Data
@EqualsAndHashCode(callSuper = false)
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_code", nullable = false, unique = true, length = 16)
    private String roomCode;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "small_blind", nullable = false)
    private Integer smallBlind;
    
    @Column(name = "big_blind", nullable = false)
    private Integer bigBlind;
    
    @Column(name = "default_chips", nullable = false)
    private Long defaultChips;
    
    @Column(name = "min_buyin", nullable = false)
    private Long minBuyin;
    
    @Column(name = "max_buyin", nullable = false)
    private Long maxBuyin;
    
    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats = 9;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "action_time_seconds", nullable = false)
    private Integer actionTimeSeconds = 30;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status = RoomStatus.WAITING;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    // 注意：房间玩家信息存储在内存中，不持久化到数据库
    
    /**
     * 房间状态枚举
     */
    public enum RoomStatus {
        WAITING,   // 等待中
        RUNNING,   // 进行中
        FINISHED,  // 已结束
        CANCELLED  // 已取消
    }
}
