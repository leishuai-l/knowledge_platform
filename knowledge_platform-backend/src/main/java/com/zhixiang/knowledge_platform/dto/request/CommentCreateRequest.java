package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "创建评论请求")
public class CommentCreateRequest {

    @NotNull(message = "文档ID不能为空")
    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "父评论ID（回复评论时需要）", example = "10")
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    @Schema(description = "评论内容", example = "这个资料很有用，感谢分享！")
    private String content;
}