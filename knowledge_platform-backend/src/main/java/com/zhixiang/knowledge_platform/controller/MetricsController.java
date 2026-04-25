package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.service.BusinessMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 业务监控控制器
 * 提供业务指标和监控数据的API接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "业务监控", description = "业务指标和监控相关接口")
@SecurityRequirement(name = "bearerAuth")
public class MetricsController {

    private final BusinessMetricsService businessMetricsService;

    /**
     * 获取实时业务指标
     */
    @GetMapping("/realtime")
    @Operation(summary = "获取实时业务指标")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtimeMetrics() {
        try {
            Map<String, Object> metrics = businessMetricsService.getRealtimeMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            log.error("获取实时指标失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取实时指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取API性能指标
     */
    @GetMapping("/api-performance")
    @Operation(summary = "获取API性能指标")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiPerformanceMetrics() {
        try {
            Map<String, Object> metrics = businessMetricsService.getApiPerformanceMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            log.error("获取API性能指标失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取API性能指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户行为指标
     */
    @GetMapping("/user-behavior")
    @Operation(summary = "获取用户行为指标")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserBehaviorMetrics() {
        try {
            Map<String, Object> metrics = businessMetricsService.getUserBehaviorMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            log.error("获取用户行为指标失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取用户行为指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取历史趋势数据
     */
    @GetMapping("/trends")
    @Operation(summary = "获取历史趋势数据")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistoricalTrends(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        try {
            if (days < 1 || days > 90) {
                return ResponseEntity.ok(ApiResponse.error("天数范围应在1-90之间"));
            }

            Map<String, Object> trends = businessMetricsService.getHistoricalTrends(days);
            return ResponseEntity.ok(ApiResponse.success(trends));
        } catch (Exception e) {
            log.error("获取历史趋势数据失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取历史趋势数据失败: " + e.getMessage()));
        }
    }

    /**
     * 记录用户访问（供前端调用）
     */
    @PostMapping("/record/visit")
    @Operation(summary = "记录用户访问")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> recordUserVisit(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action) {
        try {
            businessMetricsService.recordUserVisit(userId, action);
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("记录用户访问失败", e);
            return ResponseEntity.ok(ApiResponse.error("记录访问失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发数据清理
     */
    @PostMapping("/cleanup")
    @Operation(summary = "手动触发数据清理")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerDataCleanup() {
        try {
            businessMetricsService.cleanupExpiredData();
            log.info("手动触发指标数据清理完成");
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("手动触发数据清理失败", e);
            return ResponseEntity.ok(ApiResponse.error("数据清理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now(),
                "uptime", getSystemUptime(),
                "version", "0.0.1-SNAPSHOT"
            );

            return ResponseEntity.ok(ApiResponse.success(health));
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取健康状态失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统运行时间
     */
    private long getSystemUptime() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }
}