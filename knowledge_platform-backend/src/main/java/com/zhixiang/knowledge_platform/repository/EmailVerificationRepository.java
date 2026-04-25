package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮箱验证Repository
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * 根据邮箱和验证码查找
     */
    Optional<EmailVerification> findByEmailAndCode(String email, String code);

    /**
     * 根据邮箱和类型查找最新的验证记录
     */
    Optional<EmailVerification> findFirstByEmailAndTypeOrderByCreatedAtDesc(String email, EmailVerification.VerificationType type);

    /**
     * 根据邮箱、类型和状态查找
     */
    Optional<EmailVerification> findByEmailAndTypeAndVerified(String email, EmailVerification.VerificationType type, Boolean verified);

    /**
     * 查找用户的未验证记录
     */
    List<EmailVerification> findByUserIdAndVerified(Long userId, Boolean verified);

    /**
     * 查找指定时间前创建的记录
     */
    List<EmailVerification> findByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 查找过期的未验证记录
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.expiresAt < :now AND ev.verified = false")
    List<EmailVerification> findExpiredUnverified(@Param("now") LocalDateTime now);

    /**
     * 删除过期的验证记录
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.expiresAt < :now")
    int deleteExpiredVerifications(@Param("now") LocalDateTime now);

    /**
     * 删除指定邮箱的旧验证记录
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.email = :email AND ev.type = :type AND ev.verified = false")
    int deleteUnverifiedByEmailAndType(@Param("email") String email, @Param("type") EmailVerification.VerificationType type);

    /**
     * 统计指定邮箱今日的验证请求次数
     */
    @Query("SELECT COUNT(ev) FROM EmailVerification ev WHERE ev.email = :email AND ev.createdAt >= :startOfDay")
    long countTodayVerificationsByEmail(@Param("email") String email, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 统计指定IP今日的验证请求次数（如果需要的话）
     */
    @Query("SELECT COUNT(ev) FROM EmailVerification ev WHERE ev.createdAt >= :startOfDay")
    long countTodayVerifications(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 检查邮箱是否已经验证过
     */
    boolean existsByEmailAndTypeAndVerified(String email, EmailVerification.VerificationType type, Boolean verified);

    /**
     * 获取用户最近的验证记录
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.userId = :userId ORDER BY ev.createdAt DESC")
    List<EmailVerification> findRecentByUserId(@Param("userId") Long userId);
}