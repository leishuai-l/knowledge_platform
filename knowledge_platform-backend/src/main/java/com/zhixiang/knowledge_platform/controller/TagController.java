package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.TagInfoResponse;
import com.zhixiang.knowledge_platform.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "标签管理", description = "标签相关接口")
public class TagController {

    private final TagService tagService;

    /**
     * 获取所有标签
     */
    @GetMapping
    @Operation(summary = "获取所有标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> getAllTags() {
        List<TagInfoResponse> tags = tagService.getAllTags();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 分页获取标签列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取标签列表")
    public ResponseEntity<ApiResponse<PageResponse<TagInfoResponse>>> getTags(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "usage") String sortBy) {

        PageResponse<TagInfoResponse> tags = tagService.getTags(page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> getPopularTags(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") int limit) {

        List<TagInfoResponse> tags = tagService.getPopularTags(limit);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 搜索标签
     */
    @GetMapping("/search")
    @Operation(summary = "搜索标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> searchTags(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {

        List<TagInfoResponse> tags = tagService.searchTagsByName(keyword);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 根据ID获取标签
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取标签")
    public ResponseEntity<ApiResponse<TagInfoResponse>> getTagById(
            @Parameter(description = "标签ID") @PathVariable Long id) {

        TagInfoResponse tag = tagService.getTagById(id);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    /**
     * 创建标签
     */
    @PostMapping
    @Operation(summary = "创建标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagInfoResponse>> createTag(
            @Parameter(description = "标签名称") @RequestParam String name,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color,
            @Parameter(description = "标签描述") @RequestParam(required = false) String description) {

        TagInfoResponse tag = tagService.createTag(name, color, description);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagInfoResponse>> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @Parameter(description = "标签名称") @RequestParam(required = false) String name,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color,
            @Parameter(description = "标签描述") @RequestParam(required = false) String description) {

        TagInfoResponse tag = tagService.updateTag(id, name, color, description);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(
            @Parameter(description = "标签ID") @PathVariable Long id) {

        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 批量创建标签
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> createTags(
            @Parameter(description = "标签名称列表") @RequestBody List<String> tagNames) {

        List<TagInfoResponse> tags = tagService.createTags(tagNames);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 获取未使用的标签 (临时移除权限限制用于测试)
     */
    @GetMapping("/unused")
    @Operation(summary = "获取未使用的标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> getUnusedTags() {
        List<TagInfoResponse> tags = tagService.getUnusedTags();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 清理未使用的标签
     */
    @DeleteMapping("/unused")
    @Operation(summary = "清理未使用的标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> cleanUnusedTags() {
        int deletedCount = tagService.cleanUnusedTags();
        return ResponseEntity.ok(ApiResponse.success(deletedCount));
    }

    /**
     * 获取标签统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取标签统计信息")
    public ResponseEntity<ApiResponse<Object>> getTagStatistics() {
        Object statistics = tagService.getTagStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 根据使用次数范围查询标签
     */
    @GetMapping("/usage-range")
    @Operation(summary = "根据使用次数范围查询标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> getTagsByUsageRange(
            @Parameter(description = "最小使用次数") @RequestParam int minUsage,
            @Parameter(description = "最大使用次数") @RequestParam int maxUsage) {

        List<TagInfoResponse> tags = tagService.getTagsByUsageRange(minUsage, maxUsage);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 获取推荐标签
     */
    @GetMapping("/recommended")
    @Operation(summary = "获取推荐标签")
    public ResponseEntity<ApiResponse<List<TagInfoResponse>>> getRecommendedTags(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {

        List<TagInfoResponse> tags = tagService.getRecommendedTags(limit);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * 验证标签名称
     */
    @GetMapping("/validate")
    @Operation(summary = "验证标签名称")
    public ResponseEntity<ApiResponse<Boolean>> validateTagName(
            @Parameter(description = "标签名称") @RequestParam String name) {

        boolean isValid = tagService.isValidTagName(name);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    /**
     * 标准化标签名称
     */
    @GetMapping("/normalize")
    @Operation(summary = "标准化标签名称")
    public ResponseEntity<ApiResponse<String>> normalizeTagName(
            @Parameter(description = "标签名称") @RequestParam String name) {

        String normalized = tagService.normalizeTagName(name);
        return ResponseEntity.ok(ApiResponse.success(normalized));
    }

    /**
     * 初始化默认标签
     */
    @PostMapping("/initialize")
    @Operation(summary = "初始化默认标签")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultTags() {
        tagService.initializeDefaultTags();
        return ResponseEntity.ok(ApiResponse.success(null, "默认标签初始化完成"));
    }
}