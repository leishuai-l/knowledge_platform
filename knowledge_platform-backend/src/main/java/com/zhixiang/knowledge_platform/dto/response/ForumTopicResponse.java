package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.ForumTopic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ForumTopicResponse {
    private Long id;
    private String title;
    private String content;
    private UserInfoResponse author;
    private ForumCategoryResponse category;
    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer collectionCount;
    private java.util.List<ForumTagResponse> tags;
    private Boolean isLiked;
    private Boolean isCollected;
    private Boolean isPinned;
    private Boolean isEssence;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ForumTopicResponse fromEntity(ForumTopic topic) {
        return fromEntity(topic, null);
    }

    public static ForumTopicResponse fromEntity(ForumTopic topic, Long currentUserId) {
        ForumTopicResponse response = new ForumTopicResponse();
        response.setId(topic.getId());
        response.setTitle(topic.getTitle());
        response.setContent(topic.getContent());
        if (topic.getAuthor() != null) {
            response.setAuthor(UserInfoResponse.fromEntity(topic.getAuthor()));
        }
        if (topic.getCategory() != null) {
            response.setCategory(ForumCategoryResponse.fromEntity(topic.getCategory()));
        }
        response.setViewCount(topic.getViewCount());
        response.setReplyCount(topic.getReplyCount());
        response.setLikeCount(topic.getLikeCount() != null ? topic.getLikeCount() : 0);
        response.setCollectionCount(topic.getCollectionCount() != null ? topic.getCollectionCount() : 0);
        
        if (topic.getTags() != null) {
            response.setTags(topic.getTags().stream()
                .map(ForumTagResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList()));
        }

        response.setIsPinned(topic.getIsPinned());
        response.setIsEssence(topic.getIsEssence());
        response.setStatus(topic.getStatus());
        response.setCreatedAt(topic.getCreatedAt());
        response.setUpdatedAt(topic.getUpdatedAt());
        return response;
    }
}
