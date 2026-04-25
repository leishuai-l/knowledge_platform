package com.zhixiang.knowledge_platform.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatConversationResponse {
    private Long id;
    private String title;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
