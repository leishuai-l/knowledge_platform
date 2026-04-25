package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 根据状态查询文档（分页）
     */
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * 根据状态查询所有文档（不分页）
     */
    List<Document> findAllByStatus(DocumentStatus status);

    /**
     * 根据分类查询已审核的文档
     */
    Page<Document> findByCategoryIdAndStatus(Long categoryId, DocumentStatus status, Pageable pageable);

    /**
     * 根据上传者查询文档
     */
    Page<Document> findByUploaderId(Long uploaderId, Pageable pageable);

    /**
     * 根据上传者和状态查询文档
     */
    Page<Document> findByUploaderIdAndStatus(Long uploaderId, DocumentStatus status, Pageable pageable);

    /**
     * 全文搜索文档（标题和描述）
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "d.status = :status")
    Page<Document> searchDocuments(@Param("keyword") String keyword,
                                 @Param("status") DocumentStatus status,
                                 Pageable pageable);

    /**
     * 高级搜索文档
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR d.categoryId = :categoryId) AND " +
           "(:uploaderId IS NULL OR d.uploaderId = :uploaderId) AND " +
           "d.status = :status")
    Page<Document> advancedSearchDocuments(@Param("keyword") String keyword,
                                         @Param("categoryId") Long categoryId,
                                         @Param("uploaderId") Long uploaderId,
                                         @Param("status") DocumentStatus status,
                                         Pageable pageable);

    /**
     * 完整条件搜索文档
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR d.categoryId = :categoryId) AND " +
           "(:fileType IS NULL OR :fileType = '' OR d.fileType = :fileType) AND " +
           "(:minRating IS NULL OR d.ratingAverage >= :minRating) AND " +
           "d.status = :status")
    Page<Document> searchDocumentsWithFilters(@Param("keyword") String keyword,
                                            @Param("categoryId") Long categoryId,
                                            @Param("fileType") String fileType,
                                            @Param("minRating") Double minRating,
                                            @Param("status") DocumentStatus status,
                                            Pageable pageable);

    /**
     * 查询热门文档（按下载量排序）
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.downloadCount DESC")
    Page<Document> findPopularDocuments(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * 查询最新文档
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt DESC")
    Page<Document> findLatestDocuments(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * 查询高评分文档
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.ratingCount > 0 ORDER BY d.ratingAverage DESC")
    Page<Document> findTopRatedDocuments(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * 根据文件类型查询文档
     */
    Page<Document> findByFileTypeAndStatus(String fileType, DocumentStatus status, Pageable pageable);

    /**
     * 根据文件扩展名查询文档
     */
    Page<Document> findByFileExtensionAndStatus(String fileExtension, DocumentStatus status, Pageable pageable);

    /**
     * 查询指定时间范围内的文档
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startTime AND :endTime AND d.status = :status")
    Page<Document> findDocumentsByDateRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("status") DocumentStatus status,
                                          Pageable pageable);

    /**
     * 统计各状态文档数量
     */
    @Query("SELECT d.status, COUNT(d) FROM Document d GROUP BY d.status")
    List<Object[]> countDocumentsByStatus();

    /**
     * 统计各分类文档数量
     */
    @Query("SELECT d.categoryId, COUNT(d) FROM Document d WHERE d.status = :status GROUP BY d.categoryId")
    List<Object[]> countDocumentsByCategory(@Param("status") DocumentStatus status);

    /**
     * 统计用户上传文档数量
     */
    @Query("SELECT d.uploaderId, COUNT(d) FROM Document d WHERE d.status = :status GROUP BY d.uploaderId")
    List<Object[]> countDocumentsByUploader(@Param("status") DocumentStatus status);

    /**
     * 查询待审核文档（按创建时间升序）
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PENDING' ORDER BY d.createdAt ASC")
    Page<Document> findPendingDocuments(Pageable pageable);

    /**
     * 查询被拒绝的文档
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'REJECTED' ORDER BY d.updatedAt DESC")
    Page<Document> findRejectedDocuments(Pageable pageable);

    /**
     * 根据标签查询文档
     */
    @Query("SELECT DISTINCT d FROM Document d JOIN d.tags t WHERE t.id IN :tagIds AND d.status = :status")
    Page<Document> findDocumentsByTags(@Param("tagIds") List<Long> tagIds,
                                     @Param("status") DocumentStatus status,
                                     Pageable pageable);

    /**
     * 查询相似文档（基于分类和标签）
     */
    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN d.tags t " +
           "WHERE d.id != :documentId AND d.status = :status AND " +
           "(d.categoryId = :categoryId OR t.id IN :tagIds)")
    List<Document> findSimilarDocuments(@Param("documentId") Long documentId,
                                       @Param("categoryId") Long categoryId,
                                       @Param("tagIds") List<Long> tagIds,
                                       @Param("status") DocumentStatus status,
                                       Pageable pageable);

    /**
     * 查询用户可下载的文档（排除已下载的）
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'APPROVED' AND " +
           "d.id NOT IN (SELECT dr.documentId FROM DownloadRecord dr WHERE dr.userId = :userId)")
    Page<Document> findAvailableDocumentsForUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计文档总大小
     */
    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.status = :status")
    Long getTotalFileSizeByStatus(@Param("status") DocumentStatus status);

    /**
     * 根据状态统计文档数量
     */
    long countByStatus(DocumentStatus status);

    /**
     * 获取总下载次数
     */
    @Query("SELECT COALESCE(SUM(d.downloadCount), 0) FROM Document d WHERE d.status = 'APPROVED'")
    Long getTotalDownloadCount();

    /**
     * 获取平均评分
     */
    @Query("SELECT COALESCE(AVG(d.ratingAverage), 0.0) FROM Document d WHERE d.status = 'APPROVED' AND d.ratingCount > 0")
    Double getAverageRating();

    /**
     * 根据分类和状态查找文档（排除指定ID）
     */
    List<Document> findByCategoryIdAndStatusAndIdNot(Long categoryId, DocumentStatus status, Long excludeId);

    /**
     * 统计指定时间后创建的文档数量
     */
    Long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 增强的文档搜索（支持层级分类和简化文件类型）
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryIds IS NULL OR d.categoryId IN :categoryIds) AND " +
           "(:minRating IS NULL OR d.ratingAverage >= :minRating) AND " +
           "d.status = :status")
    Page<Document> searchDocumentsAdvanced(@Param("keyword") String keyword,
                                          @Param("categoryIds") List<Long> categoryIds,
                                          @Param("minRating") Double minRating,
                                          @Param("status") DocumentStatus status,
                                          Pageable pageable);

    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryIds IS NULL OR d.categoryId IN :categoryIds) AND " +
           "d.fileExtension IN :fileExtensions AND " +
           "(:minRating IS NULL OR d.ratingAverage >= :minRating) AND " +
           "d.status = :status")
    Page<Document> searchDocumentsAdvancedWithExtensions(@Param("keyword") String keyword,
                                                        @Param("categoryIds") List<Long> categoryIds,
                                                        @Param("fileExtensions") List<String> fileExtensions,
                                                        @Param("minRating") Double minRating,
                                                        @Param("status") DocumentStatus status,
                                                        Pageable pageable);

    /**
     * 排除指定状态的文档
     */
    Page<Document> findByStatusNot(DocumentStatus status, Pageable pageable);

    /**
     * 搜索文档（排除已删除）
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "d.status != 'DELETED'")
    Page<Document> searchDocumentsExcludeDeleted(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计指定时间后审核的文档数量
     */
    Long countByApprovedAtAfter(LocalDateTime dateTime);

    /**
     * 根据MD5查找文档
     */
    List<Document> findByMd5(String md5);

    /**
     * 统计指定时间段内创建的文档数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 根据分类和状态统计文档数量
     */
    long countByCategoryAndStatus(com.zhixiang.knowledge_platform.entity.Category category, DocumentStatus status);

    /**
     * 统计指定上传者在指定状态下的文档数量
     */
    long countByUploaderIdAndStatus(Long uploaderId, DocumentStatus status);

    /**
     * 查询热门文档 TOP 10
     */
    List<Document> findTop10ByStatusOrderByDownloadCountDesc(DocumentStatus status);
}