package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新评论请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "更新评论请求")
public class CommentUpdateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    @Schema(description = "评论内容", example = "这个资料很有用，感谢分享！")
    private String content;
}