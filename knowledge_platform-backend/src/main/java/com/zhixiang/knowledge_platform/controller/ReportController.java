package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.AdminReportHandleRequest;
import com.zhixiang.knowledge_platform.dto.request.AdminReportInvestigateRequest;
import com.zhixiang.knowledge_platform.dto.request.ReportSubmitRequest;
import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.enums.ReportType;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.CopyrightProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final CopyrightProtectionService copyrightProtectionService;

    @PostMapping("/reports")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<CopyrightReport> createReport(@Valid @RequestBody ReportSubmitRequest request,
                                                     HttpServletRequest httpRequest) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error("User not authenticated");
        }

        try {
            // 将 ReportSubmitRequest 转换为 ReportRequest
            CopyrightProtectionService.ReportRequest reportRequest = convertToReportRequest(request);
            return ApiResponse.success(copyrightProtectionService.submitReport(userId, reportRequest));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private CopyrightProtectionService.ReportRequest convertToReportRequest(ReportSubmitRequest request) {
        CopyrightProtectionService.ReportRequest reportRequest = new CopyrightProtectionService.ReportRequest();
        reportRequest.setDocumentId(request.getDocumentId());
        reportRequest.setReportType(request.getReportType());
        reportRequest.setDescription(request.getDescription());
        reportRequest.setContactInfo(request.getContactInfo());

        // 合并所有证据链接
        StringBuilder evidenceUrls = new StringBuilder();
        if (request.getEvidenceLinks() != null && !request.getEvidenceLinks().isEmpty()) {
            evidenceUrls.append(String.join(",", request.getEvidenceLinks()));
        }
        if (request.getImageEvidences() != null && !request.getImageEvidences().isEmpty()) {
            if (evidenceUrls.length() > 0) evidenceUrls.append(",");
            evidenceUrls.append(String.join(",", request.getImageEvidences()));
        }
        if (request.getFileEvidences() != null && !request.getFileEvidences().isEmpty()) {
            if (evidenceUrls.length() > 0) evidenceUrls.append(",");
            evidenceUrls.append(String.join(",", request.getFileEvidences()));
        }
        reportRequest.setEvidenceUrls(evidenceUrls.toString());

        return reportRequest;
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<CopyrightReport>> getReports(
            @RequestParam(required = false, name = "status") List<String> statusNames,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ReportStatus> statuses = parseStatuses(statusNames);
            return ApiResponse.success(copyrightProtectionService.getAdminReports(
                    statuses, reportType, documentId, PageRequest.of(page, size)));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/admin/reports/{id}/investigate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CopyrightReport> investigateReport(@PathVariable Long id,
                                                          @Valid @RequestBody AdminReportInvestigateRequest request,
                                                          HttpServletRequest httpRequest) {
        Long handlerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (handlerId == null) {
            return ApiResponse.error("User not authenticated");
        }

        try {
            return ApiResponse.success(copyrightProtectionService.markInvestigating(id, handlerId, request.getComment()));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/admin/reports/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CopyrightReport> handleReport(@PathVariable Long id,
                                                     @Valid @RequestBody AdminReportHandleRequest request,
                                                     HttpServletRequest httpRequest) {
        Long handlerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (handlerId == null) {
            return ApiResponse.error("User not authenticated");
        }

        try {
            return ApiResponse.success(copyrightProtectionService.handleReport(id, handlerId, request));
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/admin/reports/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CopyrightProtectionService.ReportStatistics> getReportStatistics() {
        return ApiResponse.success(copyrightProtectionService.getReportStatistics());
    }

    private List<ReportStatus> parseStatuses(List<String> statusNames) {
        if (statusNames == null || statusNames.isEmpty()) {
            return null;
        }

        List<ReportStatus> statuses = new ArrayList<>();
        for (String statusName : statusNames) {
            if (statusName == null || statusName.isBlank()) {
                continue;
            }
            for (String item : statusName.split(",")) {
                String normalized = item.trim();
                if (normalized.isEmpty()) {
                    continue;
                }
                try {
                    statuses.add(ReportStatus.valueOf(normalized.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("无效的举报状态: " + normalized);
                }
            }
        }

        return statuses.isEmpty() ? null : statuses;
    }
}
