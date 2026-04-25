package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.ForumTopicResponse;
import com.zhixiang.knowledge_platform.dto.response.ForumReplyResponse;
import com.zhixiang.knowledge_platform.dto.response.ForumCategoryResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
@Tag(name = "管理员-社区管理")
@PreAuthorize("hasRole('ADMIN')")
public class AdminForumController {

    private final ForumService forumService;

    @GetMapping("/topics")
    @Operation(summary = "获取帖子列表")
    public ResponseEntity<ApiResponse<PageResponse<ForumTopicResponse>>> getTopics(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ForumTopicResponse> topics = forumService.getTopics(categoryId, null, status, "latest", page, size, null);
        return ResponseEntity.ok(ApiResponse.success(topics));
    }

    @PostMapping("/topics/{id}/pin")
    @Operation(summary = "置顶/取消置顶帖子")
    public ResponseEntity<ApiResponse<Void>> togglePin(@PathVariable Long id) {
        forumService.toggleTopicPin(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/topics/{id}/feature")
    @Operation(summary = "设为精华/取消精华")
    public ResponseEntity<ApiResponse<Void>> toggleFeature(@PathVariable Long id) {
        forumService.toggleTopicEssence(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/topics/{id}")
    @Operation(summary = "更新帖子状态")
    public ResponseEntity<ApiResponse<Void>> updateTopicStatus(@PathVariable Long id) {
        forumService.toggleTopicStatus(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/topics/{id}")
    @Operation(summary = "删除帖子")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long id) {
        forumService.deleteTopic(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/replies")
    @Operation(summary = "获取评论列表")
    public ResponseEntity<ApiResponse<PageResponse<ForumReplyResponse>>> getReplies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ForumReplyResponse> replies = forumService.searchReplies(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @DeleteMapping("/replies/{id}")
    @Operation(summary = "删除评论")
    public ResponseEntity<ApiResponse<Void>> deleteReply(@PathVariable Long id) {
        forumService.deleteReply(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取板块列表")
    public ResponseEntity<ApiResponse<List<ForumCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(forumService.getAllCategories()));
    }

    @PostMapping("/categories")
    @Operation(summary = "创建板块")
    public ResponseEntity<ApiResponse<ForumCategoryResponse>> createCategory(@RequestBody ForumCategoryResponse request) {
        ForumCategoryResponse category = forumService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "更新板块")
    public ResponseEntity<ApiResponse<ForumCategoryResponse>> updateCategory(
            @PathVariable Long id, @RequestBody ForumCategoryResponse request) {
        ForumCategoryResponse category = forumService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除板块")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        forumService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
