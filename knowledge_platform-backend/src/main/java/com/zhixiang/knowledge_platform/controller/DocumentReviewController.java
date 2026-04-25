package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.DocumentFinalReviewService;
import com.zhixiang.knowledge_platform.service.DocumentInitialReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文档审核管理 Controller
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "文档审核管理", description = "管理员审核文档的接口")
public class DocumentReviewController {

    private final DocumentInitialReviewService initialReviewService;
    private final DocumentFinalReviewService finalReviewService;

    /**
     * 获取待复审的文档列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待复审的文档列表")
    public ApiResponse<Page<DocumentReview>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DocumentReview> reviews = finalReviewService.getPendingReviews(
                PageRequest.of(page, size));
        return ApiResponse.success(reviews);
    }

    /**
     * 执行文档复审
     */
    @PostMapping("/{documentId}/final-review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "执行文档复审")
    public ApiResponse<DocumentReview> performFinalReview(
            @PathVariable Long documentId,
            @RequestBody DocumentFinalReviewService.FinalReviewRequest request,
            HttpServletRequest httpRequest) {
        Long reviewerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (reviewerId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            DocumentReview review = finalReviewService.performFinalReview(
                    documentId, reviewerId, request);
            return ApiResponse.success(review);
        } catch (SecurityException e) {
            return ApiResponse.error("权限不足：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("复审失败：" + e.getMessage());
        }
    }

    /**
     * 获取审核统计
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取审核统计")
    public ApiResponse<DocumentFinalReviewService.ReviewStatistics> getReviewStatistics() {
        DocumentFinalReviewService.ReviewStatistics statistics =
                finalReviewService.getReviewStatistics();
        return ApiResponse.success(statistics);
    }
}
