package com.zhixiang.knowledge_platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文档响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上传文档响应")
public class UploadDocumentResponse {

    @Schema(description = "文档信息")
    private DocumentInfoResponse document;

    @Schema(description = "初审结果", example = "PASSED")
    private InitialReviewOutcome initialReviewOutcome;

    @Schema(description = "拒绝原因")
    private String rejectionReason;

    @Schema(description = "修改建议")
    private String suggestions;

    @Schema(description = "是否应跳转到我的上传", example = "true")
    private boolean shouldRedirectToMyUploads;

    @Schema(description = "是否可以直接重试上传", example = "false")
    private boolean canRetryUpload;

    public enum InitialReviewOutcome {
        PASSED,
        REJECTED,
        SYSTEM_REJECTED
    }
}
