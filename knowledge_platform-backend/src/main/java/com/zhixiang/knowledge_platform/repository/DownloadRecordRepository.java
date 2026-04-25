package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下载记录数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, Long> {

    /**
     * 查询用户的下载记录
     */
    Page<DownloadRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的下载记录（按下载时间）
     */
    Page<DownloadRecord> findByUserIdOrderByDownloadTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的下载记录（带文档信息和分类，避免懒加载问题）
     */
    @Query("SELECT dr FROM DownloadRecord dr " +
           "LEFT JOIN FETCH dr.document d " +
           "LEFT JOIN FETCH d.category " +
           "WHERE dr.userId = :userId ORDER BY dr.downloadTime DESC")
    Page<DownloadRecord> findByUserIdWithDocumentOrderByDownloadTimeDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查询文档的下载记录
     */
    Page<DownloadRecord> findByDocumentIdOrderByCreatedAtDesc(Long documentId, Pageable pageable);

    /**
     * 查询文档的下载记录（按下载时间）
     */
    Page<DownloadRecord> findByDocumentIdOrderByDownloadTimeDesc(Long documentId, Pageable pageable);

    /**
     * 检查用户是否已下载过文档
     */
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);

    /**
     * 检查用户是否已下载过文档
     */
    boolean existsByUserIdAndDocumentId(Long userId, Long documentId);

    /**
     * 查询用户对特定文档的下载记录
     */
    List<DownloadRecord> findByDocumentIdAndUserIdOrderByCreatedAtDesc(Long documentId, Long userId);

    /**
     * 统计文档的下载次数
     */
    @Query("SELECT COUNT(dr) FROM DownloadRecord dr WHERE dr.documentId = :documentId")
    Long countDownloadsByDocumentId(@Param("documentId") Long documentId);

    /**
     * 统计用户的下载次数
     */
    @Query("SELECT COUNT(dr) FROM DownloadRecord dr WHERE dr.userId = :userId")
    Long countDownloadsByUserId(@Param("userId") Long userId);

    /**
     * 查询指定时间范围内的下载记录
     */
    @Query("SELECT dr FROM DownloadRecord dr WHERE dr.downloadTime BETWEEN :startTime AND :endTime " +
           "ORDER BY dr.downloadTime DESC")
    Page<DownloadRecord> findDownloadsByDateRange(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                Pageable pageable);

    /**
     * 查询用户在指定时间范围内的下载记录
     */
    @Query("SELECT dr FROM DownloadRecord dr WHERE dr.userId = :userId AND " +
           "dr.downloadTime BETWEEN :startTime AND :endTime ORDER BY dr.downloadTime DESC")
    List<DownloadRecord> findUserDownloadsByDateRange(@Param("userId") Long userId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户今日下载次数
     */
    @Query("SELECT COUNT(dr) FROM DownloadRecord dr WHERE dr.userId = :userId AND " +
           "dr.downloadTime >= CURRENT_DATE")
    Long getTodayDownloadCount(@Param("userId") Long userId);

    /**
     * 统计用户今日下载消费积分
     */
    @Query("SELECT COALESCE(SUM(dr.pointsCost), 0) FROM DownloadRecord dr WHERE dr.userId = :userId AND " +
           "dr.downloadTime >= CURRENT_DATE")
    Integer getTodayPointsCost(@Param("userId") Long userId);

    /**
     * 查询热门下载文档统计
     */
    @Query("SELECT dr.documentId, COUNT(dr) as downloadCount FROM DownloadRecord dr " +
           "GROUP BY dr.documentId ORDER BY downloadCount DESC")
    List<Object[]> getPopularDocumentsStatistics(Pageable pageable);

    /**
     * 查询活跃下载用户统计
     */
    @Query("SELECT dr.userId, COUNT(dr) as downloadCount FROM DownloadRecord dr " +
           "GROUP BY dr.userId ORDER BY downloadCount DESC")
    List<Object[]> getActiveDownloadersStatistics(Pageable pageable);

    /**
     * 查询免费下载记录
     */
    List<DownloadRecord> findByPointsCostOrderByCreatedAtDesc(Integer pointsCost, Pageable pageable);

    /**
     * 查询付费下载记录
     */
    List<DownloadRecord> findByPointsCostGreaterThanOrderByCreatedAtDesc(Integer pointsCost, Pageable pageable);

    /**
     * 统计总下载次数
     */
    @Query("SELECT COUNT(dr) FROM DownloadRecord dr")
    Long getTotalDownloadCount();

    /**
     * 统计总积分消费
     */
    @Query("SELECT COALESCE(SUM(dr.pointsCost), 0) FROM DownloadRecord dr")
    Long getTotalPointsCost();

    /**
     * 查询用户下载的文档ID列表
     */
    @Query("SELECT DISTINCT dr.documentId FROM DownloadRecord dr WHERE dr.userId = :userId")
    List<Long> findDownloadedDocumentIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询下载过文档的用户ID列表
     */
    @Query("SELECT DISTINCT dr.userId FROM DownloadRecord dr WHERE dr.documentId = :documentId")
    List<Long> findDownloaderIdsByDocumentId(@Param("documentId") Long documentId);

    /**
     * 统计各时段下载量
     */
    @Query(value = "SELECT HOUR(dr.download_time), COUNT(dr.id) FROM download_records dr " +
                   "WHERE DATE(dr.download_time) = CURRENT_DATE " +
                   "GROUP BY HOUR(dr.download_time) ORDER BY HOUR(dr.download_time)",
           nativeQuery = true)
    List<Object[]> getHourlyDownloadStatistics();

    /**
     * 统计每日下载量（最近7天）
     */
    @Query(value = "SELECT DATE(dr.download_time), COUNT(dr.id) FROM download_records dr " +
                   "WHERE dr.download_time >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY) " +
                   "GROUP BY DATE(dr.download_time) ORDER BY DATE(dr.download_time)",
           nativeQuery = true)
    List<Object[]> getDailyDownloadStatistics();

    /**
     * 查询IP地址的下载记录
     */
    Page<DownloadRecord> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

    /**
     * 统计IP地址下载次数
     */
    @Query("SELECT dr.ipAddress, COUNT(dr) FROM DownloadRecord dr " +
           "WHERE dr.ipAddress IS NOT NULL GROUP BY dr.ipAddress ORDER BY COUNT(dr) DESC")
    List<Object[]> getDownloadsByIpStatistics(Pageable pageable);

    /**
     * 删除文档的所有下载记录
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 删除用户的所有下载记录
     */
    void deleteByUserId(Long userId);

    /**
     * 清理指定时间之前的下载记录
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);

    /**
     * 清理指定下载时间之前的记录
     */
    void deleteByDownloadTimeBefore(LocalDateTime cutoffTime);

    /**
     * 统计用户下载数量
     */
    Long countByUserId(Long userId);

    /**
     * 统计文档下载数量
     */
    Long countByDocumentId(Long documentId);

    /**
     * 统计用户指定时间后的下载数量
     */
    Long countByUserIdAndDownloadTimeAfter(Long userId, LocalDateTime downloadTime);

    /**
     * 统计文档指定时间后的下载数量
     */
    Long countByDocumentIdAndDownloadTimeAfter(Long documentId, LocalDateTime downloadTime);

    /**
     * 统计指定时间后的下载数量
     */
    Long countByDownloadTimeAfter(LocalDateTime downloadTime);

    /**
     * 获取用户总积分消费
     */
    @Query("SELECT COALESCE(SUM(dr.pointsCost), 0) FROM DownloadRecord dr WHERE dr.userId = :userId")
    Integer getTotalPointsSpentByUser(@Param("userId") Long userId);

    /**
     * 获取文档总积分收益
     */
    @Query("SELECT COALESCE(SUM(dr.pointsCost), 0) FROM DownloadRecord dr WHERE dr.documentId = :documentId")
    Integer getTotalPointsEarnedByDocument(@Param("documentId") Long documentId);

    /**
     * 统计文档独立下载用户数
     */
    @Query("SELECT COUNT(DISTINCT dr.userId) FROM DownloadRecord dr WHERE dr.documentId = :documentId")
    Long countDistinctUsersByDocumentId(@Param("documentId") Long documentId);

    /**
     * 获取最受欢迎的文档
     */
    @Query("SELECT dr.documentId, COUNT(dr) as downloadCount FROM DownloadRecord dr " +
           "GROUP BY dr.documentId ORDER BY downloadCount DESC")
    List<Object[]> findMostDownloadedDocuments(Pageable pageable);

    /**
     * 获取最活跃的下载用户
     */
    @Query("SELECT dr.userId, COUNT(dr) as downloadCount FROM DownloadRecord dr " +
           "GROUP BY dr.userId ORDER BY downloadCount DESC")
    List<Object[]> findMostActiveDownloaders(Pageable pageable);

    /**
     * 获取下载趋势数据
     */
    @Query("SELECT DATE(dr.downloadTime) as downloadDate, COUNT(dr) as downloadCount " +
           "FROM DownloadRecord dr WHERE dr.downloadTime BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(dr.downloadTime) ORDER BY downloadDate")
    List<Object[]> getDownloadTrendData(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最近下载记录
     */
    @Query("SELECT dr FROM DownloadRecord dr ORDER BY dr.downloadTime DESC")
    List<DownloadRecord> findRecentDownloads(Pageable pageable);

    /**
     * 获取系统总积分流转
     */
    @Query("SELECT COALESCE(SUM(dr.pointsCost), 0) FROM DownloadRecord dr")
    Integer getTotalPointsTransferred();
}