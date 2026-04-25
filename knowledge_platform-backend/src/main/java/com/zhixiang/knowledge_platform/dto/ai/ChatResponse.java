package com.zhixiang.knowledge_platform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private Long conversationId;

    public ChatResponse(String answer) {
        this.answer = answer;
    }
}
