package com.poker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 俱乐部成员实体类
 */
@Entity
@Table(name = "club_members")
@Data
@EqualsAndHashCode(callSuper = false)
public class ClubMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ClubRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClubMemberStatus status = ClubMemberStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    /**
     * 俱乐部角色枚举
     */
    public enum ClubRole {
        ADMIN,    // 管理员
        MEMBER    // 普通成员
    }
    
    /**
     * 俱乐部成员状态枚举
     */
    public enum ClubMemberStatus {
        ACTIVE,   // 活跃
        INACTIVE  // 非活跃
    }
}
