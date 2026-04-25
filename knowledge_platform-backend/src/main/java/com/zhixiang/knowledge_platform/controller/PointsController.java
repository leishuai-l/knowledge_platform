package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.PointsRecordResponse;
import com.zhixiang.knowledge_platform.enums.PointsType;
import com.zhixiang.knowledge_platform.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 积分系统控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Tag(name = "积分系统", description = "积分查询、统计等功能")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Slf4j
public class PointsController {

    private final PointsService pointsService;

    @Operation(summary = "获取用户当前积分", description = "获取当前登录用户的积分余额")
    @GetMapping("/current")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<Integer> getCurrentPoints(HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            Integer points = pointsService.getUserCurrentPoints(userId);
            return ApiResponse.success(points);
        } catch (Exception e) {
            log.error("获取用户当前积分失败", e);
            return ApiResponse.error("获取当前积分失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户积分记录", description = "分页获取用户的积分记录")
    @GetMapping("/records")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<PointsRecordResponse>> getPointsRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            PageResponse<PointsRecordResponse> records = pointsService.getUserPointsRecords(userId, page, size);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("获取用户积分记录失败", e);
            return ApiResponse.error("获取积分记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据类型获取积分记录", description = "根据积分类型分页获取用户的积分记录")
    @GetMapping("/records/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<PointsRecordResponse>> getPointsRecordsByType(
            @PathVariable PointsType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            PageResponse<PointsRecordResponse> records = pointsService.getUserPointsRecordsByType(userId, type, page, size);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("根据类型获取积分记录失败, 类型: {}", type, e);
            return ApiResponse.error("获取积分记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取今日积分统计", description = "获取用户今日积分获得和消费统计")
    @GetMapping("/today")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<Object> getTodayStatistics(HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            Object statistics = pointsService.getTodayPointsStatistics(userId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取今日积分统计失败", e);
            return ApiResponse.error("获取今日统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户积分统计", description = "获取用户的积分统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Object> getUserStatistics(HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            Object statistics = pointsService.getUserPointsStatistics(userId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取用户积分统计失败", e);
            return ApiResponse.error("获取积分统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户月度积分统计", description = "获取用户的月度积分统计")
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<Object[]>> getMonthlyStatistics(HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            List<Object[]> statistics = pointsService.getUserMonthlyStatistics(userId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取月度积分统计失败", e);
            return ApiResponse.error("获取月度统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取积分排行榜", description = "获取积分排行榜")
    @GetMapping("/leaderboard")
    public ApiResponse<List<Object[]>> getLeaderboard(
            @Parameter(description = "返回数量限制")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> leaderboard = pointsService.getPointsLeaderboard(limit);
            return ApiResponse.success(leaderboard);
        } catch (Exception e) {
            log.error("获取积分排行榜失败", e);
            return ApiResponse.error("获取排行榜失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取最近积分记录", description = "获取系统最近的积分记录")
    @GetMapping("/recent")
    public ApiResponse<List<PointsRecordResponse>> getRecentRecords(
            @Parameter(description = "返回数量限制")
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<PointsRecordResponse> records = pointsService.getRecentRecords(limit);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("获取最近积分记录失败", e);
            return ApiResponse.error("获取最近记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据时间范围获取积分记录", description = "管理员根据时间范围获取积分记录")
    @GetMapping("/admin/records/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<PointsRecordResponse>> getRecordsByDateRange(
            @Parameter(description = "开始日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<PointsRecordResponse> records = pointsService.getRecordsByDateRange(startDate, endDate, page, size);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("根据时间范围获取积分记录失败", e);
            return ApiResponse.error("获取记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员调整用户积分", description = "管理员手动调整用户积分")
    @PostMapping("/admin/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adjustUserPoints(
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "积分变化量（正数为增加，负数为减少）", required = true)
            @RequestParam Integer points,
            @Parameter(description = "调整描述", required = true)
            @RequestParam String description) {
        try {
            pointsService.adminAdjustPoints(userId, points, description);
            return ApiResponse.successMessage("积分调整成功");
        } catch (Exception e) {
            log.error("管理员调整用户积分失败, 用户ID: {}, 积分: {}", userId, points, e);
            return ApiResponse.error("积分调整失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取积分流水统计", description = "管理员获取系统积分流水统计")
    @GetMapping("/admin/flow-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Object[]>> getFlowStatistics() {
        try {
            List<Object[]> statistics = pointsService.getPointsFlowStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取积分流水统计失败", e);
            return ApiResponse.error("获取流水统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清理历史积分记录", description = "管理员清理指定月份前的积分记录")
    @DeleteMapping("/admin/clean-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> cleanHistoryRecords(
            @Parameter(description = "保留的月份数", required = true)
            @RequestParam int retainMonths) {
        try {
            pointsService.cleanHistoryRecords(retainMonths);
            return ApiResponse.successMessage("历史记录清理成功");
        } catch (Exception e) {
            log.error("清理历史积分记录失败, 保留月份: {}", retainMonths, e);
            return ApiResponse.error("清理历史记录失败: " + e.getMessage());
        }
    }
}