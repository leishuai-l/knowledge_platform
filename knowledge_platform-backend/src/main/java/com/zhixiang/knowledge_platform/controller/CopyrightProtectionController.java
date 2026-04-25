package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.AdminReportHandleRequest;
import com.zhixiang.knowledge_platform.dto.request.AdminReportInvestigateRequest;
import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.CopyrightProtectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识产权保护 Controller
 */
@RestController
@RequestMapping("/api/copyright")
@RequiredArgsConstructor
@Tag(name = "知识产权保护", description = "侵权举报和版权保护接口")
public class CopyrightProtectionController {

    private final CopyrightProtectionService copyrightService;

    /**
     * 提交侵权举报
     */
    @PostMapping("/reports")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "提交侵权举报")
    public ApiResponse<CopyrightReport> submitReport(
            @RequestBody CopyrightProtectionService.ReportRequest request,
            HttpServletRequest httpRequest) {
        Long reporterId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (reporterId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            CopyrightReport report = copyrightService.submitReport(reporterId, request);
            return ApiResponse.success(report);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("参数错误：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("提交举报失败：" + e.getMessage());
        }
    }

    /**
     * 提交举报反馈
     */
    @PostMapping("/reports/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "提交举报反馈")
    public ApiResponse<CopyrightReport> submitReportWithEvidence(
            @RequestBody CopyrightProtectionService.ReportRequest request,
            HttpServletRequest httpRequest) {
        Long reporterId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (reporterId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            CopyrightReport report = copyrightService.submitReport(reporterId, request);
            return ApiResponse.success(report, "举报提交成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("参数错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("提交举报失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的举报记录
     */
    @GetMapping("/reports/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "获取我的举报记录")
    public ApiResponse<Page<CopyrightReport>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Long reporterId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (reporterId == null) {
            return ApiResponse.error("用户未登录");
        }

        Page<CopyrightReport> reports = copyrightService.getUserReports(
                reporterId, PageRequest.of(page, size));
        return ApiResponse.success(reports);
    }

    /**
     * 获取文档的举报记录
     */
    @GetMapping("/reports/document/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取文档的举报记录")
    public ApiResponse<List<CopyrightReport>> getDocumentReports(
            @PathVariable Long documentId) {
        try {
            List<CopyrightReport> reports = copyrightService.getDocumentReports(documentId);
            return ApiResponse.success(reports);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取待处理的举报列表（管理员）
     */
    @GetMapping("/reports/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待处理的举报列表")
    public ApiResponse<Page<CopyrightReport>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CopyrightReport> reports = copyrightService.getAdminReports(
                List.of(ReportStatus.PENDING, ReportStatus.INVESTIGATING), null, null, PageRequest.of(page, size));
        return ApiResponse.success(reports);
    }

    @PostMapping("/reports/{reportId}/investigate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "将举报标记为调查中")
    public ApiResponse<CopyrightReport> investigateReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportInvestigateRequest request,
            HttpServletRequest httpRequest) {
        Long handlerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (handlerId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            CopyrightReport report = copyrightService.markInvestigating(reportId, handlerId, request.getComment());
            return ApiResponse.success(report);
        } catch (SecurityException e) {
            return ApiResponse.error("权限不足：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("处理举报失败：" + e.getMessage());
        }
    }

    /**
     * 处理举报（管理员）
     */
    @PostMapping("/reports/{reportId}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "处理侵权举报")
    public ApiResponse<CopyrightReport> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportHandleRequest request,
            HttpServletRequest httpRequest) {
        Long handlerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (handlerId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            CopyrightReport report = copyrightService.handleReport(
                    reportId, handlerId, request);
            return ApiResponse.success(report);
        } catch (SecurityException e) {
            return ApiResponse.error("权限不足：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("处理举报失败：" + e.getMessage());
        }
    }

    /**
     * 获取举报统计（管理员）
     */
    @GetMapping("/reports/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取举报统计")
    public ApiResponse<CopyrightProtectionService.ReportStatistics> getReportStatistics() {
        CopyrightProtectionService.ReportStatistics statistics =
                copyrightService.getReportStatistics();
        return ApiResponse.success(statistics);
    }
}