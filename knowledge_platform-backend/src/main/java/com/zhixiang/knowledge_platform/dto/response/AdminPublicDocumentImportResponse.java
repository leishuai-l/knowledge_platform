package com.zhixiang.knowledge_platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理员公开文档导入结果")
public class AdminPublicDocumentImportResponse {

    @Schema(description = "成功数量")
    private int successCount;

    @Schema(description = "失败数量")
    private int failureCount;

    @Schema(description = "导入结果明细")
    private List<ImportItemResult> items = new ArrayList<>();

    @Data
    @Schema(description = "单个导入条目结果")
    public static class ImportItemResult {
        @Schema(description = "源URL")
        private String url;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "是否成功")
        private boolean success;

        @Schema(description = "结果消息")
        private String message;

        @Schema(description = "创建的文档ID")
        private Long documentId;

        @Schema(description = "保存的相对路径")
        private String relativePath;

        @Schema(description = "文档状态")
        private String documentStatus;
    }
}
