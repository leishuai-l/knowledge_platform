package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.ForumCategory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ForumCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ForumCategoryResponse fromEntity(ForumCategory category) {
        ForumCategoryResponse response = new ForumCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setIcon(category.getIcon());
        response.setSortOrder(category.getSortOrder());
        response.setStatus(category.getStatus());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
