package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.NotificationDto;
import com.zhixiang.knowledge_platform.enums.NotificationType;
import com.zhixiang.knowledge_platform.service.NotificationService;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public ApiResponse<Page<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.success(Page.empty());
        }

        Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, page, size, isRead);
        return ApiResponse.success(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ApiResponse<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.success(0L);
        }

        Long count = notificationService.getUnreadNotificationCount(userId);
        return ApiResponse.success(count);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近通知")
    public ApiResponse<List<NotificationDto>> getRecentNotifications(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.success(List.of());
        }

        List<NotificationDto> notifications = notificationService.getRecentNotifications(userId, limit);
        return ApiResponse.success(notifications);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未认证");
        }
        notificationService.markNotificationAsRead(id, userId);
        return ApiResponse.success();
    }

    @PutMapping("/batch-read")
    @Operation(summary = "批量标记通知为已读")
    public ApiResponse<Void> batchMarkAsRead(
            @RequestBody List<Long> notificationIds,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未认证");
        }
        notificationService.markNotificationsAsRead(notificationIds, userId);
        return ApiResponse.success();
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "标记所有通知为已读")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未认证");
        }
        notificationService.markAllNotificationsAsRead(userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id, HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未认证");
        }
        notificationService.deleteNotification(id, userId);
        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/system-announcement")
    @Operation(summary = "发送系统公告（管理员）")
    public ApiResponse<Void> sendSystemAnnouncement(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String title = request.get("title");
        String content = request.get("content");

        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error("标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error("内容不能为空");
        }

        notificationService.sendSystemAnnouncement(title.trim(), content.trim());
        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/targeted-notification")
    @Operation(summary = "发送定向通知（管理员）")
    public ApiResponse<Void> sendTargetedNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String title = (String) request.get("title");
        String content = (String) request.get("content");
        @SuppressWarnings("unchecked")
        List<Object> userIdObjects = (List<Object>) request.get("userIds");
        String typeStr = (String) request.get("type");

        // 将Integer/Long转换为Long类型
        List<Long> userIds = userIdObjects.stream()
                .map(obj -> {
                    if (obj instanceof Integer) {
                        return ((Integer) obj).longValue();
                    } else if (obj instanceof Long) {
                        return (Long) obj;
                    } else {
                        return Long.valueOf(obj.toString());
                    }
                })
                .collect(java.util.stream.Collectors.toList());

        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error("标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error("内容不能为空");
        }
        if (userIds == null || userIds.isEmpty()) {
            return ApiResponse.error("目标用户不能为空");
        }

        // 解析通知类型，默认为系统公告
        NotificationType type = NotificationType.SYSTEM_ANNOUNCEMENT;
        if (typeStr != null && !typeStr.trim().isEmpty()) {
            try {
                type = NotificationType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ApiResponse.error("无效的通知类型: " + typeStr);
            }
        }

        notificationService.sendNotificationToUsers(userIds, title.trim(), content.trim(), type, null);
        return ApiResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/statistics")
    @Operation(summary = "获取通知统计信息（管理员）")
    public ApiResponse<Map<String, Object>> getNotificationStatistics() {
        Map<String, Object> statistics = notificationService.getNotificationStatistics();
        return ApiResponse.success(statistics);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    @Operation(summary = "获取所有通知列表（管理员）")
    public ApiResponse<Page<NotificationDto>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead) {

        Page<NotificationDto> notifications = notificationService.getAllNotifications(page, size, type, isRead);
        return ApiResponse.success(notifications);
    }
}


