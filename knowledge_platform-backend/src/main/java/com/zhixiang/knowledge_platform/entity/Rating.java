package com.zhixiang.knowledge_platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 评分实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "ratings")
@Data
@EqualsAndHashCode(callSuper = false)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "文档ID不能为空")
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    @Column(name = "rating", nullable = false)
    private Integer score;

    @Size(max = 500, message = "评分评论长度不能超过500个字符")
    @Column(name = "comment", length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 是否为好评（4星及以上）
     */
    public boolean isPositive() {
        return this.score >= 4;
    }

    /**
     * 是否为差评（2星及以下）
     */
    public boolean isNegative() {
        return this.score <= 2;
    }

    /**
     * 验证评分值是否有效
     */
    public boolean isValidRating() {
        return this.score >= 1 && this.score <= 5;
    }
}