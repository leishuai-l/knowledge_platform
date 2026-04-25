package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.CommentCreateRequest;
import com.zhixiang.knowledge_platform.dto.request.CommentUpdateRequest;
import com.zhixiang.knowledge_platform.dto.response.CommentInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.CommentService;
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
import java.util.Map;

/**
 * 评论管理控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "评论管理", description = "文档评论相关接口")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final CommentService commentService;

    /**
     * 创建评论
     */
    @PostMapping("/documents/{documentId}")
    @Operation(summary = "创建文档评论")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentInfoResponse>> createComment(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest) {

        // 确保路径参数和请求体中的documentId一致
        if (!documentId.equals(request.getDocumentId())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "路径中的文档ID与请求体中的文档ID不匹配"));
        }

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        CommentInfoResponse comment = commentService.createComment(documentId, userId, request);

        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    /**
     * 获取文档评论列表
     */
    @GetMapping("/documents/{documentId}")
    @Operation(summary = "获取文档评论列表")
    public ResponseEntity<ApiResponse<PageResponse<CommentInfoResponse>>> getDocumentComments(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        PageResponse<CommentInfoResponse> comments = commentService.getDocumentComments(documentId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取评论的回复列表
     */
    @GetMapping("/{parentId}/replies")
    @Operation(summary = "获取评论的回复列表")
    public ResponseEntity<ApiResponse<List<CommentInfoResponse>>> getCommentReplies(
            @Parameter(description = "父评论ID") @PathVariable Long parentId) {

        List<CommentInfoResponse> replies = commentService.getCommentReplies(parentId);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    /**
     * 获取用户评论列表
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户评论列表")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CommentInfoResponse>>> getUserComments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        PageResponse<CommentInfoResponse> comments = commentService.getUserComments(userId, page, size);

        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取评论详情
     */
    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情")
    public ResponseEntity<ApiResponse<CommentInfoResponse>> getCommentById(
            @Parameter(description = "评论ID") @PathVariable Long commentId) {

        CommentInfoResponse comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    /**
     * 更新评论内容
     */
    @PutMapping("/{commentId}")
    @Operation(summary = "更新评论内容")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentInfoResponse>> updateComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        CommentInfoResponse comment = commentService.updateComment(commentId, userId, request.getContent());

        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        commentService.deleteComment(commentId, userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 管理员删除评论
     */
    @DeleteMapping("/admin/{commentId}")
    @Operation(summary = "管理员删除评论")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            @Parameter(description = "删除原因") @RequestParam(required = false) String reason) {

        commentService.adminDeleteComment(commentId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取文档评论统计
     */
    @GetMapping("/documents/{documentId}/statistics")
    @Operation(summary = "获取文档评论统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentCommentStatistics(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {

        Map<String, Object> statistics = commentService.getDocumentCommentStatistics(documentId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取用户评论统计
     */
    @GetMapping("/user/statistics")
    @Operation(summary = "获取用户评论统计")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCommentStatistics(
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        Map<String, Object> statistics = commentService.getUserCommentStatistics(userId);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 搜索评论
     */
    @GetMapping("/search")
    @Operation(summary = "搜索评论")
    @PreAuthorize("hasRole('ADMIN')") // 仅管理员可搜索评论
    public ResponseEntity<ApiResponse<PageResponse<CommentInfoResponse>>> searchComments(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        PageResponse<CommentInfoResponse> comments = commentService.searchComments(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取最新评论
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最新评论")
    public ResponseEntity<ApiResponse<List<CommentInfoResponse>>> getRecentComments(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        List<CommentInfoResponse> comments = commentService.getRecentComments(limit);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取热门评论
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门评论")
    public ResponseEntity<ApiResponse<List<CommentInfoResponse>>> getPopularComments(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        List<CommentInfoResponse> comments = commentService.getPopularComments(limit);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 检查用户是否已评论过文档
     */
    @GetMapping("/documents/{documentId}/check")
    @Operation(summary = "检查用户是否已评论过文档")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasUserCommentedDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletRequest httpRequest) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        boolean hasCommented = commentService.hasUserCommentedDocument(documentId, userId);

        return ResponseEntity.ok(ApiResponse.success(hasCommented));
    }
}