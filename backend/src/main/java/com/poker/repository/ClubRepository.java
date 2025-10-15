package com.poker.repository;

import com.poker.entity.Club;
import com.poker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 俱乐部数据访问层
 */
@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    
    /**
     * 根据创建者查找俱乐部
     */
    List<Club> findByCreator(User creator);
    
    /**
     * 根据创建者ID查找俱乐部
     */
    List<Club> findByCreatorId(Long creatorId);
    
    /**
     * 根据名称查找俱乐部
     */
    List<Club> findByNameContaining(String name);
    
    /**
     * 查找用户加入的俱乐部
     */
    @Query("SELECT DISTINCT c FROM Club c " +
           "JOIN c.members m " +
           "WHERE m.user = :user AND m.status = 'ACTIVE'")
    List<Club> findByMembersUser(@Param("user") User user);
    
    /**
     * 检查俱乐部名称是否存在
     */
    boolean existsByName(String name);
}
