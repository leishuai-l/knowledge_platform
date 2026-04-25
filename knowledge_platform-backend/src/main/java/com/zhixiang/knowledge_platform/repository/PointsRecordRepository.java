package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.PointsRecord;
import com.zhixiang.knowledge_platform.enums.PointsSource;
import com.zhixiang.knowledge_platform.enums.PointsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分记录数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {

    /**
     * 查询用户的积分记录
     */
    Page<PointsRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户指定类型的积分记录
     */
    Page<PointsRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, PointsType type, Pageable pageable);

    /**
     * 查询用户指定来源的积分记录
     */
    Page<PointsRecord> findByUserIdAndSourceOrderByCreatedAtDesc(Long userId, PointsSource source, Pageable pageable);

    /**
     * 查询用户在指定时间范围内的积分记录
     */
    @Query("SELECT pr FROM PointsRecord pr WHERE pr.userId = :userId AND " +
           "pr.createdAt BETWEEN :startTime AND :endTime ORDER BY pr.createdAt DESC")
    List<PointsRecord> findByUserIdAndDateRange(@Param("userId") Long userId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户指定类型的积分总额
     */
    @Query("SELECT COALESCE(SUM(pr.points), 0) FROM PointsRecord pr WHERE pr.userId = :userId AND pr.type = :type")
    Integer sumPointsByUserIdAndType(@Param("userId") Long userId, @Param("type") PointsType type);

    /**
     * 统计用户指定来源的积分总额
     */
    @Query("SELECT COALESCE(SUM(pr.points), 0) FROM PointsRecord pr WHERE pr.userId = :userId AND pr.source = :source")
    Integer sumPointsByUserIdAndSource(@Param("userId") Long userId, @Param("source") PointsSource source);

    /**
     * 查询用户今日积分获得总额
     */
    @Query("SELECT COALESCE(SUM(pr.points), 0) FROM PointsRecord pr WHERE pr.userId = :userId AND " +
           "pr.type = 'EARN' AND pr.createdAt >= CURRENT_DATE")
    Integer getTodayEarnedPoints(@Param("userId") Long userId);

    /**
     * 查询用户今日积分消费总额
     */
    @Query("SELECT COALESCE(SUM(pr.points), 0) FROM PointsRecord pr WHERE pr.userId = :userId AND " +
           "pr.type = 'SPEND' AND pr.createdAt >= CURRENT_DATE")
    Integer getTodaySpentPoints(@Param("userId") Long userId);

    /**
     * 查询指定时间范围内的积分记录
     */
    @Query("SELECT pr FROM PointsRecord pr WHERE pr.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY pr.createdAt DESC")
    Page<PointsRecord> findRecordsByDateRange(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            Pageable pageable);

    /**
     * 统计各类型积分记录数量
     */
    @Query("SELECT pr.type, COUNT(pr) FROM PointsRecord pr GROUP BY pr.type")
    List<Object[]> countRecordsByType();

    /**
     * 统计各来源积分记录数量
     */
    @Query("SELECT pr.source, COUNT(pr) FROM PointsRecord pr GROUP BY pr.source")
    List<Object[]> countRecordsBySource();

    /**
     * 查询积分流水统计
     */
    @Query("SELECT pr.type, pr.source, SUM(pr.points), COUNT(pr) FROM PointsRecord pr " +
           "GROUP BY pr.type, pr.source ORDER BY pr.type, pr.source")
    List<Object[]> getPointsFlowStatistics();

    /**
     * 查询用户积分记录统计
     */
    @Query("SELECT " +
           "SUM(CASE WHEN pr.type = 'EARN' THEN pr.points ELSE 0 END) as totalEarned, " +
           "SUM(CASE WHEN pr.type = 'SPEND' THEN pr.points ELSE 0 END) as totalSpent, " +
           "COUNT(CASE WHEN pr.type = 'EARN' THEN 1 END) as earnCount, " +
           "COUNT(CASE WHEN pr.type = 'SPEND' THEN 1 END) as spendCount " +
           "FROM PointsRecord pr WHERE pr.userId = :userId")
    Object[] getUserPointsStatistics(@Param("userId") Long userId);

    /**
     * 查询关联特定资源的积分记录
     */
    List<PointsRecord> findByReferenceIdAndSourceOrderByCreatedAtDesc(Long referenceId, PointsSource source);

    /**
     * 查询最近的积分记录
     */
    @Query("SELECT pr FROM PointsRecord pr ORDER BY pr.createdAt DESC")
    List<PointsRecord> findRecentRecords(Pageable pageable);

    /**
     * 查询积分排行榜数据
     */
    @Query("SELECT pr.userId, SUM(CASE WHEN pr.type = 'EARN' THEN pr.points ELSE -pr.points END) as netPoints " +
           "FROM PointsRecord pr GROUP BY pr.userId ORDER BY netPoints DESC")
    List<Object[]> getPointsLeaderboard(Pageable pageable);

    /**
     * 根据类型和时间范围查询记录
     */
    Page<PointsRecord> findByTypeAndCreatedAtBetween(PointsType type, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据类型查询记录
     */
    Page<PointsRecord> findByType(PointsType type, Pageable pageable);

    /**
     * 根据时间范围查询记录
     */
    Page<PointsRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询用户月度积分统计
     */
    @Query(value = "SELECT YEAR(pr.created_at), MONTH(pr.created_at), pr.type, SUM(pr.points) " +
                   "FROM points_records pr WHERE pr.user_id = :userId " +
                   "GROUP BY YEAR(pr.created_at), MONTH(pr.created_at), pr.type " +
                   "ORDER BY YEAR(pr.created_at) DESC, MONTH(pr.created_at) DESC",
           nativeQuery = true)
    List<Object[]> getUserMonthlyPointsStatistics(@Param("userId") Long userId);

    /**
     * 删除用户的所有积分记录
     */
    void deleteByUserId(Long userId);

    /**
     * 清理指定时间之前的积分记录
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
}