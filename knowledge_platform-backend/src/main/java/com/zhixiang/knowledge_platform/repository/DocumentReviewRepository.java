package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.enums.ReviewStatus;
import com.zhixiang.knowledge_platform.enums.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档审核记录 Repository
 */
@Repository
public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {

    /**
     * 根据文档ID和审核类型查找审核记录
     */
    Optional<DocumentReview> findByDocumentIdAndReviewType(Long documentId, ReviewType reviewType);

    /**
     * 根据文档ID查找所有审核记录
     */
    List<DocumentReview> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * 根据状态查找审核记录（分页）
     */
    Page<DocumentReview> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    /**
     * 根据审核类型和状态查找审核记录
     */
    Page<DocumentReview> findByReviewTypeAndStatusOrderByCreatedAtDesc(
            ReviewType reviewType, ReviewStatus status, Pageable pageable);

    /**
     * 查找待审核的记录数量
     */
    @Query("SELECT COUNT(r) FROM DocumentReview r WHERE r.status = :status")
    long countByStatus(@Param("status") ReviewStatus status);

    /**
     * 查找审核人的审核记录
     */
    Page<DocumentReview> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId, Pageable pageable);

    /**
     * 查找相似度超过阈值的文档
     */
    @Query("SELECT r FROM DocumentReview r WHERE r.similarityScore > :threshold AND r.reviewType = 'INITIAL'")
    List<DocumentReview> findHighSimilarityDocuments(@Param("threshold") Double threshold);
}
