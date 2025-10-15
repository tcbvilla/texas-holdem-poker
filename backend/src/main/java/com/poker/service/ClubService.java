package com.poker.service;

import com.poker.entity.Club;
import com.poker.entity.ClubMember;
import com.poker.entity.User;

import java.util.List;

/**
 * 俱乐部服务接口
 */
public interface ClubService {
    
    /**
     * 创建俱乐部
     */
    Club createClub(String name, String description, User creator);
    
    /**
     * 根据ID获取俱乐部
     */
    Club getClubById(Long clubId);
    
    /**
     * 获取用户创建的所有俱乐部
     */
    List<Club> getClubsByCreator(User creator);
    
    /**
     * 获取用户加入的所有俱乐部
     */
    List<Club> getClubsByMember(User user);
    
    /**
     * 邀请成员加入俱乐部
     */
    ClubMember inviteMember(Long clubId, User user, User inviter);
    
    /**
     * 移除俱乐部成员
     */
    void removeMember(Long clubId, Long userId, User operator);
    
    /**
     * 删除俱乐部
     */
    void deleteClub(Long clubId, User operator);
    
    /**
     * 检查用户是否为俱乐部管理员
     */
    boolean isClubAdmin(Long clubId, User user);
    
    /**
     * 检查用户是否为俱乐部成员
     */
    boolean isClubMember(Long clubId, User user);
    
    /**
     * 获取俱乐部的所有成员
     */
    List<ClubMember> getClubMembers(Long clubId);
    
    /**
     * 获取所有俱乐部（公开）
     */
    List<Club> getAllClubs();
    
    /**
     * 根据关键词搜索俱乐部
     */
    List<Club> searchClubs(String keyword);
    
    /**
     * 用户申请加入俱乐部
     */
    ClubMember applyToJoinClub(Long clubId, User user);
    
    /**
     * 用户退出俱乐部
     */
    void leaveClub(Long clubId, User user);
}
