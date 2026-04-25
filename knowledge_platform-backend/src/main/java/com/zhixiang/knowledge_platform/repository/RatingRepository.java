package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 评分数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * 查找用户对特定文档的评分
     */
    Optional<Rating> findByDocumentIdAndUserId(Long documentId, Long userId);

    /**
     * 检查用户是否已评分
     */
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);

    /**
     * 查询文档的所有评分
     */
    List<Rating> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * 查询文档的所有评分（不按时间排序）
     */
    List<Rating> findByDocumentId(Long documentId);

    /**
     * 查询用户的所有评分
     */
    List<Rating> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户的所有评分（带文档信息和分类，避免懒加载问题）
     */
    @Query("SELECT r FROM Rating r " +
           "LEFT JOIN FETCH r.document d " +
           "LEFT JOIN FETCH d.category " +
           "LEFT JOIN FETCH d.uploader " +
           "LEFT JOIN FETCH d.tags " +
           "LEFT JOIN FETCH r.user " +
           "WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Rating> findByUserIdWithDocumentOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 查询用户的所有评分（不分页）
     */
    List<Rating> findByUserId(Long userId);

    /**
     * 分页查询文档的评分
     */
    Page<Rating> findByDocumentIdOrderByCreatedAtDesc(Long documentId, Pageable pageable);

    /**
     * 查询用户的所有评分
     */
    Page<Rating> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询特定评分值的记录
     */
    List<Rating> findByDocumentIdAndScore(Long documentId, Integer score);

    /**
     * 统计文档的评分分布
     */
    @Query("SELECT r.score, COUNT(r) FROM Rating r WHERE r.documentId = :documentId GROUP BY r.score ORDER BY r.score DESC")
    List<Object[]> getRatingDistribution(@Param("documentId") Long documentId);

    /**
     * 计算文档的平均评分
     */
    @Query("SELECT AVG(CAST(r.score AS double)) FROM Rating r WHERE r.documentId = :documentId")
    BigDecimal calculateAverageRating(@Param("documentId") Long documentId);

    /**
     * 统计文档的评分总数
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.documentId = :documentId")
    Long countRatingsByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查询高评分记录（4星及以上）
     */
    List<Rating> findByDocumentIdAndScoreGreaterThanEqual(Long documentId, Integer score);

    /**
     * 查询低评分记录（2星及以下）
     */
    List<Rating> findByDocumentIdAndScoreLessThanEqual(Long documentId, Integer score);

    /**
     * 统计用户评分总数
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.userId = :userId")
    Long countRatingsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的平均评分倾向
     */
    @Query("SELECT AVG(CAST(r.score AS double)) FROM Rating r WHERE r.userId = :userId")
    BigDecimal calculateUserAverageRating(@Param("userId") Long userId);

    /**
     * 查询评分在指定范围内的记录
     */
    List<Rating> findByDocumentIdAndScoreBetween(Long documentId, Integer minScore, Integer maxScore);

    /**
     * 查询最近的评分记录
     */
    @Query("SELECT r FROM Rating r WHERE r.documentId = :documentId ORDER BY r.createdAt DESC")
    List<Rating> findRecentRatingsByDocument(@Param("documentId") Long documentId, Pageable pageable);

    /**
     * 删除文档的所有评分
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 删除用户的所有评分
     */
    void deleteByUserId(Long userId);

    /**
     * 获取全局评分分布统计
     */
    @Query("SELECT r.score, COUNT(r) FROM Rating r GROUP BY r.score ORDER BY r.score")
    List<Object[]> getRatingDistribution();

    /**
     * 查询评分统计信息
     */
    @Query("SELECT " +
           "COUNT(r) as totalRatings, " +
           "AVG(CAST(r.score AS double)) as averageRating, " +
           "MIN(r.score) as minRating, " +
           "MAX(r.score) as maxRating " +
           "FROM Rating r WHERE r.documentId = :documentId")
    Object[] getRatingStatistics(@Param("documentId") Long documentId);

    /**
     * 查询文档评分摘要（各星级数量）
     */
    @Query("SELECT " +
           "SUM(CASE WHEN r.score = 5 THEN 1 ELSE 0 END) as fiveStars, " +
           "SUM(CASE WHEN r.score = 4 THEN 1 ELSE 0 END) as fourStars, " +
           "SUM(CASE WHEN r.score = 3 THEN 1 ELSE 0 END) as threeStars, " +
           "SUM(CASE WHEN r.score = 2 THEN 1 ELSE 0 END) as twoStars, " +
           "SUM(CASE WHEN r.score = 1 THEN 1 ELSE 0 END) as oneStars " +
           "FROM Rating r WHERE r.documentId = :documentId")
    Object[] getRatingSummary(@Param("documentId") Long documentId);

    /**
     * 分页查询所有评分（按创建时间降序）
     */
    Page<Rating> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查询高分文档排行
     */
    @Query("SELECT d.id, d.title, AVG(CAST(r.score AS double)) as avgRating, COUNT(r) as ratingCount " +
           "FROM Rating r JOIN Document d ON r.documentId = d.id " +
           "WHERE d.status = 'APPROVED' " +
           "GROUP BY d.id, d.title " +
           "HAVING COUNT(r) >= 5 " +
           "ORDER BY avgRating DESC, ratingCount DESC")
    List<Object[]> findTopRatedDocuments(Pageable pageable);

    /**
     * 计算全局平均评分
     */
    @Query("SELECT AVG(CAST(r.score AS double)) FROM Rating r")
    Double getAverageRating();
}