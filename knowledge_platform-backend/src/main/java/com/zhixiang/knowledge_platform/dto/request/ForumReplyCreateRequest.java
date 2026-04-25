package com.zhixiang.knowledge_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForumReplyCreateRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotNull(message = "Topic ID cannot be null")
    private Long topicId;

    private Long parentId; // Optional, for nested replies
}
