package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理员上传目录清理请求")
public class AdminUploadsCleanupRequest {

    @NotNull(message = "dryRun不能为空")
    @Schema(description = "是否仅预览候选清单而不执行删除", example = "true")
    private Boolean dryRun = true;

    @Schema(description = "是否清理预览缓存目录", example = "true")
    private boolean cleanupPreviews = true;

    @Schema(description = "是否清理临时目录", example = "true")
    private boolean cleanupTemp = true;

    @Schema(description = "是否清理文档根目录下的高置信演示文件", example = "true")
    private boolean cleanupDemoRootFiles = true;

    @Schema(description = "是否清理 documents/2026/04 候选演示文件", example = "false")
    private boolean cleanupApril2026DemoFiles = false;

    @Schema(description = "是否清理 documents/2025、documents/2026/03、avatars、reports 下的孤儿文件", example = "false")
    private boolean cleanupOrphanFiles = false;
}
