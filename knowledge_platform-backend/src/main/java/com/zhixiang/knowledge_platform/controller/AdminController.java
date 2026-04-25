package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.AdminPublicDocumentImportRequest;
import com.zhixiang.knowledge_platform.dto.request.AdminUploadsCleanupRequest;
import com.zhixiang.knowledge_platform.dto.response.AdminPublicDocumentImportResponse;
import com.zhixiang.knowledge_platform.dto.response.AdminUploadsCleanupResponse;
import com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.UserInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.CommentInfoResponse;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.ai.RagService;
import com.zhixiang.knowledge_platform.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "管理员功能", description = "管理员后台管理相关接口")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DocumentService documentService;
    private final UserService userService;
    private final PointsService pointsService;
    private final CommentService commentService;
    private final RatingService ratingService;
    private final TagService tagService;
    private final DownloadRecordService downloadRecordService;
    private final AdminService adminService;
    private final AdminDocumentOpsService adminDocumentOpsService;
    private final RagService ragService;

    /**
     * 管理员批量导入公开文档
     */
    @PostMapping("/documents/import-public")
    @Operation(summary = "管理员批量导入公开文档")
    public ResponseEntity<ApiResponse<AdminPublicDocumentImportResponse>> importPublicDocuments(
            @RequestBody AdminPublicDocumentImportRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId(httpServletRequest);
            AdminPublicDocumentImportResponse result = adminDocumentOpsService.importPublicDocuments(request, adminId);
            return ResponseEntity.ok(ApiResponse.success(result, "公开文档导入完成"));
        } catch (Exception e) {
            log.error("导入公开文档失败", e);
            return ResponseEntity.ok(ApiResponse.error("导入公开文档失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员清理 uploads 目录
     */
    @PostMapping("/cleanup/uploads")
    @Operation(summary = "管理员清理 uploads 目录")
    public ResponseEntity<ApiResponse<AdminUploadsCleanupResponse>> cleanupUploads(
            @RequestBody AdminUploadsCleanupRequest request) {
        try {
            AdminUploadsCleanupResponse result = adminDocumentOpsService.cleanupUploads(request);
            String message = Boolean.TRUE.equals(request.getDryRun()) ? "uploads 清理候选清单已生成" : "uploads 清理完成";
            return ResponseEntity.ok(ApiResponse.success(result, message));
        } catch (Exception e) {
            log.error("清理 uploads 失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("清理 uploads 失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统总览统计
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取管理员仪表盘数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData() {
        try {
            Map<String, Object> dashboard = adminService.getDashboardData();
            return ResponseEntity.ok(ApiResponse.success(dashboard));
        } catch (Exception e) {
            log.error("获取管理员仪表盘数据失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取仪表盘数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文档列表（支持筛选）
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表")
    public ResponseEntity<ApiResponse<PageResponse<DocumentInfoResponse>>> getDocuments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "文档状态") @RequestParam(required = false) String status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        try {
            PageResponse<DocumentInfoResponse> documents = adminService.getDocumentsForAdmin(page, size, status, keyword);
            return ResponseEntity.ok(ApiResponse.success(documents));
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取文档列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取待审核文档列表
     */
    @GetMapping("/documents/pending")
    @Operation(summary = "获取待审核文档列表")
    public ResponseEntity<ApiResponse<PageResponse<DocumentInfoResponse>>> getPendingDocuments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        PageResponse<DocumentInfoResponse> documents = documentService.getPendingDocuments(page, size);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * 审核通过文档
     */
    @PostMapping("/documents/{id}/approve")
    @Operation(summary = "审核通过文档")
    public ResponseEntity<ApiResponse<Void>> approveDocument(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        Long adminId = JwtAuthenticationFilter.getCurrentUserId(request);
        documentService.approveDocument(id, adminId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 拒绝文档审核
     */
    @PostMapping("/documents/{id}/reject")
    @Operation(summary = "拒绝文档审核")
    public ResponseEntity<ApiResponse<Void>> rejectDocument(
            @Parameter(description = "文档ID") @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String reason) {

        documentService.rejectDocument(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "管理员删除文档")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        Long adminId = JwtAuthenticationFilter.getCurrentUserId(request);
        documentService.deleteDocument(id, adminId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 用户管理 - 获取用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户列表")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> getAllUsers() {
        try {
            List<UserInfoResponse> users = adminService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 锁定用户
     */
    @PostMapping("/users/{id}/lock")
    @Operation(summary = "锁定用户")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        userService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 解锁用户
     */
    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "解锁用户")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 调整用户积分
     */
    @PostMapping("/users/{id}/points")
    @Operation(summary = "管理员调整用户积分")
    public ResponseEntity<ApiResponse<Void>> adjustUserPoints(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "积分变化（正数为增加，负数为扣除）") @RequestParam Integer points,
            @Parameter(description = "操作描述") @RequestParam String description) {

        pointsService.adminAdjustPoints(id, points, description);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取积分流水统计
     */
    @GetMapping("/points/statistics")
    @Operation(summary = "获取积分流水统计")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPointsFlowStatistics() {
        List<Object[]> statistics = pointsService.getPointsFlowStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取积分排行榜
     */
    @GetMapping("/points/leaderboard")
    @Operation(summary = "获取积分排行榜")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPointsLeaderboard(
            @Parameter(description = "排行榜数量") @RequestParam(defaultValue = "50") int limit) {

        List<Object[]> leaderboard = pointsService.getPointsLeaderboard(limit);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }

    /**
     * 评论管理 - 搜索评论
     */
    @GetMapping("/comments/search")
    @Operation(summary = "搜索评论")
    public ResponseEntity<ApiResponse<PageResponse<CommentInfoResponse>>> searchComments(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        PageResponse<CommentInfoResponse> comments =
                commentService.searchComments(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{id}")
    @Operation(summary = "管理员删除评论")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "评论ID") @PathVariable Long id,
            @Parameter(description = "删除原因") @RequestParam(required = false) String reason) {

        commentService.adminDeleteComment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 删除评分
     */
    @DeleteMapping("/ratings/{id}")
    @Operation(summary = "管理员删除评分")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @Parameter(description = "评分ID") @PathVariable Long id,
            @Parameter(description = "删除原因") @RequestParam(required = false) String reason) {

        ratingService.adminDeleteRating(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量清理未使用的标签
     */
    @DeleteMapping("/tags/unused")
    @Operation(summary = "清理未使用的标签")
    public ResponseEntity<ApiResponse<Integer>> cleanUnusedTags() {
        int deletedCount = tagService.cleanUnusedTags();
        return ResponseEntity.ok(ApiResponse.success(deletedCount));
    }

    /**
     * 获取系统配置信息
     */
    @GetMapping("/system/info")
    @Operation(summary = "获取系统信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemInfo() {
        Map<String, Object> systemInfo = Map.of(
            "version", "0.0.1-SNAPSHOT",
            "javaVersion", System.getProperty("java.version"),
            "springBootVersion", "3.5.5",
            "serverTime", java.time.LocalDateTime.now(),
            "uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime()
        );

        return ResponseEntity.ok(ApiResponse.success(systemInfo));
    }

    /**
     * 获取系统性能监控信息
     */
    @GetMapping("/system/performance")
    @Operation(summary = "获取系统性能监控信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemPerformance() {
        try {
            // 获取运行时信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            // 获取运行时长（毫秒转小时）
            long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
            long uptimeHours = uptimeMs / (1000 * 60 * 60);

            // 获取活跃用户数（简化处理，实际应该从会话管理或Redis获取）
            long onlineUsers = userService.getActiveUserCount();

            // 获取今日访问量（从dashboard数据获取）
            Map<String, Object> dashboardData = adminService.getDashboardData();
            long todayVisits = 0;
            if (dashboardData.containsKey("todayActivity")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> todayActivity = (Map<String, Object>) dashboardData.get("todayActivity");
                if (todayActivity != null) {
                    Object downloadsObj = todayActivity.getOrDefault("downloads", 0);
                    Object uploadsObj = todayActivity.getOrDefault("uploads", 0);
                    int downloads = downloadsObj instanceof Number ? ((Number) downloadsObj).intValue() : 0;
                    int uploads = uploadsObj instanceof Number ? ((Number) uploadsObj).intValue() : 0;
                    todayVisits = downloads + uploads;
                }
            }

            Map<String, Object> performanceData = Map.of(
                "systemInfo", Map.of(
                    "onlineUsers", onlineUsers,
                    "todayVisits", todayVisits,
                    "uptime", uptimeHours
                ),
                "serverStatus", Map.of(
                    "memoryUsage", Math.round((double) usedMemory / maxMemory * 100),
                    "memoryTotal", maxMemory / (1024 * 1024), // MB
                    "memoryUsed", usedMemory / (1024 * 1024), // MB
                    "memoryFree", freeMemory / (1024 * 1024), // MB
                    "availableProcessors", runtime.availableProcessors()
                )
            );

            return ResponseEntity.ok(ApiResponse.success(performanceData));
        } catch (Exception e) {
            log.error("获取系统性能信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取系统性能信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门下载文档
     */
    @GetMapping("/downloads/popular")
    @Operation(summary = "获取热门下载文档")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPopularDownloads(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {

        List<Object[]> popularDocs = downloadRecordService.getPopularDocuments(limit);
        return ResponseEntity.ok(ApiResponse.success(popularDocs));
    }

    /**
     * 获取活跃下载用户
     */
    @GetMapping("/downloads/active-users")
    @Operation(summary = "获取活跃下载用户")
    public ResponseEntity<ApiResponse<List<Object[]>>> getActiveDownloaders(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {

        List<Object[]> activeUsers = downloadRecordService.getActiveDownloaders(limit);
        return ResponseEntity.ok(ApiResponse.success(activeUsers));
    }

    /**
     * 数据清理 - 清理历史积分记录
     */
    @DeleteMapping("/cleanup/points")
    @Operation(summary = "清理历史积分记录")
    public ResponseEntity<ApiResponse<Void>> cleanupPointsHistory(
            @Parameter(description = "保留月数") @RequestParam(defaultValue = "12") int retainMonths) {

        pointsService.cleanHistoryRecords(retainMonths);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 数据清理 - 清理历史下载记录
     */
    @DeleteMapping("/cleanup/downloads")
    @Operation(summary = "清理历史下载记录")
    public ResponseEntity<ApiResponse<Void>> cleanupDownloadHistory(
            @Parameter(description = "保留月数") @RequestParam(defaultValue = "12") int retainMonths) {

        downloadRecordService.cleanHistoryRecords(retainMonths);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量审核文档
     */
    @PostMapping("/documents/batch-approve")
    @Operation(summary = "批量审核通过多个文档")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> batchApproveDocuments(
            @RequestBody List<Long> documentIds,
            HttpServletRequest request) {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId(request);
            Map<String, Integer> result = adminService.batchApproveDocuments(documentIds, adminId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("批量审核文档失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量审核失败: " + e.getMessage()));
        }
    }

    /**
     * 获取内容统计报告
     */
    @GetMapping("/reports/content")
    @Operation(summary = "获取内容统计报告")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContentStatisticsReport() {
        try {
            Map<String, Object> report = adminService.getContentStatisticsReport();
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("获取内容统计报告失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取统计报告失败: " + e.getMessage()));
        }
    }

    /**
     * 禁用用户
     */
    @PostMapping("/users/{id}/disable")
    @Operation(summary = "禁用用户账户")
    public ResponseEntity<ApiResponse<Void>> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "禁用原因") @RequestParam String reason) {
        try {
            adminService.disableUser(id, reason);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("禁用用户失败, ID: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("禁用用户失败: " + e.getMessage()));
        }
    }

    /**
     * 启用用户
     */
    @PostMapping("/users/{id}/enable")
    @Operation(summary = "启用用户账户")
    public ResponseEntity<ApiResponse<Void>> enableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        try {
            adminService.enableUser(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("启用用户失败, ID: {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("启用用户失败: " + e.getMessage()));
        }
    }

    /**
     * 删除不当内容
     */
    @DeleteMapping("/content/{type}/{id}")
    @Operation(summary = "删除不当内容")
    public ResponseEntity<ApiResponse<Void>> removeInappropriateContent(
            @Parameter(description = "内容类型") @PathVariable String type,
            @Parameter(description = "内容ID") @PathVariable Long id,
            @Parameter(description = "删除原因") @RequestParam String reason) {
        try {
            adminService.removeInappropriateContent(type, id, reason);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("删除不当内容失败, 类型: {}, ID: {}", type, id, e);
            return ResponseEntity.ok(ApiResponse.error("删除内容失败: " + e.getMessage()));
        }
    }

    /**
     * 重新向量化单个文档
     */
    @PostMapping("/rag/reingest/{documentId}")
    @Operation(summary = "重新向量化单个文档")
    public ResponseEntity<ApiResponse<Void>> reingestDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        try {
            if (!ragService.isAvailable()) {
                return ResponseEntity.ok(ApiResponse.error("RAG 向量存储不可用，请检查 Qdrant 服务和 AI API 配置"));
            }
            ragService.reingestDocument(documentId);
            return ResponseEntity.ok(ApiResponse.success(null, "文档重新向量化完成"));
        } catch (Exception e) {
            log.error("重新向量化文档失败, documentId: {}", documentId, e);
            return ResponseEntity.ok(ApiResponse.error("重新向量化失败: " + e.getMessage()));
        }
    }

    /**
     * 全量向量化所有已审核通过的文档
     */
    @PostMapping("/rag/ingest-all")
    @Operation(summary = "全量向量化所有已审核通过的文档")
    public ResponseEntity<ApiResponse<Integer>> ingestAllDocuments() {
        try {
            if (!ragService.isAvailable()) {
                return ResponseEntity.ok(ApiResponse.error("RAG 向量存储不可用，请检查 Qdrant 服务和 AI API 配置"));
            }
            ragService.ingestAllDocuments();
            return ResponseEntity.ok(ApiResponse.success(null, "全量向量化完成"));
        } catch (Exception e) {
            log.error("全量向量化失败", e);
            return ResponseEntity.ok(ApiResponse.error("全量向量化失败: " + e.getMessage()));
        }
    }
}
