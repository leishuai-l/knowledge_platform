package com.zhixiang.knowledge_platform.dto.request;

import com.zhixiang.knowledge_platform.enums.ReviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申诉请求 DTO
 */
@Data
public class AppealRequest {

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @NotNull(message = "审核类型不能为空")
    private ReviewType reviewType;

    @NotBlank(message = "申诉理由不能为空")
    @Size(min = 10, max = 1000, message = "申诉理由长度必须在10-1000字符之间")
    private String appealReason;

    @Size(max = 500, message = "证据材料长度不能超过500字符")
    private String evidence;
}
