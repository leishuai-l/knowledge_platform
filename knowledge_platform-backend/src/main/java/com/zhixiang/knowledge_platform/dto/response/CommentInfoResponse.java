package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "评论信息响应")
public class CommentInfoResponse {

    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "父评论ID", example = "null")
    private Long parentId;

    @Schema(description = "评论内容", example = "这个资料很有用，感谢分享！")
    private String content;

    @Schema(description = "评论者信息")
    private UserInfoResponse user;

    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;

    @Schema(description = "用户头像URL", example = "/avatars/user_1.jpg")
    private String userAvatar;

    @Schema(description = "父评论用户昵称", example = "李四")
    private String parentUserNickname;

    @Schema(description = "回复列表")
    private List<CommentInfoResponse> replies;

    @Schema(description = "回复数量", example = "3")
    private Integer replyCount;

    @Schema(description = "是否已删除", example = "false")
    private Boolean isDeleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从Comment实体转换为CommentInfoResponse
     */
    public static CommentInfoResponse fromEntity(Comment comment) {
        CommentInfoResponse response = new CommentInfoResponse();
        response.setId(comment.getId());
        response.setDocumentId(comment.getDocumentId());
        response.setParentId(comment.getParentId());
        response.setContent(comment.getDisplayContent()); // 使用显示内容（已删除的评论会显示占位符）
        response.setIsDeleted(comment.getIsDeleted());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        // 设置评论者信息
        if (comment.getUser() != null) {
            response.setUser(UserInfoResponse.fromEntity(comment.getUser()));
        }

        // 转换回复列表
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            response.setReplies(comment.getReplies().stream()
                    .filter(reply -> !reply.getIsDeleted()) // 过滤已删除的回复
                    .map(CommentInfoResponse::fromEntity)
                    .toList());
            response.setReplyCount(response.getReplies().size());
        } else {
            response.setReplyCount(0);
        }

        return response;
    }

    /**
     * 简化版本转换（不包含回复列表）
     */
    public static CommentInfoResponse fromEntitySimple(Comment comment) {
        CommentInfoResponse response = new CommentInfoResponse();
        response.setId(comment.getId());
        response.setDocumentId(comment.getDocumentId());
        response.setParentId(comment.getParentId());
        response.setContent(comment.getDisplayContent());
        response.setIsDeleted(comment.getIsDeleted());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        if (comment.getUser() != null) {
            response.setUser(UserInfoResponse.fromEntity(comment.getUser()));
        }

        return response;
    }
}