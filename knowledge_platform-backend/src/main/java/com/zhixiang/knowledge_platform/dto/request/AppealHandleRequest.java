package com.zhixiang.knowledge_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 申诉处理请求 DTO
 */
@Data
public class AppealHandleRequest {

    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    @NotBlank(message = "处理意见不能为空")
    private String comment;

    @NotBlank(message = "最终决定不能为空")
    private String decision;
}
