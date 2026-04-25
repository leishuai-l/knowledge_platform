package com.zhixiang.knowledge_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForumTopicCreateRequest {
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotNull(message = "Category ID cannot be null")
    private Long categoryId;

    private Integer status = 0; // 0: normal, 1: locked, 2: hidden, 3: draft

    private java.util.List<String> tags;
}
