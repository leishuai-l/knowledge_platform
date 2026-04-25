package com.zhixiang.knowledge_platform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文档信息更新请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
public class DocumentUpdateRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;

    @Size(max = 1000, message = "文档描述长度不能超过1000个字符")
    private String description;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Size(max = 10, message = "标签数量不能超过10个")
    private List<String> tagNames;

    @Min(value = 0, message = "下载积分不能小于0")
    @Max(value = 9999, message = "下载积分不能超过9999")
    private Integer downloadPoints;
}