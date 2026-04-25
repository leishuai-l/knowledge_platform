package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统测试控制器
 * 仅允许管理员访问
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "系统测试")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseTestController {

    private final DataSource dataSource;
    private final com.zhixiang.knowledge_platform.service.UserService userService;

    public DatabaseTestController(DataSource dataSource, com.zhixiang.knowledge_platform.service.UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    @GetMapping("/db-connection")
    @Operation(summary = "数据库连接测试")
    public ApiResponse<Map<String, Object>> testDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "connected");
            boolean connected = connection.isValid(5);
            result.put("connected", connected);
            result.put("database", "zhixiang");
            result.put("url", connection.getMetaData().getURL());
            result.put("driver", connection.getMetaData().getDriverName());
            result.put("userCount", userService.getUserCount());
            result.put("connectionValid", connected);
            result.put("timestamp", java.time.LocalDateTime.now());

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("数据库连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "系统状态检查")
    public ApiResponse<Map<String, Object>> checkSystemHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");

            // 数据库状态
            Map<String, Object> database = new HashMap<>();
            try (Connection connection = dataSource.getConnection()) {
                database.put("status", "UP");
                database.put("url", connection.getMetaData().getURL());
            } catch (Exception e) {
                database.put("status", "DOWN");
                database.put("error", e.getMessage());
            }
            health.put("database", database);

            // 应用状态
            Map<String, Object> application = new HashMap<>();
            application.put("status", "RUNNING");
            health.put("application", application);

            // 统计信息
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", userService.getUserCount());
            statistics.put("activeUsers", userService.getActiveUserCount());
            health.put("statistics", statistics);

            health.put("timestamp", java.time.LocalDateTime.now());

            return ApiResponse.success(health);
        } catch (Exception e) {
            return ApiResponse.error("系统状态检查失败: " + e.getMessage());
        }
    }
}
