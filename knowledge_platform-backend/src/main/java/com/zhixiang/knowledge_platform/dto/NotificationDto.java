package com.zhixiang.knowledge_platform.dto;

import com.zhixiang.knowledge_platform.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
public class NotificationDto {
    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String content;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}