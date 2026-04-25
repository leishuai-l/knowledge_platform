package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 下载记录响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Builder
public class DownloadRecordResponse {

    private Long id;
    private Long documentId;
    private Long userId;
    private Integer pointsCost;
    private String ipAddress;
    private LocalDateTime downloadTime;
    private LocalDateTime createdAt;

    // 文档基本信息
    private DocumentBasicInfo document;

    @Data
    @Builder
    public static class DocumentBasicInfo {
        private Long id;
        private String title;
        private String description;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private String fileExtension;
        private Integer downloadCount;
        private Integer viewCount;
        private java.math.BigDecimal ratingAverage;
        private Integer ratingCount;
        private Integer downloadPoints;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // 分类信息
        private String categoryName;
        private Long categoryId;
    }

    /**
     * 从实体转换为DTO
     */
    public static DownloadRecordResponse fromEntity(DownloadRecord record) {
        DocumentBasicInfo documentInfo = null;

        if (record.getDocument() != null) {
            var doc = record.getDocument();
            documentInfo = DocumentBasicInfo.builder()
                    .id(doc.getId())
                    .title(doc.getTitle())
                    .description(doc.getDescription())
                    .fileName(doc.getFileName())
                    .fileSize(doc.getFileSize())
                    .fileType(doc.getFileType())
                    .fileExtension(doc.getFileExtension())
                    .downloadCount(doc.getDownloadCount())
                    .viewCount(doc.getViewCount())
                    .ratingAverage(doc.getRatingAverage())
                    .ratingCount(doc.getRatingCount())
                    .downloadPoints(doc.getDownloadPoints())
                    .status(doc.getStatus() != null ? doc.getStatus().name() : null)
                    .createdAt(doc.getCreatedAt())
                    .updatedAt(doc.getUpdatedAt())
                    .categoryId(doc.getCategoryId())
                    .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                    .build();
        }

        return DownloadRecordResponse.builder()
                .id(record.getId())
                .documentId(record.getDocumentId())
                .userId(record.getUserId())
                .pointsCost(record.getPointsCost())
                .ipAddress(record.getIpAddress())
                .downloadTime(record.getDownloadTime())
                .createdAt(record.getCreatedAt())
                .document(documentInfo)
                .build();
    }
}