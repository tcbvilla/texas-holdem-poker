package com.poker.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InviteCodeDto {
    private Long id;
    private String code;
    private String status;
    private LocalDateTime createdAt;
    private String usedByUsername;
    private LocalDateTime usedAt;
}
