package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.DocumentAppeal;
import com.zhixiang.knowledge_platform.enums.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档申诉 Repository
 */
@Repository
public interface DocumentAppealRepository extends JpaRepository<DocumentAppeal, Long> {

    /**
     * 根据文档ID查找申诉记录
     */
    List<DocumentAppeal> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * 根据用户ID查找申诉记录
     */
    Page<DocumentAppeal> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据状态查找申诉记录
     */
    Page<DocumentAppeal> findByStatusOrderByCreatedAtDesc(AppealStatus status, Pageable pageable);

    /**
     * 根据审核记录ID查找申诉
     */
    Optional<DocumentAppeal> findByReviewId(Long reviewId);

    /**
     * 查找待处理的申诉数量
     */
    @Query("SELECT COUNT(a) FROM DocumentAppeal a WHERE a.status = :status")
    long countByStatus(@Param("status") AppealStatus status);

    /**
     * 查找处理人的申诉记录
     */
    Page<DocumentAppeal> findByHandlerIdOrderByCreatedAtDesc(Long handlerId, Pageable pageable);

    /**
     * 检查文档是否已有待处理的申诉
     */
    @Query("SELECT COUNT(a) > 0 FROM DocumentAppeal a WHERE a.documentId = :documentId AND a.status = 'PENDING'")
    boolean existsPendingAppealForDocument(@Param("documentId") Long documentId);
}
