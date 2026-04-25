package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.ForumReply;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ForumReplyResponse {
    private Long id;
    private String content;
    private UserInfoResponse author;
    private Long topicId;
    private Long parentId;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;

    public static ForumReplyResponse fromEntity(ForumReply reply) {
        return fromEntity(reply, null);
    }

    public static ForumReplyResponse fromEntity(ForumReply reply, Long currentUserId) {
        ForumReplyResponse response = new ForumReplyResponse();
        response.setId(reply.getId());
        response.setContent(reply.getContent());
        if (reply.getAuthor() != null) {
            response.setAuthor(UserInfoResponse.fromEntity(reply.getAuthor()));
        }
        if (reply.getTopic() != null) {
            response.setTopicId(reply.getTopic().getId());
        }
        if (reply.getParent() != null) {
            response.setParentId(reply.getParent().getId());
        }
        response.setLikeCount(reply.getLikeCount() != null ? reply.getLikeCount() : 0);
        response.setCreatedAt(reply.getCreatedAt());
        return response;
    }
}
