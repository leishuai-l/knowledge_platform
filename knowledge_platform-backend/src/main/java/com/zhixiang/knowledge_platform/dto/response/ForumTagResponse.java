package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.ForumTag;
import lombok.Data;

@Data
public class ForumTagResponse {
    private Long id;
    private String name;
    private Integer topicCount;

    public static ForumTagResponse fromEntity(ForumTag tag) {
        ForumTagResponse response = new ForumTagResponse();
        response.setId(tag.getId());
        response.setName(tag.getName());
        response.setTopicCount(tag.getTopicCount());
        return response;
    }
}
