package com.poker.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房间数据传输对象
 */
@Data
public class RoomDto {
    private Long id;
    private String roomCode;
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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String clubName; // 俱乐部名称
    private String createdByName; // 创建者用户名
}
