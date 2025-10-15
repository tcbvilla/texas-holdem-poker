package com.poker.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 俱乐部数据传输对象
 */
@Data
public class ClubDto {
    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private String settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String creatorName; // 创建者用户名
}
