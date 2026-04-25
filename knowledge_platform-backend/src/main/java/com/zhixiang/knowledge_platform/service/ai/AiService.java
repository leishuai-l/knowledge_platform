package com.zhixiang.knowledge_platform.service.ai;

import com.zhixiang.knowledge_platform.dto.ai.ChatRequest;
import com.zhixiang.knowledge_platform.dto.ai.ChatResponse;
import com.zhixiang.knowledge_platform.dto.ai.MessageItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixiang.knowledge_platform.entity.ChatConversation;
import com.zhixiang.knowledge_platform.dto.response.ChatConversationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@ConditionalOnProperty(name = "zhixiang.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient.Builder chatClientBuilder;
    private final RagService ragService;

    private final com.zhixiang.knowledge_platform.repository.DocumentRepository documentRepository;
    private final com.zhixiang.knowledge_platform.service.DocumentService documentService;
    private final com.zhixiang.knowledge_platform.repository.ChatConversationRepository chatConversationRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是知享助手，知享校园知识库共享平台的智能AI助手。

            关于知享平台：
            知享是一个校园知识库共享平台，旨在帮助学生和教师分享、管理和获取学习资料。
            平台主要功能包括文档上传与管理、文档预览与下载、智能标签分类、文档搜索与推荐、积分系统、社区论坛交流、AI智能问答与文档分析。

            你的职责：
            帮助用户了解和使用知享平台的各项功能，推荐平台中的优质文档资源，解答关于文档内容的问题，提供学习建议和资料推荐，协助用户进行文档搜索和分类。

            回答原则：
            优先推荐知享平台中的文档资源，回答要结合平台实际功能和文档内容，保持专业、友善、耐心的态度，回答简洁明了、重点突出。

            重要：回答时不要使用编号列表（如1. 2. 3.或29.等），使用自然段落或短横线列表即可。
            """;

    public List<com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse> getRecommendedDocuments(Long userId) {
        // 1. Try to find last interaction (download history)
        var history = documentService.getUserDownloadHistory(userId, 0, 1);

        if (history.getTotal() > 0 && !history.getList().isEmpty()) {
            Long lastDocId = history.getList().get(0).getId();
            // Recommend based on last interaction
            return documentService.getRecommendedDocuments(lastDocId, 6);
        }

        // 2. Fallback to popular documents
        return documentService.getPopularDocuments(0, 6).getList();
    }

    public ChatResponse chat(ChatRequest request, Long userId) {
        String promptText = buildRagPrompt(request);

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(promptText)
                .call()
                .content();

        Long conversationId = null;
        if (userId != null) {
            conversationId = getOrCreateConversation(request, userId);
            saveMessage(conversationId, "user", request.getMessage());
            saveMessage(conversationId, "assistant", response);
        }

        return new ChatResponse(response, conversationId);
    }

    public SseEmitter streamChat(ChatRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(180000L);

        new Thread(() -> {
            try {
                String finalPrompt = buildRagPrompt(request);
                ChatClient chatClient = chatClientBuilder.build();
                StringBuilder fullResponse = new StringBuilder();

                // 获取或创建对话（仅登录用户）
                Long conversationId = null;
                if (userId != null) {
                    conversationId = getOrCreateConversation(request, userId);
                    // 保存用户消息
                    saveMessage(conversationId, "user", request.getMessage());
                    // Send conversationId as first event with special marker
                    emitter.send(SseEmitter.event().name("conversation").data(conversationId.toString()));
                }

                final Long finalConversationId = conversationId;

                Flux<String> stream = chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(finalPrompt)
                        .stream()
                        .content();

                stream.subscribe(
                        content -> {
                            try {
                                if (content != null) {
                                    fullResponse.append(content);
                                    emitter.send(content, MediaType.TEXT_PLAIN);
                                }
                            } catch (IOException e) {
                                log.info("Client disconnected");
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("Error in AI stream", error);
                            if (fullResponse.length() > 0 && finalConversationId != null) {
                                saveMessage(finalConversationId, "assistant", fullResponse.toString());
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            if (finalConversationId != null) {
                                saveMessage(finalConversationId, "assistant", fullResponse.toString());
                            }
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("Failed to initialize stream chat", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private String buildRagPrompt(ChatRequest request) {
        StringBuilder promptBuilder = new StringBuilder();

        // 1. 如果有历史消息，添加到上下文中
        if (request.getMessageHistory() != null && !request.getMessageHistory().isEmpty()) {
            promptBuilder.append("【历史对话】\n");
            for (MessageItem msg : request.getMessageHistory()) {
                String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
                promptBuilder.append(String.format("%s: %s\n", roleName, msg.getContent()));
            }
            promptBuilder.append("\n");
        }

        // 2. 搜索相关文档内容
        StringBuilder contextBuilder = new StringBuilder();
        List<org.springframework.ai.document.Document> similarDocs = ragService.similaritySearch(
            request.getMessage(),
            request.getDocumentId()
        );

        if (!similarDocs.isEmpty()) {
            contextBuilder.append("【相关文档内容】\n");
            similarDocs.forEach(doc -> contextBuilder.append(doc.getContent()).append("\n---\n"));
        } else if (request.getDocumentId() != null) {
            var docOpt = documentRepository.findById(request.getDocumentId());
            if (docOpt.isPresent()) {
                var doc = docOpt.get();
                contextBuilder.append("【文档信息】\n");
                contextBuilder.append(String.format("标题: %s\n", doc.getTitle()));
                contextBuilder.append(String.format("描述: %s\n", doc.getDescription()));
                contextBuilder.append(String.format("分类: %s\n", doc.getCategory() != null ? doc.getCategory().getName() : "未分类"));
                String tagsStr = doc.getTags() != null && !doc.getTags().isEmpty()
                    ? doc.getTags().stream().map(tag -> tag.getName()).collect(Collectors.joining(", "))
                    : "无";
                contextBuilder.append(String.format("标签: %s\n", tagsStr));
                contextBuilder.append(String.format("下载次数: %d\n", doc.getDownloadCount()));
                contextBuilder.append(String.format("上传者: %s\n", doc.getUploader() != null ? doc.getUploader().getUsername() : "未知"));
            }
        } else {
            var popularDocs = documentService.getPopularDocuments(0, 5);
            if (popularDocs.getTotal() > 0) {
                contextBuilder.append("【知享平台热门文档】\n");
                popularDocs.getList().forEach(doc -> {
                    String categoryName = doc.getCategory() != null ? doc.getCategory().getName() : "未分类";
                    String desc = doc.getDescription();
                    String shortDesc = desc != null && desc.length() > 50 ? desc.substring(0, 50) + "..." : desc;
                    contextBuilder.append(String.format("- %s (%s) - %s\n", doc.getTitle(), categoryName, shortDesc));
                });
                contextBuilder.append("\n");
            }

            var latestDocs = documentService.getLatestDocuments(0, 5);
            if (latestDocs.getTotal() > 0) {
                contextBuilder.append("【最新上传文档】\n");
                latestDocs.getList().forEach(doc -> {
                    String categoryName = doc.getCategory() != null ? doc.getCategory().getName() : "未分类";
                    contextBuilder.append(String.format("- %s (%s)\n", doc.getTitle(), categoryName));
                });
            }
        }

        String context = contextBuilder.toString();
        String currentQuestion = request.getMessage();

        if (!context.isEmpty()) {
            promptBuilder.append(String.format("""
                    你是知享平台的AI助手，请基于以下平台信息回答用户问题。

                    %s

                    用户问题: %s

                    请结合平台信息给出专业、有帮助的回答。如果用户询问文档推荐，优先推荐上述文档。
                    """, context, currentQuestion));
        } else {
            promptBuilder.append(currentQuestion);
        }

        return promptBuilder.toString();
    }

    private Long getOrCreateConversation(ChatRequest request, Long userId) {
        // 如果用户未登录，不保存对话
        if (userId == null) {
            return null;
        }
        // 如果已有对话ID，直接返回
        if (request.getConversationId() != null) {
            return request.getConversationId();
        }
        // 没有对话ID时，创建新对话
        ChatConversation conversation = doCreateNewConversation(userId, request.getMessage());
        return conversation.getId();
    }

    private ChatConversation doCreateNewConversation(Long userId, String firstMessage) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(generateConversationTitle(firstMessage));
        try {
            // 初始化为空数组
            conversation.setMessages(objectMapper.writeValueAsString(new ArrayList<MessageItem>()));
        } catch (Exception e) {
            conversation.setMessages("[]");
        }
        conversation.setMessageCount(0);
        return chatConversationRepository.save(conversation);
    }

    private void saveMessage(Long conversationId, String role, String content) {
        if (conversationId == null) {
            return;
        }

        try {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("对话不存在"));

            // 解析现有消息
            List<MessageItem> messages;
            if (conversation.getMessages() == null || conversation.getMessages().isEmpty()) {
                messages = new ArrayList<>();
            } else {
                messages = objectMapper.readValue(conversation.getMessages(), new TypeReference<List<MessageItem>>() {});
            }

            // 添加新消息
            messages.add(new MessageItem(role, content));

            // 更新对话
            conversation.setMessages(objectMapper.writeValueAsString(messages));
            conversation.setMessageCount(messages.size() / 2); // 一轮对话包含user和assistant两条消息
            chatConversationRepository.save(conversation);
        } catch (Exception e) {
            log.error("保存消息失败", e);
        }
    }

    private String generateConversationTitle(String firstMessage) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format("""
                请为以下用户问题生成一个简短的标题（10字以内），只返回标题文字，不要引号或其他符号。

                用户问题：%s
                """, firstMessage.substring(0, Math.min(firstMessage.length(), 100)));

            String title = chatClient.prompt().user(prompt).call().content();
            if (title == null || title.trim().isEmpty()) {
                return firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
            }
            title = title.trim();
            return title.length() > 30 ? title.substring(0, 30) : title;
        } catch (Exception e) {
            log.error("Error generating conversation title", e);
            return firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
        }
    }

    public Page<ChatConversationResponse> getUserConversations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(conv -> {
                    ChatConversationResponse resp = new ChatConversationResponse();
                    resp.setId(conv.getId());
                    resp.setTitle(conv.getTitle());
                    resp.setMessageCount(conv.getMessageCount());
                    resp.setCreatedAt(conv.getCreatedAt());
                    resp.setUpdatedAt(conv.getUpdatedAt());
                    return resp;
                });
    }

    public List<MessageItem> getConversationMessages(Long conversationId, Long userId) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此对话");
        }
        try {
            if (conversation.getMessages() == null || conversation.getMessages().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(conversation.getMessages(), new TypeReference<List<MessageItem>>() {});
        } catch (Exception e) {
            log.error("解析消息失败", e);
            return new ArrayList<>();
        }
    }

    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此对话");
        }
        chatConversationRepository.delete(conversation);
    }

    /**
     * 创建新对话并返回ID
     */
    public Long createNewConversation(Long userId, String firstMessage) {
        ChatConversation conversation = doCreateNewConversation(userId, firstMessage);
        return conversation.getId();
    }

    /**
     * Generate summary for document content
     */
    public String generateSummary(String content) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format("""
                请为以下文档内容生成一个简明扼要的摘要（200字以内），重点概括核心观点和结论。

                文档内容片段：
                %s
                """, content.substring(0, Math.min(content.length(), 2000))); // Limit input size

            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("Error generating summary", e);
            return "摘要生成失败";
        }
    }

    /**
     * Generate tags for document content
     */
    public List<String> generateTags(String content) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String prompt = String.format("""
                请提取以下文档的3-5个核心关键词（标签），直接以逗号分隔返回，不要包含其他文字。
                例如：Java,Spring Boot,微服务

                文档内容片段：
                %s
                """, content.substring(0, Math.min(content.length(), 2000)));

            String response = chatClient.prompt().user(prompt).call().content();
            if (response == null || response.trim().isEmpty()) {
                return List.of();
            }
            response = response.trim();
            return List.of(response.replace("，", ",").split(","));
        } catch (Exception e) {
            log.error("Error generating tags", e);
            return List.of();
        }
    }
}
