package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文档上传请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "文档上传请求")
public class DocumentUploadRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    @Schema(description = "文档标题", example = "Java编程基础教程")
    private String title;

    @Size(max = 1000, message = "文档描述长度不能超过1000个字符")
    @Schema(description = "文档描述", example = "适合初学者的Java编程入门教程，包含基础语法和常用API")
    private String description;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "7")
    private Long categoryId;

    @Schema(description = "标签列表", example = "[\"课件\", \"Java\", \"编程基础\"]")
    private List<String> tags;

    @Min(value = 0, message = "下载积分不能小于0")
    @Max(value = 9999, message = "下载积分不能超过9999")
    @Schema(description = "下载所需积分", example = "0")
    private Integer downloadPoints = 0;
}