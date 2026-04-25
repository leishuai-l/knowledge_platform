package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "分类信息响应")
public class CategoryInfoResponse {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "计算机科学")
    private String name;

    @Schema(description = "父分类ID", example = "null")
    private Long parentId;

    @Schema(description = "分类层级", example = "1")
    private Integer level;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;

    @Schema(description = "分类描述", example = "计算机科学相关课程资料")
    private String description;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    @Schema(description = "子分类列表")
    private List<CategoryInfoResponse> children;

    @Schema(description = "文档数量", example = "25")
    private Long documentCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 从Category实体转换为CategoryInfoResponse
     */
    public static CategoryInfoResponse fromEntity(Category category) {
        CategoryInfoResponse response = new CategoryInfoResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setLevel(category.getLevel());
        response.setSortOrder(category.getSortOrder());
        response.setDescription(category.getDescription());
        response.setIsActive(category.getIsActive());
        response.setCreatedAt(category.getCreatedAt());

        // 转换子分类
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            response.setChildren(category.getChildren().stream()
                    .map(CategoryInfoResponse::fromEntity)
                    .toList());
        }

        return response;
    }

    /**
     * 简化版本转换（不包含子分类）
     */
    public static CategoryInfoResponse fromEntitySimple(Category category) {
        CategoryInfoResponse response = new CategoryInfoResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setLevel(category.getLevel());
        response.setSortOrder(category.getSortOrder());
        response.setDescription(category.getDescription());
        response.setIsActive(category.getIsActive());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }
}