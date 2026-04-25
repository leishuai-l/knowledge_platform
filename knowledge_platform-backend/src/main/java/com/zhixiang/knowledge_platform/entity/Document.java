package com.zhixiang.knowledge_platform.entity;

import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "documents")
@Data
@EqualsAndHashCode(callSuper = false)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_extension", nullable = false, length = 10)
    private String fileExtension;

    @Column(name = "md5", length = 32)
    private String md5;


    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "rating_average", precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "download_points", nullable = false)
    private Integer downloadPoints = 0;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_analysis_status", length = 20)
    private String aiAnalysisStatus = "PENDING";

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", insertable = false, updatable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private User approver;

    // 文档标签关联
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "document_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    /**
     * 是否已审核通过
     */
    public boolean isApproved() {
        return DocumentStatus.APPROVED.equals(this.status);
    }

    /**
     * 是否待审核
     */
    public boolean isPending() {
        return DocumentStatus.PENDING.equals(this.status);
    }

    /**
     * 是否被拒绝
     */
    public boolean isRejected() {
        return DocumentStatus.REJECTED.equals(this.status);
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return DocumentStatus.DELETED.equals(this.status);
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 更新评分信息
     */
    public void updateRating(BigDecimal newAverage, Integer newCount) {
        this.ratingAverage = newAverage;
        this.ratingCount = newCount;
    }

    /**
     * 审核通过
     */
    public void approve(Long approverId) {
        this.status = DocumentStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    /**
     * 审核拒绝
     */
    public void reject(String reason) {
        this.status = DocumentStatus.REJECTED;
        this.rejectionReason = reason;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    /**
     * 删除文档
     */
    public void delete(String reason) {
        this.status = DocumentStatus.DELETED;
        this.rejectionReason = reason;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    /**
     * 获取文件大小的可读格式
     */
    public String getReadableFileSize() {
        long bytes = this.fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}