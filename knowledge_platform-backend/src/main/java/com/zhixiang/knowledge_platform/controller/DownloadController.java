package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.DownloadRecordResponse;
import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import com.zhixiang.knowledge_platform.service.DownloadRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 下载记录控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Tag(name = "下载记录", description = "下载记录查询和统计功能")
@RestController
@RequestMapping("/api/downloads")
@RequiredArgsConstructor
@Slf4j
public class DownloadController {

    private final DownloadRecordService downloadRecordService;

    @Operation(summary = "获取用户下载历史", description = "分页获取当前用户的下载历史记录")
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<DownloadRecordResponse>> getUserDownloadHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("用户未认证");
            }
            PageResponse<DownloadRecordResponse> response = downloadRecordService.getUserDownloadHistoryDto(userId, page, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户下载历史失败", e);
            return ApiResponse.error("获取下载历史失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户下载统计", description = "获取当前用户的下载统计信息")
    @GetMapping("/my/statistics")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getUserDownloadStatistics(HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("用户未认证");
            }
            Map<String, Object> statistics = downloadRecordService.getUserDownloadStatistics(userId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取用户下载统计失败", e);
            return ApiResponse.error("获取下载统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检查文档下载状态", description = "检查用户是否已下载过指定文档")
    @GetMapping("/check/{documentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> checkDownloadStatus(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("用户未认证");
            }
            boolean hasDownloaded = downloadRecordService.hasUserDownloadedDocument(userId, documentId);
            return ApiResponse.success(Map.of("hasDownloaded", hasDownloaded));
        } catch (Exception e) {
            log.error("检查下载状态失败，文档ID: {}", documentId, e);
            return ApiResponse.error("检查下载状态失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取文档下载记录", description = "管理员分页获取指定文档的下载记录")
    @GetMapping("/admin/document/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<DownloadRecord>> getDocumentDownloadHistory(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<DownloadRecord> response = downloadRecordService.getDocumentDownloadHistory(documentId, page, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取文档下载记录失败，文档ID: {}", documentId, e);
            return ApiResponse.error("获取文档下载记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取文档下载统计", description = "管理员获取指定文档的下载统计信息")
    @GetMapping("/admin/document/{documentId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getDocumentDownloadStatistics(@PathVariable Long documentId) {
        try {
            Map<String, Object> statistics = downloadRecordService.getDocumentDownloadStatistics(documentId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取文档下载统计失败，文档ID: {}", documentId, e);
            return ApiResponse.error("获取文档下载统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取热门下载文档", description = "管理员获取下载量最高的文档列表")
    @GetMapping("/admin/popular")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Object[]>> getPopularDocuments(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> popularDocuments = downloadRecordService.getPopularDocuments(limit);
            return ApiResponse.success(popularDocuments);
        } catch (Exception e) {
            log.error("获取热门下载文档失败", e);
            return ApiResponse.error("获取热门下载文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取活跃下载用户", description = "管理员获取下载最活跃的用户列表")
    @GetMapping("/admin/active-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Object[]>> getActiveDownloaders(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Object[]> activeDownloaders = downloadRecordService.getActiveDownloaders(limit);
            return ApiResponse.success(activeDownloaders);
        } catch (Exception e) {
            log.error("获取活跃下载用户失败", e);
            return ApiResponse.error("获取活跃下载用户失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取下载趋势数据", description = "管理员获取指定时间范围的下载趋势数据")
    @GetMapping("/admin/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Object[]>> getDownloadTrendData(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            List<Object[]> trendData = downloadRecordService.getDownloadTrendData(start, end);
            return ApiResponse.success(trendData);
        } catch (Exception e) {
            log.error("获取下载趋势数据失败", e);
            return ApiResponse.error("获取下载趋势数据失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取最近下载记录", description = "管理员获取最近的下载记录")
    @GetMapping("/admin/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DownloadRecord>> getRecentDownloads(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<DownloadRecord> recentDownloads = downloadRecordService.getRecentDownloads(limit);
            return ApiResponse.success(recentDownloads);
        } catch (Exception e) {
            log.error("获取最近下载记录失败", e);
            return ApiResponse.error("获取最近下载记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取系统下载统计", description = "管理员获取整个系统的下载统计信息")
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getSystemDownloadStatistics() {
        try {
            Map<String, Object> statistics = downloadRecordService.getSystemDownloadStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取系统下载统计失败", e);
            return ApiResponse.error("获取系统下载统计失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：清理历史下载记录", description = "管理员清理指定月份之前的下载记录")
    @DeleteMapping("/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> cleanHistoryRecords(
            @RequestParam(defaultValue = "12") int retainMonths) {
        try {
            downloadRecordService.cleanHistoryRecords(retainMonths);
            return ApiResponse.success(null, "历史记录清理完成");
        } catch (Exception e) {
            log.error("清理历史下载记录失败", e);
            return ApiResponse.error("清理历史记录失败: " + e.getMessage());
        }
    }
}