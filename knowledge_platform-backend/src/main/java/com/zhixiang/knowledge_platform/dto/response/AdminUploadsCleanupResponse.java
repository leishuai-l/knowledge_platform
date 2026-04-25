package com.zhixiang.knowledge_platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理员上传目录清理结果")
public class AdminUploadsCleanupResponse {

    @Schema(description = "是否为 dry-run")
    private boolean dryRun;

    @Schema(description = "候选数量")
    private int candidateCount;

    @Schema(description = "实际删除数量")
    private int deletedCount;

    @Schema(description = "跳过数量")
    private int skippedCount;

    @Schema(description = "失败数量")
    private int failedCount;

    @Schema(description = "清理项明细")
    private List<CleanupItemResult> items = new ArrayList<>();

    @Data
    @Schema(description = "单个清理项结果")
    public static class CleanupItemResult {
        @Schema(description = "相对路径")
        private String relativePath;

        @Schema(description = "清理分组")
        private String group;

        @Schema(description = "是否有数据库引用")
        private boolean hasDatabaseReference;

        @Schema(description = "数据库引用说明")
        private String referenceDetail;

        @Schema(description = "是否实际删除")
        private boolean deleted;

        @Schema(description = "结果消息")
        private String message;
    }
}
