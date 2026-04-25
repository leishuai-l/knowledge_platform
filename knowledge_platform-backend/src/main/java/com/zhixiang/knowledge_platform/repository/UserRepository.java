package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据角色查询用户
     */
    List<User> findByRole(UserRole role);

    /**
     * 根据状态查询用户
     */
    List<User> findByStatus(UserStatus status);

    /**
     * 分页查询用户
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * 根据角色和状态查询用户
     */
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);


    /**
     * 查询积分排行榜
     */
    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.totalPoints DESC")
    Page<User> findTopUsersByPoints(@Param("status") UserStatus status, Pageable pageable);

    /**
     * 统计指定时间后创建的用户数量
     */
    Long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 根据状态和关键词搜索用户
     */
    Page<User> findByStatusAndUsernameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
            UserStatus status1, String keyword1, UserStatus status2, String keyword2, Pageable pageable);

    /**
     * 统计各角色用户数量
     */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.status = :status GROUP BY u.role")
    List<Object[]> countUsersByRole(@Param("status") UserStatus status);

    /**
     * 查询积分大于指定值的用户
     */
    List<User> findByPointsGreaterThan(Integer points);

    /**
     * 查询活跃用户（有登录记录的）
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL AND u.status = :status")
    Page<User> findActiveUsers(@Param("status") UserStatus status, Pageable pageable);

    /**
     * 根据用户名或邮箱或昵称模糊查询
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "u.status = :status")
    Page<User> searchUsers(@Param("keyword") String keyword,
                          @Param("status") UserStatus status,
                          Pageable pageable);

    /**
     * 根据关键词搜索用户（不限制状态）
     */
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);

    /**
     * 根据用户名或邮箱或昵称搜索用户（兼容性方法，改为只搜索用户名和邮箱）
     */
    default Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            String username, String email, String nickname, Pageable pageable) {
        return findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(username, email, pageable);
    }

    /**
     * 根据状态统计用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 根据角色统计用户数量
     */
    long countByRole(UserRole role);

    /**
     * 查询最近注册的用户
     */
    @Query("SELECT u FROM User u WHERE u.status = :status ORDER BY u.createdAt DESC")
    Page<User> findRecentUsers(@Param("status") UserStatus status, Pageable pageable);

    /**
     * 查询最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL AND u.status = :status ORDER BY u.lastLoginTime DESC")
    Page<User> findRecentlyActiveUsers(@Param("status") UserStatus status, Pageable pageable);

    /**
     * 统计指定时间后登录的用户数量
     */
    Long countByLastLoginTimeAfter(LocalDateTime dateTime);

    /**
     * 统计指定时间段内创建的用户数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}