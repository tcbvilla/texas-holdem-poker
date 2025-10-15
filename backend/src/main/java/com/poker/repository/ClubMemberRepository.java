package com.poker.repository;

import com.poker.entity.Club;
import com.poker.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 俱乐部成员数据访问层
 */
@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {
    
    /**
     * 根据俱乐部查找所有成员
     */
    List<ClubMember> findByClub(Club club);
    
    /**
     * 根据俱乐部ID查找所有成员
     */
    List<ClubMember> findByClubId(Long clubId);
    
    /**
     * 根据俱乐部和用户查找成员记录
     */
    Optional<ClubMember> findByClubAndUser(Club club, com.poker.entity.User user);
    
    /**
     * 根据俱乐部ID和用户ID查找成员记录
     */
    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);
    
    /**
     * 根据俱乐部ID和用户ID查找成员记录（用于查询）
     */
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId")
    Optional<ClubMember> findByClubAndUserId(@Param("clubId") Club club, @Param("userId") Long userId);
    
    /**
     * 根据俱乐部ID、用户ID和角色查找成员记录
     */
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId AND cm.role = :role")
    Optional<ClubMember> findByClubIdAndUserIdAndRole(@Param("clubId") Long clubId, 
                                                     @Param("userId") Long userId, 
                                                     @Param("role") ClubMember.ClubRole role);
    
    /**
     * 根据用户查找所有成员记录
     */
    List<ClubMember> findByUser(com.poker.entity.User user);
    
    /**
     * 根据角色查找成员
     */
    List<ClubMember> findByRole(ClubMember.ClubRole role);
    
    /**
     * 根据状态查找成员
     */
    List<ClubMember> findByStatus(ClubMember.ClubMemberStatus status);
}
