package com.zhixiang.knowledge_platform.entity;

import com.zhixiang.knowledge_platform.enums.AppealStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档审核申诉实体
 * 用户对审核结果不满意时可以提交申诉
 */
@Entity
@Table(name = "document_appeals")
@Data
public class DocumentAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "review_id", nullable = false)
    private Long reviewId; // 关联的审核记录ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 申诉人ID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppealStatus status = AppealStatus.PENDING;

    @Column(name = "appeal_reason", columnDefinition = "TEXT", nullable = false)
    private String appealReason; // 申诉理由

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // 证据材料（可以是文字说明或文件链接）

    @Column(name = "handler_id")
    private Long handlerId; // 处理人ID

    @Column(name = "handler_comment", columnDefinition = "TEXT")
    private String handlerComment; // 处理意见

    @Column(name = "final_decision", columnDefinition = "TEXT")
    private String finalDecision; // 最终处理决定

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false)
    private DocumentReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id", insertable = false, updatable = false)
    private User handler;

    /**
     * 是否待处理
     */
    public boolean isPending() {
        return AppealStatus.PENDING.equals(this.status);
    }

    /**
     * 是否已处理
     */
    public boolean isHandled() {
        return AppealStatus.APPROVED.equals(this.status) ||
               AppealStatus.REJECTED.equals(this.status);
    }

    /**
     * 处理申诉
     */
    public void handle(Long handlerId, String comment, String decision, AppealStatus status) {
        this.handlerId = handlerId;
        this.handlerComment = comment;
        this.finalDecision = decision;
        this.status = status;
        this.handledAt = LocalDateTime.now();
    }
}
