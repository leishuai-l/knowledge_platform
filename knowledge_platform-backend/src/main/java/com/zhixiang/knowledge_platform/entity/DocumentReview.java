package com.zhixiang.knowledge_platform.entity;

import com.zhixiang.knowledge_platform.enums.ReviewStatus;
import com.zhixiang.knowledge_platform.enums.ReviewType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文档审核记录实体
 * 记录文档的初审和复审过程
 */
@Entity
@Table(name = "document_reviews")
@Data
public class DocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType; // INITIAL(初审), FINAL(复审)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "reviewer_id")
    private Long reviewerId; // 审核人ID（初审为null，复审为管理员ID）

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment; // 审核意见

    // 初审检测结果
    @Column(name = "format_check_passed")
    private Boolean formatCheckPassed;

    @Column(name = "content_compliance_passed")
    private Boolean contentCompliancePassed;

    @Column(name = "similarity_score")
    private Double similarityScore; // 相似度分数 0-100

    @Column(name = "similar_document_id")
    private Long similarDocumentId; // 相似文档ID

    // 复审评估维度
    @Column(name = "academic_score")
    private Integer academicScore; // 学术性评分 1-5

    @Column(name = "originality_score")
    private Integer originalityScore; // 原创性评分 1-5

    @Column(name = "practicality_score")
    private Integer practicalityScore; // 实用性评分 1-5

    @Column(name = "copyright_compliance")
    private Boolean copyrightCompliance; // 版权合规性

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // 拒绝原因

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions; // 修改建议

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    /**
     * 是否通过审核
     */
    public boolean isPassed() {
        return ReviewStatus.APPROVED.equals(this.status);
    }

    /**
     * 是否被拒绝
     */
    public boolean isRejected() {
        return ReviewStatus.REJECTED.equals(this.status);
    }
}
