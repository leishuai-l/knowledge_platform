package com.zhixiang.knowledge_platform.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private Long documentId;
    private Long conversationId;
    private List<MessageItem> messageHistory;
}
