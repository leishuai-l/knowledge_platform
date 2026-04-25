package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 查询文档的所有评论（包括已删除的）
     */
    List<Comment> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    /**
     * 分页查询文档的评论
     */
    Page<Comment> findByDocumentIdAndIsDeletedOrderByCreatedAtAsc(Long documentId, Boolean isDeleted, Pageable pageable);

    /**
     * 查询文档的顶级评论（非回复）
     */
    List<Comment> findByDocumentIdAndParentIdIsNullAndIsDeletedOrderByCreatedAtAsc(Long documentId, Boolean isDeleted);

    /**
     * 分页查询文档的顶级评论
     */
    Page<Comment> findByDocumentIdAndParentIdIsNullAndIsDeletedFalse(Long documentId, Pageable pageable);

    /**
     * 查询评论的回复
     */
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    /**
     * 查询用户的评论
     */
    Page<Comment> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    /**
     * 查询用户的评论（不分页）
     */
    List<Comment> findByUserIdAndIsDeletedFalse(Long userId);

    /**
     * 统计文档的评论数量（不包括已删除的）
     */
    Long countByDocumentIdAndIsDeletedFalse(Long documentId);

    /**
     * 统计文档顶级评论数量
     */
    Long countByDocumentIdAndParentIdIsNullAndIsDeletedFalse(Long documentId);

    /**
     * 统计用户顶级评论数量
     */
    Long countByUserIdAndParentIdIsNullAndIsDeletedFalse(Long userId);

    /**
     * 统计评论的回复数量
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentId = :parentId AND c.isDeleted = false")
    Long countRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 统计用户的评论数量
     */
    Long countByUserIdAndIsDeletedFalse(Long userId);

    /**
     * 检查用户是否已评论过文档
     */
    boolean existsByDocumentIdAndUserIdAndIsDeletedFalse(Long documentId, Long userId);

    /**
     * 查询最近的评论
     */
    @Query("SELECT c FROM Comment c WHERE c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(Pageable pageable);

    /**
     * 查询指定时间范围内的评论
     */
    @Query("SELECT c FROM Comment c WHERE c.createdAt BETWEEN :startTime AND :endTime AND c.isDeleted = false")
    List<Comment> findCommentsByDateRange(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 搜索评论内容
     */
    @Query("SELECT c FROM Comment c WHERE " +
           "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) AND " +
           "c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> searchComments(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询文档的评论树结构
     */
    @Query("SELECT c FROM Comment c WHERE c.documentId = :documentId AND c.isDeleted = false " +
           "ORDER BY COALESCE(c.parentId, c.id), c.createdAt ASC")
    List<Comment> findCommentTreeByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查询用户在特定文档下的评论
     */
    List<Comment> findByDocumentIdAndUserIdAndIsDeletedOrderByCreatedAtDesc(Long documentId, Long userId, Boolean isDeleted);

    /**
     * 删除文档的所有评论（软删除）
     */
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.documentId = :documentId")
    void softDeleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 统计指定时间后的评论数量
     */
    Long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 删除用户的所有评论（软删除）
     */
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.userId = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);

    /**
     * 查询有回复的评论
     */
    @Query("SELECT DISTINCT c FROM Comment c WHERE c.id IN " +
           "(SELECT DISTINCT c2.parentId FROM Comment c2 WHERE c2.parentId IS NOT NULL AND c2.isDeleted = false)")
    List<Comment> findCommentsWithReplies();

    /**
     * 统计各文档的评论数量
     */
    @Query("SELECT c.documentId, COUNT(c) FROM Comment c WHERE c.isDeleted = false GROUP BY c.documentId")
    List<Object[]> countCommentsByDocument();

    /**
     * 查询活跃评论者
     */
    @Query("SELECT c.userId, COUNT(c) as commentCount FROM Comment c " +
           "WHERE c.isDeleted = false GROUP BY c.userId ORDER BY commentCount DESC")
    List<Object[]> findActiveCommenters(Pageable pageable);

    /**
     * 物理删除已软删除的评论（清理任务用）
     */
    void deleteByIsDeletedAndUpdatedAtBefore(Boolean isDeleted, LocalDateTime cutoffTime);

    /**
     * 查询热门评论（回复数多的评论）
     */
    @Query("SELECT c FROM Comment c WHERE c.isDeleted = false AND c.parentId IS NULL " +
           "AND (SELECT COUNT(r) FROM Comment r WHERE r.parentId = c.id AND r.isDeleted = false) > 0 " +
           "ORDER BY (SELECT COUNT(r) FROM Comment r WHERE r.parentId = c.id AND r.isDeleted = false) DESC, c.createdAt DESC")
    List<Comment> findPopularComments(Pageable pageable);
}