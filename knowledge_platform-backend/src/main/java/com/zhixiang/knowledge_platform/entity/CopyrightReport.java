package com.zhixiang.knowledge_platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.enums.ReportType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 侵权举报实体
 * 用户可以举报侵犯知识产权的文档
 */
@Entity
@Table(name = "copyright_reports")
@Data
public class CopyrightReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId; // 举报人ID

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType; // 举报类型

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description; // 举报描述

    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls; // 证据链接（多个用逗号分隔）

    @Column(name = "contact_info")
    private String contactInfo; // 联系方式

    @Column(name = "handler_id")
    private Long handlerId; // 处理人ID

    @Column(name = "handler_comment", columnDefinition = "TEXT")
    private String handlerComment; // 处理意见

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken; // 采取的措施

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    // 关联关系
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", insertable = false, updatable = false)
    private User reporter;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id", insertable = false, updatable = false)
    private User handler;

    /**
     * 是否待处理
     */
    public boolean isPending() {
        return ReportStatus.PENDING.equals(this.status);
    }

    public boolean isInvestigating() {
        return ReportStatus.INVESTIGATING.equals(this.status);
    }

    public boolean canBeHandled() {
        return isPending() || isInvestigating();
    }

    /**
     * 是否已处理
     */
    public boolean isHandled() {
        return ReportStatus.CONFIRMED.equals(this.status) ||
               ReportStatus.REJECTED.equals(this.status);
    }

    public void markInvestigating(Long handlerId, String comment) {
        this.handlerId = handlerId;
        this.handlerComment = comment;
        this.status = ReportStatus.INVESTIGATING;
        this.handledAt = null;
    }

    /**
     * 处理举报
     */
    public void handle(Long handlerId, String comment, String action, ReportStatus status) {
        this.handlerId = handlerId;
        this.handlerComment = comment;
        this.actionTaken = action;
        this.status = status;
        this.handledAt = LocalDateTime.now();
    }
}
