package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final NotificationService notificationService;

    /**
     * 处理客户端连接
     */
    @MessageMapping("/connect")
    @SendToUser("/queue/connect-response")
    public Map<String, Object> handleConnect(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("WebSocket用户连接: userId={}", userId);

        // 可以在这里执行连接后的初始化操作
        // 比如发送未读通知数量等

        return Map.of(
            "status", "connected",
            "message", "WebSocket连接成功",
            "userId", userId
        );
    }

    /**
     * 处理心跳消息
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat-response")
    public Map<String, Object> handleHeartbeat(@Payload Map<String, Object> message, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.debug("收到心跳消息: userId={}", userId);

        return Map.of(
            "status", "alive",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 处理通知已读状态更新
     */
    @MessageMapping("/notification/read")
    public void handleNotificationRead(@Payload Map<String, Object> message, Principal principal) {
        if (principal == null) {
            log.warn("未认证用户尝试更新通知状态");
            return;
        }

        Long userId = Long.parseLong(principal.getName());
        Object notificationIdObj = message.get("notificationId");

        if (notificationIdObj != null) {
            Long notificationId = Long.parseLong(notificationIdObj.toString());
            notificationService.markNotificationAsRead(notificationId, userId);
            log.info("通知已标记为已读: notificationId={}, userId={}", notificationId, userId);
        }
    }

    /**
     * 处理获取未读通知数量请求
     */
    @MessageMapping("/notification/unread-count")
    @SendToUser("/queue/unread-count")
    public Map<String, Object> getUnreadCount(Principal principal) {
        if (principal == null) {
            return Map.of("count", 0, "error", "未认证");
        }

        Long userId = Long.parseLong(principal.getName());
        Long unreadCount = notificationService.getUnreadNotificationCount(userId);

        return Map.of("count", unreadCount);
    }
}