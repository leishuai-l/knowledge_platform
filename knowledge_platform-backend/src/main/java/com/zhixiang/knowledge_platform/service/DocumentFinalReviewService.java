package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.ReviewStatus;
import com.zhixiang.knowledge_platform.enums.ReviewType;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentReviewRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文档复审服务
 * 管理员对通过初审的文档进行复审
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentFinalReviewService {

    private final DocumentReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * 获取待复审的文档列表
     */
    public Page<DocumentReview> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByReviewTypeAndStatusOrderByCreatedAtDesc(
                ReviewType.INITIAL, ReviewStatus.APPROVED, pageable);
    }

    /**
     * 执行复审
     */
    @Transactional
    public DocumentReview performFinalReview(Long documentId, Long reviewerId,
                                             FinalReviewRequest request) {
        log.info("管理员 {} 开始复审文档 {}", reviewerId, documentId);

        // 1. 验证管理员权限
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("审核人不存在"));

        if (!reviewer.isAdmin()) {
            throw new SecurityException("只有管理员才能进行复审");
        }

        // 2. 获取文档
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        // 3. 检查是否已通过初审
        DocumentReview initialReview = reviewRepository
                .findByDocumentIdAndReviewType(documentId, ReviewType.INITIAL)
                .orElseThrow(() -> new IllegalStateException("文档未进行初审"));

        if (!initialReview.isPassed()) {
            throw new IllegalStateException("文档初审未通过，无法进行复审");
        }

        // 4. 创建复审记录
        DocumentReview finalReview = new DocumentReview();
        finalReview.setDocumentId(documentId);
        finalReview.setReviewType(ReviewType.FINAL);
        finalReview.setReviewerId(reviewerId);
        finalReview.setAcademicScore(request.getAcademicScore());
        finalReview.setOriginalityScore(request.getOriginalityScore());
        finalReview.setPracticalityScore(request.getPracticalityScore());
        finalReview.setCopyrightCompliance(request.getCopyrightCompliance());
        finalReview.setReviewComment(request.getComment());

        // 5. 判断是否通过
        boolean passed = evaluateReview(request);

        if (passed) {
            finalReview.setStatus(ReviewStatus.APPROVED);
            approveDocument(document, reviewerId);
        } else {
            finalReview.setStatus(ReviewStatus.REJECTED);
            finalReview.setRejectionReason(request.getRejectionReason());
            finalReview.setSuggestions(request.getSuggestions());
            rejectDocument(document, request.getRejectionReason());
        }

        finalReview.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(finalReview);

        log.info("文档 {} 复审完成，结果: {}", documentId, finalReview.getStatus());
        return finalReview;
    }

    /**
     * 评估复审结果
     */
    private boolean evaluateReview(FinalReviewRequest request) {
        // 所有评分必须 >= 3 分（满分5分）
        if (request.getAcademicScore() < 3 ||
            request.getOriginalityScore() < 3 ||
            request.getPracticalityScore() < 3) {
            return false;
        }

        // 版权必须合规
        if (!request.getCopyrightCompliance()) {
            return false;
        }

        return true;
    }

    /**
     * 批准文档
     */
    private void approveDocument(Document document, Long approverId) {
        document.approve(approverId);
        documentRepository.save(document);
    }

    /**
     * 拒绝文档
     */
    private void rejectDocument(Document document, String reason) {
        document.reject(reason);
        documentRepository.save(document);
    }

    /**
     * 获取审核统计
     */
    public ReviewStatistics getReviewStatistics() {
        long pendingInitial = reviewRepository.countByStatus(ReviewStatus.PENDING);
        long pendingFinal = reviewRepository.findByReviewTypeAndStatusOrderByCreatedAtDesc(
                ReviewType.INITIAL, ReviewStatus.APPROVED, Pageable.unpaged()).getTotalElements();

        return new ReviewStatistics(pendingInitial, pendingFinal);
    }

    /**
     * 复审请求
     */
    public static class FinalReviewRequest {
        private Integer academicScore; // 学术性评分 1-5
        private Integer originalityScore; // 原创性评分 1-5
        private Integer practicalityScore; // 实用性评分 1-5
        private Boolean copyrightCompliance; // 版权合规性
        private String comment; // 审核意见
        private String rejectionReason; // 拒绝原因
        private String suggestions; // 修改建议

        // Getters and Setters
        public Integer getAcademicScore() {
            return academicScore;
        }

        public void setAcademicScore(Integer academicScore) {
            this.academicScore = academicScore;
        }

        public Integer getOriginalityScore() {
            return originalityScore;
        }

        public void setOriginalityScore(Integer originalityScore) {
            this.originalityScore = originalityScore;
        }

        public Integer getPracticalityScore() {
            return practicalityScore;
        }

        public void setPracticalityScore(Integer practicalityScore) {
            this.practicalityScore = practicalityScore;
        }

        public Boolean getCopyrightCompliance() {
            return copyrightCompliance;
        }

        public void setCopyrightCompliance(Boolean copyrightCompliance) {
            this.copyrightCompliance = copyrightCompliance;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }

        public String getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(String suggestions) {
            this.suggestions = suggestions;
        }
    }

    /**
     * 审核统计
     */
    public static class ReviewStatistics {
        private final long pendingInitialReviews;
        private final long pendingFinalReviews;

        public ReviewStatistics(long pendingInitialReviews, long pendingFinalReviews) {
            this.pendingInitialReviews = pendingInitialReviews;
            this.pendingFinalReviews = pendingFinalReviews;
        }

        public long getPendingInitialReviews() {
            return pendingInitialReviews;
        }

        public long getPendingFinalReviews() {
            return pendingFinalReviews;
        }
    }
}
