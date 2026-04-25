package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 文档搜索请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "文档搜索请求")
public class DocumentSearchRequest {

    @Schema(description = "搜索关键词", example = "Java")
    private String keyword;

    @Schema(description = "分类ID", example = "7")
    private Long categoryId;

    @Schema(description = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;

    @Schema(description = "文件类型", example = "application/pdf")
    private String fileType;

    @Schema(description = "文件扩展名", example = "pdf")
    private String fileExtension;

    @Schema(description = "上传者ID", example = "1")
    private Long uploaderId;

    @Schema(description = "最小评分", example = "3.0")
    private Double minRating;

    @Schema(description = "排序字段", example = "createdAt", allowableValues = {"createdAt", "downloadCount", "ratingAverage", "title"})
    private String sortBy = "createdAt";

    @Schema(description = "排序方向", example = "desc", allowableValues = {"asc", "desc"})
    private String sortDirection = "desc";

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}