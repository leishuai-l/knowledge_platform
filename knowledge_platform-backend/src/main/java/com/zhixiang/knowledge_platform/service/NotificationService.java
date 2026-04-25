package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.NotificationDto;
import com.zhixiang.knowledge_platform.entity.Notification;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.NotificationType;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.NotificationRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 创建并发送通知
     */
    @Transactional
    public void createAndSendNotification(Long userId, NotificationType type,
                                        String title, String content, Long referenceId) {
        // 创建通知记录
        Notification notification = Notification.createNotification(userId, type, title, content, referenceId);
        notification = notificationRepository.save(notification);

        // 转换为DTO并通过WebSocket发送
        NotificationDto dto = convertToDto(notification);
        sendRealTimeNotification(userId, dto);

        log.info("通知已创建并发送: userId={}, type={}, title={}", userId, type, title);
    }

    /**
     * 通过WebSocket发送实时通知
     */
    public void sendRealTimeNotification(Long userId, NotificationDto notification) {
        try {
            // 发送给特定用户
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.debug("实时通知已发送给用户: {}", userId);
        } catch (Exception e) {
            log.error("发送实时通知失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 获取用户通知列表
     */
    public Page<NotificationDto> getUserNotifications(Long userId, int page, int size, Boolean isRead) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications;

        // 检查是否为管理员查看其他用户的通知
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 根据用户角色和读取状态查询通知
        if (isAdmin) {
            // 管理员可以看到所有通知（包括软删除的）
            if (isRead == null) {
                notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            } else if (isRead) {
                notifications = notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(userId, pageable);
            } else {
                notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
            }
        } else {
            // 普通用户只能看到未删除的通知
            if (isRead == null) {
                notifications = notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
            } else if (isRead) {
                notifications = notificationRepository.findByUserIdAndIsReadTrueAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
            } else {
                notifications = notificationRepository.findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
            }
        }

        return notifications.map(this::convertToDto);
    }

    /**
     * 获取用户未读通知数量
     */
    public Long getUnreadNotificationCount(Long userId) {
        // 检查用户权限
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (isAdmin) {
            // 管理员可以看到所有未读通知（包括软删除的）
            return notificationRepository.countByUserIdAndIsReadFalse(userId);
        } else {
            // 普通用户只统计未删除的未读通知
            return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
        }
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作此通知");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);

            // 发送已读状态更新
            NotificationDto dto = convertToDto(notification);
            sendRealTimeNotification(userId, dto);
        }
    }

    /**
     * 批量标记通知为已读
     */
    @Transactional
    public void markNotificationsAsRead(List<Long> notificationIds, Long userId) {
        int updatedCount = notificationRepository.markAsReadByIds(
                notificationIds, userId, LocalDateTime.now()
        );
        log.info("批量标记通知为已读: userId={}, count={}", userId, updatedCount);

        // 发送批量更新通知
        sendBatchUpdateNotification(userId, notificationIds, true);
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
        log.info("标记所有通知为已读: userId={}, count={}", userId, updatedCount);

        // 发送全部已读通知
        sendAllReadNotification(userId);
    }

    /**
     * 删除通知
     * - 管理员可以真删除任何通知
     * - 普通用户只能软删除（隐藏）自己的通知
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        // 检查用户权限
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (isAdmin) {
            // 管理员可以真删除任何通知
            notificationRepository.delete(notification);
            log.info("管理员删除通知: id={}, adminId={}", notificationId, userId);
        } else {
            // 普通用户只能软删除自己的通知
            if (!notification.getUserId().equals(userId)) {
                throw new RuntimeException("无权限删除此通知");
            }

            notification.markAsDeleted();
            notificationRepository.save(notification);
            log.info("用户隐藏通知: id={}, userId={}", notificationId, userId);
        }
    }

    /**
     * 获取最近通知
     */
    public List<NotificationDto> getRecentNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 检查用户权限
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        List<Notification> notifications;
        if (isAdmin) {
            // 管理员可以看到所有通知（包括软删除的）
            notifications = notificationRepository.findRecentNotificationsForAdmin(userId, pageable);
        } else {
            // 普通用户只能看到未删除的通知
            notifications = notificationRepository.findRecentNotifications(userId, pageable);
        }

        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 发送系统公告
     */
    @Transactional
    public void sendSystemAnnouncement(String title, String content) {
        try {
            // 获取所有活跃用户
            List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);

            // 为每个活跃用户创建通知记录
            List<Notification> notifications = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (User user : activeUsers) {
                Notification notification = Notification.createNotification(
                    user.getId(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    title,
                    content,
                    null
                );
                notification.setCreatedAt(now); // 确保所有通知时间一致
                notifications.add(notification);
            }

            // 批量保存通知
            notificationRepository.saveAll(notifications);
            log.info("系统公告已保存到数据库，影响用户数: {}", notifications.size());

            // 创建广播用的DTO
            NotificationDto announcement = new NotificationDto();
            announcement.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setCreatedAt(now);

            // 广播系统公告给在线用户
            messagingTemplate.convertAndSend("/topic/announcements", announcement);
            log.info("系统公告已广播: title={}", title);

            // 为每个用户发送个人通知（如果在线）
            for (Notification notification : notifications) {
                NotificationDto dto = convertToDto(notification);
                sendRealTimeNotification(notification.getUserId(), dto);
            }

        } catch (Exception e) {
            log.error("发送系统公告失败: title={}, error={}", title, e.getMessage(), e);
            throw new RuntimeException("发送系统公告失败: " + e.getMessage());
        }
    }

    /**
     * 发送定向通知给指定用户
     */
    @Transactional
    public void sendNotificationToUsers(List<Long> userIds, String title, String content, NotificationType type, Long referenceId) {
        try {
            List<Notification> notifications = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Long userId : userIds) {
                Notification notification = Notification.createNotification(
                    userId,
                    type,
                    title,
                    content,
                    referenceId
                );
                notification.setCreatedAt(now);
                notifications.add(notification);
            }

            // 批量保存通知
            List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
            log.info("定向通知已发送，影响用户数: {}", savedNotifications.size());

            // 为每个用户发送实时通知
            for (Notification notification : savedNotifications) {
                NotificationDto dto = convertToDto(notification);
                sendRealTimeNotification(notification.getUserId(), dto);
            }

        } catch (Exception e) {
            log.error("发送定向通知失败: userIds={}, title={}, error={}", userIds, title, e.getMessage(), e);
            throw new RuntimeException("发送定向通知失败: " + e.getMessage());
        }
    }

    /**
     * 发送批量更新通知
     */
    private void sendBatchUpdateNotification(Long userId, List<Long> notificationIds, boolean isRead) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/batch-update",
                    new BatchUpdateNotification(notificationIds, isRead)
            );
        } catch (Exception e) {
            log.error("发送批量更新通知失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 发送全部已读通知
     */
    private void sendAllReadNotification(Long userId) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/all-read",
                    new AllReadNotification(true)
            );
        } catch (Exception e) {
            log.error("发送全部已读通知失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 获取通知统计信息（管理员）
     */
    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总通知数量
        long totalCount = notificationRepository.count();
        statistics.put("totalCount", totalCount);

        // 未读通知数量
        statistics.put("unreadCount", notificationRepository.countByIsReadFalse());

        // 已读通知数量
        statistics.put("readCount", totalCount - (Long) statistics.get("unreadCount"));

        // 各类型通知数量
        List<Object[]> typeStats = notificationRepository.countNotificationsByType();
        Map<String, Long> typeCountMap = new HashMap<>();
        for (Object[] stat : typeStats) {
            NotificationType type = (NotificationType) stat[0];
            Long count = (Long) stat[1];
            typeCountMap.put(type.name(), count);
        }
        statistics.put("typeStatistics", typeCountMap);

        // 最近7天的通知数量
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentCount = notificationRepository.countByCreatedAtAfter(weekAgo);
        statistics.put("recentWeekCount", recentCount);

        return statistics;
    }

    /**
     * 获取所有通知列表（管理员）
     */
    public Page<NotificationDto> getAllNotifications(int page, int size, String type, Boolean isRead) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications;

        if (type != null && !type.trim().isEmpty() && isRead != null) {
            // 按类型和读取状态过滤
            NotificationType notificationType = NotificationType.valueOf(type.trim().toUpperCase());
            notifications = notificationRepository.findByTypeAndIsReadOrderByCreatedAtDesc(notificationType, isRead, pageable);
        } else if (type != null && !type.trim().isEmpty()) {
            // 只按类型过滤
            NotificationType notificationType = NotificationType.valueOf(type.trim().toUpperCase());
            notifications = notificationRepository.findByTypeOrderByCreatedAtDesc(notificationType, pageable);
        } else if (isRead != null) {
            // 只按读取状态过滤
            notifications = notificationRepository.findByIsReadOrderByCreatedAtDesc(isRead, pageable);
        } else {
            // 获取所有通知
            notifications = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return notifications.map(this::convertToDto);
    }

    /**
     * 转换为DTO
     */
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }

    /**
     * 批量更新通知内部类
     */
    public static class BatchUpdateNotification {
        private List<Long> notificationIds;
        private boolean isRead;

        public BatchUpdateNotification(List<Long> notificationIds, boolean isRead) {
            this.notificationIds = notificationIds;
            this.isRead = isRead;
        }

        public List<Long> getNotificationIds() { return notificationIds; }
        public boolean isRead() { return isRead; }
    }

    /**
     * 全部已读通知内部类
     */
    public static class AllReadNotification {
        private boolean allRead;

        public AllReadNotification(boolean allRead) {
            this.allRead = allRead;
        }

        public boolean isAllRead() { return allRead; }
    }
}