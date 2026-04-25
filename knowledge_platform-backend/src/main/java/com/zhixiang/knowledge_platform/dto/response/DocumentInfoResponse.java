package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "文档信息响应")
public class DocumentInfoResponse {

    @Schema(description = "文档ID", example = "1")
    private Long id;

    @Schema(description = "文档标题", example = "Java编程基础教程")
    private String title;

    @Schema(description = "文档描述", example = "适合初学者的Java编程入门教程")
    private String description;

    @Schema(description = "原始文件名", example = "java_tutorial.pdf")
    private String fileName;

    @Schema(description = "文件大小", example = "2048576")
    private Long fileSize;

    @Schema(description = "文件大小（可读格式）", example = "2.0 MB")
    private String readableFileSize;

    @Schema(description = "文件类型", example = "application/pdf")
    private String fileType;

    @Schema(description = "文件扩展名", example = "pdf")
    private String fileExtension;

    @Schema(description = "分类信息")
    private CategoryInfoResponse category;

    @Schema(description = "上传者信息")
    private UserInfoResponse uploader;

    @Schema(description = "上传者ID", example = "1")
    private Long uploaderId;

    @Schema(description = "文档状态", example = "APPROVED")
    private DocumentStatus status;

    @Schema(description = "下载次数", example = "158")
    private Integer downloadCount;

    @Schema(description = "平均评分", example = "4.2")
    private BigDecimal ratingAverage;

    @Schema(description = "评分人数", example = "25")
    private Integer ratingCount;

    @Schema(description = "下载所需积分", example = "5")
    private Integer downloadPoints;

    @Schema(description = "标签列表")
    private List<TagInfoResponse> tags;

    @Schema(description = "审核时间")
    private LocalDateTime approvedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "AI 摘要")
    private String aiSummary;

    @Schema(description = "AI 分析状态: PENDING-待分析, PROCESSING-分析中, COMPLETED-已完成, FAILED-失败, SKIPPED-跳过")
    private String aiAnalysisStatus;

    /**
     * 从Document实体转换为DocumentInfoResponse
     */
    public static DocumentInfoResponse fromEntity(Document document) {
        DocumentInfoResponse response = new DocumentInfoResponse();
        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setDescription(document.getDescription());
        response.setAiSummary(document.getAiSummary());
        response.setAiAnalysisStatus(document.getAiAnalysisStatus());
        response.setFileName(document.getFileName());
        response.setFileSize(document.getFileSize());
        response.setReadableFileSize(document.getReadableFileSize());
        response.setFileType(document.getFileType());
        response.setFileExtension(document.getFileExtension());
        response.setStatus(document.getStatus());
        response.setDownloadCount(document.getDownloadCount());
        response.setRatingAverage(document.getRatingAverage());
        response.setRatingCount(document.getRatingCount());
        response.setDownloadPoints(document.getDownloadPoints());
        response.setApprovedAt(document.getApprovedAt());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setUploaderId(document.getUploaderId());

        // 设置关联信息
        if (document.getCategory() != null) {
            response.setCategory(CategoryInfoResponse.fromEntity(document.getCategory()));
        }
        if (document.getUploader() != null) {
            response.setUploader(UserInfoResponse.fromEntity(document.getUploader()));
        }
        if (document.getTags() != null) {
            response.setTags(document.getTags().stream()
                    .map(TagInfoResponse::fromEntity)
                    .toList());
        }

        return response;
    }
}