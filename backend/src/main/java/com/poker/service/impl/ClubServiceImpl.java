package com.poker.service.impl;

import com.poker.entity.Club;
import com.poker.entity.ClubMember;
import com.poker.entity.User;
import com.poker.repository.ClubMemberRepository;
import com.poker.repository.ClubRepository;
import com.poker.service.ClubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 俱乐部服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClubServiceImpl implements ClubService {
    
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    
    @Override
    public Club createClub(String name, String description, User creator) {
        log.info("创建俱乐部: name={}, creator={}", name, creator.getUsername());
        
        // 创建俱乐部
        Club club = new Club();
        club.setName(name);
        club.setDescription(description);
        club.setCreator(creator);
        club.setSettings("{}"); // 设置默认的空JSON对象
        club = clubRepository.save(club);
        
        // 创建者自动成为管理员
        ClubMember adminMember = new ClubMember();
        adminMember.setClub(club);
        adminMember.setUser(creator);
        adminMember.setRole(ClubMember.ClubRole.ADMIN);
        adminMember.setStatus(ClubMember.ClubMemberStatus.ACTIVE);
        clubMemberRepository.save(adminMember);
        
        log.info("俱乐部创建成功: id={}, name={}", club.getId(), club.getName());
        return club;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Club getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("俱乐部不存在: " + clubId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByCreator(User creator) {
        return clubRepository.findByCreator(creator);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByMember(User user) {
        return clubRepository.findByMembersUser(user);
    }
    
    @Override
    public ClubMember inviteMember(Long clubId, User user, User inviter) {
        log.info("邀请成员加入俱乐部: clubId={}, user={}, inviter={}", 
                clubId, user.getUsername(), inviter.getUsername());
        
        Club club = getClubById(clubId);
        
        // 检查邀请者权限
        if (!isClubAdmin(clubId, inviter)) {
            throw new IllegalArgumentException("只有管理员可以邀请成员");
        }
        
        // 检查用户是否已经是成员
        if (isClubMember(clubId, user)) {
            throw new IllegalArgumentException("用户已经是俱乐部成员");
        }
        
        // 创建成员记录
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(user);
        member.setRole(ClubMember.ClubRole.MEMBER);
        member.setStatus(ClubMember.ClubMemberStatus.ACTIVE);
        member = clubMemberRepository.save(member);
        
        log.info("成员邀请成功: clubId={}, user={}", clubId, user.getUsername());
        return member;
    }
    
    @Override
    public void removeMember(Long clubId, Long userId, User operator) {
        log.info("移除俱乐部成员: clubId={}, userId={}, operator={}", 
                clubId, userId, operator.getUsername());
        
        Club club = getClubById(clubId);
        
        // 检查操作者权限
        if (!isClubAdmin(clubId, operator)) {
            throw new IllegalArgumentException("只有管理员可以移除成员");
        }
        
        // 查找成员记录
        ClubMember member = clubMemberRepository.findByClubAndUserId(club, userId)
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
        
        // 不能移除创建者
        if (club.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("不能移除俱乐部创建者");
        }
        
        // 删除成员记录
        clubMemberRepository.delete(member);
        
        log.info("成员移除成功: clubId={}, userId={}", clubId, userId);
    }
    
    @Override
    public void deleteClub(Long clubId, User operator) {
        log.info("删除俱乐部: clubId={}, operator={}", clubId, operator.getUsername());
        
        Club club = getClubById(clubId);
        
        // 检查操作者权限（只有创建者可以删除俱乐部）
        if (!club.getCreator().getId().equals(operator.getId())) {
            throw new IllegalArgumentException("只有俱乐部创建者可以删除俱乐部");
        }
        
        // 删除所有成员记录
        List<ClubMember> members = clubMemberRepository.findByClub(club);
        clubMemberRepository.deleteAll(members);
        
        // 删除俱乐部
        clubRepository.delete(club);
        
        log.info("俱乐部删除成功: clubId={}", clubId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isClubAdmin(Long clubId, User user) {
        return clubMemberRepository.findByClubIdAndUserIdAndRole(clubId, user.getId(), ClubMember.ClubRole.ADMIN)
                .isPresent();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isClubMember(Long clubId, User user) {
        return clubMemberRepository.findByClubIdAndUserId(clubId, user.getId())
                .isPresent();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ClubMember> getClubMembers(Long clubId) {
        Club club = getClubById(clubId);
        return clubMemberRepository.findByClub(club);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Club> searchClubs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllClubs();
        }
        return clubRepository.findByNameContaining(keyword.trim());
    }
    
    @Override
    public ClubMember applyToJoinClub(Long clubId, User user) {
        log.info("用户申请加入俱乐部: clubId={}, user={}", clubId, user.getUsername());
        
        Club club = getClubById(clubId);
        
        // 检查用户是否已经是成员
        if (isClubMember(clubId, user)) {
            throw new IllegalArgumentException("您已经是该俱乐部的成员");
        }
        
        // 直接创建成员记录（简化流程，不需要审批）
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(user);
        member.setRole(ClubMember.ClubRole.MEMBER);
        member.setStatus(ClubMember.ClubMemberStatus.ACTIVE);
        member = clubMemberRepository.save(member);
        
        log.info("用户成功加入俱乐部: clubId={}, user={}", clubId, user.getUsername());
        return member;
    }
    
    @Override
    public void leaveClub(Long clubId, User user) {
        log.info("用户退出俱乐部: clubId={}, user={}", clubId, user.getUsername());
        
        Club club = getClubById(clubId);
        
        // 检查用户是否是成员
        if (!isClubMember(clubId, user)) {
            throw new IllegalArgumentException("您不是该俱乐部的成员");
        }
        
        // 不能退出自己创建的俱乐部
        if (club.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("俱乐部创建者不能退出俱乐部");
        }
        
        // 查找并删除成员记录
        ClubMember member = clubMemberRepository.findByClubAndUser(club, user)
                .orElseThrow(() -> new IllegalArgumentException("成员记录不存在"));
        
        clubMemberRepository.delete(member);
        
        log.info("用户成功退出俱乐部: clubId={}, user={}", clubId, user.getUsername());
    }
}
