package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "标签信息响应")
public class TagInfoResponse {

    @Schema(description = "标签ID", example = "1")
    private Long id;

    @Schema(description = "标签名称", example = "课件")
    private String name;

    @Schema(description = "标签颜色", example = "#409EFF")
    private String color;

    @Schema(description = "使用次数", example = "158")
    private Integer usageCount;

    @Schema(description = "标签描述", example = "用于标识课程相关文档")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从Tag实体转换为TagInfoResponse
     */
    public static TagInfoResponse fromEntity(Tag tag) {
        TagInfoResponse response = new TagInfoResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setColor(tag.getColor());
        response.setDescription(tag.getDescription());
        response.setUsageCount(tag.getUsageCount());
        response.setCreatedAt(tag.getCreatedAt());
        response.setUpdatedAt(tag.getUpdatedAt());
        return response;
    }
}