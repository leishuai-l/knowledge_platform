package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.entity.DocumentAppeal;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.DocumentAppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文档申诉管理 Controller
 */
@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
@Tag(name = "文档申诉管理", description = "用户申诉和管理员处理申诉的接口")
public class DocumentAppealController {

    private final DocumentAppealService appealService;

    /**
     * 提交申诉
     */
    @PostMapping
    @Operation(summary = "提交文档审核申诉")
    public ApiResponse<DocumentAppeal> submitAppeal(
            @RequestBody DocumentAppealService.AppealRequest request,
            HttpServletRequest httpRequest) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            DocumentAppeal appeal = appealService.submitAppeal(userId, request);
            return ApiResponse.success(appeal);
        } catch (SecurityException e) {
            return ApiResponse.error("权限不足：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("提交申诉失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的申诉记录
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的申诉记录")
    public ApiResponse<Page<DocumentAppeal>> getMyAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }

        Page<DocumentAppeal> appeals = appealService.getUserAppeals(
                userId, PageRequest.of(page, size));
        return ApiResponse.success(appeals);
    }

    /**
     * 获取待处理的申诉列表（管理员）
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取待处理的申诉列表")
    public ApiResponse<Page<DocumentAppeal>> getPendingAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DocumentAppeal> appeals = appealService.getPendingAppeals(
                PageRequest.of(page, size));
        return ApiResponse.success(appeals);
    }

    /**
     * 处理申诉（管理员）
     */
    @PostMapping("/{appealId}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "处理申诉")
    public ApiResponse<DocumentAppeal> handleAppeal(
            @PathVariable Long appealId,
            @RequestBody DocumentAppealService.AppealHandleRequest request,
            HttpServletRequest httpRequest) {
        Long handlerId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        if (handlerId == null) {
            return ApiResponse.error("用户未登录");
        }

        try {
            DocumentAppeal appeal = appealService.handleAppeal(
                    appealId, handlerId, request);
            return ApiResponse.success(appeal);
        } catch (SecurityException e) {
            return ApiResponse.error("权限不足：" + e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("状态错误：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("处理申诉失败：" + e.getMessage());
        }
    }

    /**
     * 获取申诉统计（管理员）
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取申诉统计")
    public ApiResponse<DocumentAppealService.AppealStatistics> getAppealStatistics() {
        DocumentAppealService.AppealStatistics statistics =
                appealService.getAppealStatistics();
        return ApiResponse.success(statistics);
    }
}
