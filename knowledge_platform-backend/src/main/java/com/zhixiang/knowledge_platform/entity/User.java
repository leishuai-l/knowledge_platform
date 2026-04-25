package com.zhixiang.knowledge_platform.entity;

import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "avatar")
    private String avatar = "avatars/default.png";

    @Column(name = "bio", length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "points", nullable = false)
    private Integer points = 100;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * 是否为活跃用户
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /**
     * 增加积分
     */
    public void addPoints(Integer points) {
        if (points > 0) {
            this.points += points;
            this.totalPoints += points;
        }
    }

    /**
     * 扣除积分
     */
    public boolean deductPoints(Integer points) {
        if (points > 0 && this.points >= points) {
            this.points -= points;
            return true;
        }
        return false;
    }

    /**
     * 检查是否有足够积分
     */
    public boolean hasEnoughPoints(Integer requiredPoints) {
        return this.points >= requiredPoints;
    }

    /**
     * 增加失败登录次数
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * 重置失败登录次数
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * 锁定账户
     */
    public void lockAccount(int lockMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked() {
        return this.lockedUntil != null && LocalDateTime.now().isBefore(this.lockedUntil);
    }

    /**
     * 检查是否需要锁定账户（失败次数达到阈值）
     */
    public boolean shouldLockAccount(int maxAttempts) {
        return this.failedLoginAttempts >= maxAttempts;
    }
}