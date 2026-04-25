package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理员公开文档导入请求")
public class AdminPublicDocumentImportRequest {

    @NotEmpty(message = "导入条目不能为空")
    @Size(max = 20, message = "单次最多导入20个文件")
    @Valid
    @Schema(description = "待导入的公开文档条目")
    private List<ImportItem> items = new ArrayList<>();

    @Data
    @Schema(description = "公开文档导入条目")
    public static class ImportItem {

        @NotBlank(message = "文件URL不能为空")
        @Size(max = 2000, message = "文件URL过长")
        @Schema(description = "公开可访问的文件URL", example = "https://example.com/files/java-guide.pdf")
        private String url;

        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题长度不能超过200个字符")
        @Schema(description = "文档标题", example = "Java 编程公开教程")
        private String title;

        @Size(max = 1000, message = "描述长度不能超过1000个字符")
        @Schema(description = "文档描述")
        private String description;

        @NotNull(message = "分类ID不能为空")
        @Schema(description = "分类ID", example = "7")
        private Long categoryId;

        @Min(value = 0, message = "下载积分不能小于0")
        @Max(value = 9999, message = "下载积分不能超过9999")
        @Schema(description = "下载积分", example = "0")
        private Integer downloadPoints = 0;

        @Schema(description = "标签列表")
        private List<String> tags = new ArrayList<>();
    }
}
