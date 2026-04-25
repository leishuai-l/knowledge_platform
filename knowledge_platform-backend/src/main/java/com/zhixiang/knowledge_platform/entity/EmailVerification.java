package com.zhixiang.knowledge_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮箱验证实体
 * 存储邮箱验证相关信息
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "email_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邮箱地址
     */
    @Column(nullable = false)
    private String email;

    /**
     * 验证码或令牌
     */
    @Column(nullable = false)
    private String code;

    /**
     * 验证类型：REGISTRATION(注册), PASSWORD_RESET(密码重置), EMAIL_CHANGE(邮箱变更)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    /**
     * 关联的用户ID（可为空，注册时用户还未创建）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 是否已验证
     */
    @Column(nullable = false)
    private Boolean verified = false;

    /**
     * 验证时间
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 尝试次数
     */
    @Column(nullable = false)
    private Integer attempts = 0;

    /**
     * 最大尝试次数
     */
    @Column(nullable = false)
    private Integer maxAttempts = 5;

    /**
     * 验证类型枚举
     */
    public enum VerificationType {
        REGISTRATION("注册验证"),
        PASSWORD_RESET("密码重置"),
        EMAIL_CHANGE("邮箱变更");

        private final String description;

        VerificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否超过最大尝试次数
     */
    public boolean isExceededMaxAttempts() {
        return attempts >= maxAttempts;
    }

    /**
     * 增加尝试次数
     */
    public void incrementAttempts() {
        this.attempts++;
    }

    /**
     * 标记为已验证
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // 默认10分钟过期
            expiresAt = createdAt.plusMinutes(10);
        }
    }
}