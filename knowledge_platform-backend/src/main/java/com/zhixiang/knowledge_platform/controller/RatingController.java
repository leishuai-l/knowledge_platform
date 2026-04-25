package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.RatingCreateRequest;
import com.zhixiang.knowledge_platform.dto.response.RatingInfoResponse;
import com.zhixiang.knowledge_platform.entity.Rating;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分管理控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "评分管理", description = "文档评分相关接口")
@SecurityRequirement(name = "bearerAuth")
public class RatingController {

    private final RatingService ratingService;

    /**
     * 创建或更新评分
     */
    @PostMapping("/documents/{documentId}")
    @Operation(summary = "创建或更新文档评分")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RatingInfoResponse>> createOrUpdateRating(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Valid @RequestBody RatingCreateRequest request,
            HttpServletRequest httpRequest) {

        // 确保路径参数和请求体中的documentId一致
        if (!documentId.equals(request.getDocumentId())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "路径中的文档ID与请求体中的文档ID不匹配"));
        }

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        RatingInfoResponse rating = ratingService.createOrUpdateRating(documentId, userId, request);

        return ResponseEntity.ok(ApiResponse.success(rating));
    }

    /**
     * 获取文档评分列表
     */
    @GetMapping("/documents/{documentId}")
    @Operation(summary = "获取文档评分列表")
    public ResponseEntity<ApiResponse<List<Rating>>> getDocumentRatings(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {

        List<Rating> ratings = ratingService.getDocumentRatings(documentId);
        return ResponseEntity.ok(ApiResponse.success(ratings));
    }

    /**
     * 获取用户对文档的评分
     */
    @GetMapping("/documents/{documentId}/user")
    @Operation(summary = "获取用户对文档的评分")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Rating>> getUserRatingForDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        Rating rating = ratingService.getUserRatingForDocument(documentId, userId);

        return ResponseEntity.ok(ApiResponse.success(rating));
    }

    /**
     * 获取用户的评分记录
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户的评分记录")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Rating>>> getUserRatings(
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        List<Rating> ratings = ratingService.getUserRatings(userId);

        return ResponseEntity.ok(ApiResponse.success(ratings));
    }

    /**
     * 删除评分
     */
    @DeleteMapping("/{ratingId}")
    @Operation(summary = "删除评分")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @Parameter(description = "评分ID") @PathVariable Long ratingId,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        ratingService.deleteRating(ratingId, userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 管理员删除评分
     */
    @DeleteMapping("/admin/{ratingId}")
    @Operation(summary = "管理员删除评分")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteRating(
            @Parameter(description = "评分ID") @PathVariable Long ratingId,
            @Parameter(description = "删除原因") @RequestParam(required = false) String reason) {

        ratingService.adminDeleteRating(ratingId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取文档评分统计
     */
    @GetMapping("/documents/{documentId}/statistics")
    @Operation(summary = "获取文档评分统计")
    public ResponseEntity<ApiResponse<Object>> getDocumentRatingStatistics(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {

        Object statistics = ratingService.getDocumentRatingStatistics(documentId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取用户评分统计
     */
    @GetMapping("/user/statistics")
    @Operation(summary = "获取用户评分统计")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getUserRatingStatistics(
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        Object statistics = ratingService.getUserRatingStatistics(userId);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取最新评分列表
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最新评分列表")
    public ResponseEntity<ApiResponse<List<Rating>>> getRecentRatings(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        List<Rating> ratings = ratingService.getRecentRatings(limit);
        return ResponseEntity.ok(ApiResponse.success(ratings));
    }

    /**
     * 获取高分文档排行
     */
    @GetMapping("/top-documents")
    @Operation(summary = "获取高分文档排行")
    public ResponseEntity<ApiResponse<List<Object[]>>> getTopRatedDocuments(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        List<Object[]> topDocuments = ratingService.getTopRatedDocuments(limit);
        return ResponseEntity.ok(ApiResponse.success(topDocuments));
    }

    /**
     * 检查用户是否已评分
     */
    @GetMapping("/documents/{documentId}/check")
    @Operation(summary = "检查用户是否已评分")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasUserRatedDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        boolean hasRated = ratingService.hasUserRatedDocument(documentId, userId);

        return ResponseEntity.ok(ApiResponse.success(hasRated));
    }
}