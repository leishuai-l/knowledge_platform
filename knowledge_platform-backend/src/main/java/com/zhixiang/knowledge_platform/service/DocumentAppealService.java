package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DocumentAppeal;
import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.AppealStatus;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.DocumentAppealRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentReviewRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档申诉服务
 * 用户对审核结果不满意时可以提交申诉
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentAppealService {

    private final DocumentAppealRepository appealRepository;
    private final DocumentReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * 提交申诉
     */
    @Transactional
    public DocumentAppeal submitAppeal(Long userId, AppealRequest request) {
        log.info("用户 {} 提交文档 {} 的申诉", userId, request.getDocumentId());

        // 1. 验证文档
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

        // 2. 验证用户是否为文档上传者
        if (!document.getUploaderId().equals(userId)) {
            throw new SecurityException("只有文档上传者才能提交申诉");
        }

        // 3. 验证文档状态（只有被拒绝的文档才能申诉）
        if (!document.isRejected()) {
            throw new IllegalStateException("只有被拒绝的文档才能提交申诉");
        }

        // 4. 检查是否已有待处理的申诉
        if (appealRepository.existsPendingAppealForDocument(request.getDocumentId())) {
            throw new IllegalStateException("该文档已有待处理的申诉");
        }

        // 5. 获取审核记录
        DocumentReview review = reviewRepository.findByDocumentIdAndReviewType(
                        request.getDocumentId(), request.getReviewType())
                .orElseThrow(() -> new IllegalArgumentException("审核记录不存在"));

        // 6. 创建申诉记录
        DocumentAppeal appeal = new DocumentAppeal();
        appeal.setDocumentId(request.getDocumentId());
        appeal.setReviewId(review.getId());
        appeal.setUserId(userId);
        appeal.setAppealReason(request.getAppealReason());
        appeal.setEvidence(request.getEvidence());
        appeal.setStatus(AppealStatus.PENDING);

        appealRepository.save(appeal);

        log.info("申诉提交成功，申诉ID: {}", appeal.getId());
        return appeal;
    }

    /**
     * 获取用户的申诉记录
     */
    public Page<DocumentAppeal> getUserAppeals(Long userId, Pageable pageable) {
        return appealRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 获取待处理的申诉列表（管理员）
     */
    public Page<DocumentAppeal> getPendingAppeals(Pageable pageable) {
        return appealRepository.findByStatusOrderByCreatedAtDesc(
                AppealStatus.PENDING, pageable);
    }

    /**
     * 处理申诉（管理员）
     */
    @Transactional
    public DocumentAppeal handleAppeal(Long appealId, Long handlerId,
                                       AppealHandleRequest request) {
        log.info("管理员 {} 处理申诉 {}", handlerId, appealId);

        // 1. 验证管理员权限
        User handler = userRepository.findById(handlerId)
                .orElseThrow(() -> new IllegalArgumentException("处理人不存在"));

        if (!handler.isAdmin()) {
            throw new SecurityException("只有管理员才能处理申诉");
        }

        // 2. 获取申诉记录
        DocumentAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new IllegalArgumentException("申诉记录不存在"));

        // 3. 检查申诉状态
        if (!appeal.isPending()) {
            throw new IllegalStateException("该申诉已被处理");
        }

        // 4. 处理申诉
        AppealStatus status = request.isApproved() ?
                AppealStatus.APPROVED : AppealStatus.REJECTED;

        appeal.handle(handlerId, request.getComment(),
                request.getDecision(), status);

        appealRepository.save(appeal);

        // 5. 如果申诉通过，重新审核文档
        if (request.isApproved()) {
            Document document = documentRepository.findById(appeal.getDocumentId())
                    .orElseThrow(() -> new IllegalArgumentException("文档不存在"));

            // 将文档状态改为待审核，等待重新审核
            document.setStatus(DocumentStatus.PENDING);
            document.setRejectionReason(null);
            documentRepository.save(document);

            log.info("申诉通过，文档 {} 将重新审核", document.getId());
        }

        log.info("申诉处理完成，结果: {}", status);
        return appeal;
    }

    /**
     * 获取申诉统计
     */
    public AppealStatistics getAppealStatistics() {
        long pending = appealRepository.countByStatus(AppealStatus.PENDING);
        long approved = appealRepository.countByStatus(AppealStatus.APPROVED);
        long rejected = appealRepository.countByStatus(AppealStatus.REJECTED);

        return new AppealStatistics(pending, approved, rejected);
    }

    /**
     * 申诉请求
     */
    public static class AppealRequest {
        private Long documentId;
        private com.zhixiang.knowledge_platform.enums.ReviewType reviewType;
        private String appealReason;
        private String evidence;

        // Getters and Setters
        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public com.zhixiang.knowledge_platform.enums.ReviewType getReviewType() {
            return reviewType;
        }

        public void setReviewType(com.zhixiang.knowledge_platform.enums.ReviewType reviewType) {
            this.reviewType = reviewType;
        }

        public String getAppealReason() {
            return appealReason;
        }

        public void setAppealReason(String appealReason) {
            this.appealReason = appealReason;
        }

        public String getEvidence() {
            return evidence;
        }

        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }
    }

    /**
     * 申诉处理请求
     */
    public static class AppealHandleRequest {
        private boolean approved;
        private String comment;
        private String decision;

        // Getters and Setters
        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }
    }

    /**
     * 申诉统计
     */
    public static class AppealStatistics {
        private final long pending;
        private final long approved;
        private final long rejected;

        public AppealStatistics(long pending, long approved, long rejected) {
            this.pending = pending;
            this.approved = approved;
            this.rejected = rejected;
        }

        public long getPending() {
            return pending;
        }

        public long getApproved() {
            return approved;
        }

        public long getRejected() {
            return rejected;
        }
    }
}
