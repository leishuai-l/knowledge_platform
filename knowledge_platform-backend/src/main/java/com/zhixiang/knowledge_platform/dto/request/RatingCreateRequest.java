package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评分请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "创建评分请求")
public class RatingCreateRequest {

    @NotNull(message = "文档ID不能为空")
    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    @Schema(description = "评分（1-5星）", example = "4")
    private Integer score;

    @Size(max = 500, message = "评分评论长度不能超过500个字符")
    @Schema(description = "评分评论", example = "这个文档很有用!")
    private String comment;
}