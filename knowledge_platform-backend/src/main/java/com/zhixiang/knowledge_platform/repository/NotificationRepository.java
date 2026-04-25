package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Notification;
import com.zhixiang.knowledge_platform.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查询用户的通知（按创建时间倒序）- 排除已删除的
     */
    Page<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的通知（按创建时间倒序）- 管理员查看所有
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户未读通知（排除已删除）
     */
    Page<Notification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户已读通知（排除已删除）
     */
    Page<Notification> findByUserIdAndIsReadTrueAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户未读通知（管理员）
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户已读通知（管理员）
     */
    Page<Notification> findByUserIdAndIsReadTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询用户指定类型的通知
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    /**
     * 统计用户未读通知数量（排除已删除）
     */
    Long countByUserIdAndIsReadFalseAndIsDeletedFalse(Long userId);

    /**
     * 统计用户未读通知数量（管理员）
     */
    Long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 统计用户指定类型的未读通知数量
     */
    Long countByUserIdAndTypeAndIsReadFalse(Long userId, NotificationType type);

    /**
     * 查询用户最近的通知（排除已删除）
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查询用户最近的通知（管理员）
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotificationsForAdmin(@Param("userId") Long userId, Pageable pageable);

    /**
     * 批量标记用户通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * 批量标记指定通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :notificationIds AND n.userId = :userId")
    int markAsReadByIds(@Param("notificationIds") List<Long> notificationIds,
                       @Param("userId") Long userId,
                       @Param("readAt") LocalDateTime readAt);

    /**
     * 删除用户的所有通知
     */
    void deleteByUserId(Long userId);

    /**
     * 删除指定时间之前的通知
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);

    /**
     * 查询指定时间范围内的通知
     */
    Page<Notification> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询系统公告类型的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.type = 'SYSTEM_ANNOUNCEMENT' ORDER BY n.createdAt DESC")
    Page<Notification> findSystemAnnouncements(Pageable pageable);

    /**
     * 根据关联ID查询通知
     */
    List<Notification> findByReferenceIdAndType(Long referenceId, NotificationType type);

    /**
     * 统计各类型通知数量
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type")
    List<Object[]> countNotificationsByType();

    /**
     * 查询用户通知统计信息
     */
    @Query("SELECT " +
           "COUNT(n) as total, " +
           "SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END) as unread, " +
           "SUM(CASE WHEN n.isRead = true THEN 1 ELSE 0 END) as read " +
           "FROM Notification n WHERE n.userId = :userId")
    Object[] getUserNotificationStatistics(@Param("userId") Long userId);

    /**
     * 统计未读通知数量
     */
    Long countByIsReadFalse();

    /**
     * 统计指定时间后创建的通知数量
     */
    Long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 按类型和读取状态查询通知
     */
    Page<Notification> findByTypeAndIsReadOrderByCreatedAtDesc(NotificationType type, Boolean isRead, Pageable pageable);

    /**
     * 按类型查询通知
     */
    Page<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type, Pageable pageable);

    /**
     * 按读取状态查询通知
     */
    Page<Notification> findByIsReadOrderByCreatedAtDesc(Boolean isRead, Pageable pageable);

    /**
     * 查询所有通知（按创建时间倒序）
     */
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}