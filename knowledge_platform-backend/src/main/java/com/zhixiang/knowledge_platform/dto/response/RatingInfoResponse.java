package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.Rating;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评分信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "评分信息响应")
public class RatingInfoResponse {

    @Schema(description = "评分ID", example = "1")
    private Long id;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "评分值", example = "5")
    private Integer score;

    @Schema(description = "评分备注", example = "很有用的资料，推荐下载！")
    private String comment;

    @Schema(description = "用户信息")
    private UserInfoResponse user;

    @Schema(description = "文档标题", example = "Java编程指南")
    private String documentTitle;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从Rating实体转换为RatingInfoResponse
     */
    public static RatingInfoResponse fromEntity(Rating rating) {
        RatingInfoResponse response = new RatingInfoResponse();
        response.setId(rating.getId());
        response.setDocumentId(rating.getDocumentId());
        response.setUserId(rating.getUserId());
        response.setScore(rating.getScore());
        response.setComment(rating.getComment());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());

        // 安全地处理用户信息
        if (rating.getUser() != null) {
            response.setUser(UserInfoResponse.fromEntity(rating.getUser()));
        }

        // 安全地处理文档信息
        if (rating.getDocument() != null) {
            response.setDocumentTitle(rating.getDocument().getTitle());
        }

        return response;
    }

    /**
     * 简化版本转换（不包含关联对象信息）
     */
    public static RatingInfoResponse fromEntitySimple(Rating rating) {
        RatingInfoResponse response = new RatingInfoResponse();
        response.setId(rating.getId());
        response.setDocumentId(rating.getDocumentId());
        response.setUserId(rating.getUserId());
        response.setScore(rating.getScore());
        response.setComment(rating.getComment());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());

        return response;
    }
}