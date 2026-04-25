package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.*;
import com.zhixiang.knowledge_platform.dto.response.*;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "社区交流", description = "论坛板块、帖子及回复管理")
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/categories")
    @Operation(summary = "获取板块列表")
    public ResponseEntity<ApiResponse<List<ForumCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(forumService.getAllCategories()));
    }

    @GetMapping("/topics")
    @Operation(summary = "获取帖子列表")
    public ResponseEntity<ApiResponse<PageResponse<ForumTopicResponse>>> getTopics(
            @Parameter(description = "板块ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签") @RequestParam(required = false) String tag,
            @Parameter(description = "状态(0:正常, 3:草稿)") @RequestParam(required = false) Integer status,
            @Parameter(description = "排序(latest:最新, hot:最热)") @RequestParam(defaultValue = "latest") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long currentUserId = JwtAuthenticationFilter.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.getTopics(categoryId, tag, status, sort, page, size, currentUserId)));
    }

    @GetMapping("/topics/{id}")
    @Operation(summary = "获取帖子详情")
    public ResponseEntity<ApiResponse<ForumTopicResponse>> getTopicDetail(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long currentUserId = JwtAuthenticationFilter.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.getTopicDetail(id, currentUserId)));
    }

    @PostMapping("/topics")
    @Operation(summary = "发布新帖子")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForumTopicResponse>> createTopic(
            @Valid @RequestBody ForumTopicCreateRequest createRequest,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.createTopic(userId, createRequest)));
    }

    @PostMapping("/replies")
    @Operation(summary = "发表回复")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ForumReplyResponse>> createReply(
            @Valid @RequestBody ForumReplyCreateRequest createRequest,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.createReply(userId, createRequest)));
    }

    @GetMapping("/topics/{id}/replies")
    @Operation(summary = "获取帖子回复列表")
    public ResponseEntity<ApiResponse<PageResponse<ForumReplyResponse>>> getReplies(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long currentUserId = JwtAuthenticationFilter.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.getReplies(id, page, size, currentUserId)));
    }

    @PostMapping("/topics/{id}/like")
    @Operation(summary = "点赞帖子")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleTopicLike(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleTopicLike(userId, id)));
    }

    @PostMapping("/replies/{id}/like")
    @Operation(summary = "点赞回复")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleReplyLike(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleReplyLike(userId, id)));
    }

    @PostMapping("/topics/{id}/pin")
    @Operation(summary = "置顶/取消置顶帖子")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleTopicPin(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleTopicPin(id)));
    }

    @PostMapping("/topics/{id}/essence")
    @Operation(summary = "加精/取消加精帖子")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleTopicEssence(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleTopicEssence(id)));
    }

    @GetMapping("/tags")
    @Operation(summary = "获取所有标签")
    public ResponseEntity<ApiResponse<List<ForumTagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success(forumService.getAllTags()));
    }

    @GetMapping("/hot-topics")
    @Operation(summary = "获取热门帖子")
    public ResponseEntity<ApiResponse<List<ForumTopicResponse>>> getHotTopics(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(forumService.getHotTopics(limit)));
    }

    @PostMapping("/topics/{id}/collect")
    @Operation(summary = "收藏/取消收藏帖子")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleTopicCollection(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleTopicCollection(userId, id)));
    }

    @GetMapping("/users/collections")
    @Operation(summary = "获取我的收藏")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ForumTopicResponse>>> getUserCollections(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.getUserCollections(userId, page, size)));
    }

    @GetMapping("/users/drafts")
    @Operation(summary = "获取我的草稿")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ForumTopicResponse>>> getUserDrafts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.getUserDrafts(userId, page, size)));
    }

    @PostMapping("/users/{id}/follow")
    @Operation(summary = "关注/取消关注用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> toggleUserFollow(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(forumService.toggleUserFollow(userId, id)));
    }
}
