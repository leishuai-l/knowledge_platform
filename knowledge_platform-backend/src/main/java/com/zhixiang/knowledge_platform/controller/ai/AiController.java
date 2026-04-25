package com.zhixiang.knowledge_platform.controller.ai;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.ai.ChatRequest;
import com.zhixiang.knowledge_platform.dto.ai.ChatResponse;
import com.zhixiang.knowledge_platform.service.FileValidationService;
import com.zhixiang.knowledge_platform.service.FileTypeRegistryService;
import com.zhixiang.knowledge_platform.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

import org.springframework.http.MediaType;

import com.zhixiang.knowledge_platform.dto.ai.MessageItem;
import com.zhixiang.knowledge_platform.dto.response.ChatConversationResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(name = "zhixiang.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Tag(name = "AI 智能服务", description = "提供智能问答、推荐与数据导出功能")
public class AiController {

    private final AiService aiService;
    private final FileValidationService fileValidationService;
    private final FileTypeRegistryService fileTypeRegistryService;

    @PostMapping(value = "/generate-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 生成文档信息 (摘要和标签)")
    public ApiResponse<Map<String, Object>> generateDocumentInfo(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return generateInfoInternal(file, request, true, true);
    }

    @PostMapping(value = "/generate-summary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 仅生成文档摘要")
    public ApiResponse<Map<String, Object>> generateDocumentSummary(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return generateInfoInternal(file, request, true, false);
    }

    @PostMapping(value = "/generate-tags", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 仅生成文档标签")
    public ApiResponse<Map<String, Object>> generateDocumentTags(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return generateInfoInternal(file, request, false, true);
    }

    private ApiResponse<Map<String, Object>> generateInfoInternal(
            MultipartFile file,
            HttpServletRequest request,
            boolean generateSummary,
            boolean generateTags) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }

        ApiResponse<Map<String, Object>> validationError = validateAiFile(file);
        if (validationError != null) {
            return validationError;
        }

        try {
            Resource resource = new InputStreamResource(file.getInputStream());
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> documents = reader.get();

            if (documents.isEmpty()) {
                return ApiResponse.error("未能从文件中提取有效文本，扫描件、图片或压缩包暂不支持 AI 一键填写");
            }

            String fullContent = documents.stream()
                .map(org.springframework.ai.document.Document::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"))
                .trim();

            if (!StringUtils.hasText(fullContent)) {
                return ApiResponse.error("未能从文件中提取有效文本，扫描件、图片或压缩包暂不支持 AI 一键填写");
            }

            String summary = null;
            List<String> tags = null;

            if (generateSummary) {
                summary = aiService.generateSummary(fullContent);
            }
            if (generateTags) {
                tags = aiService.generateTags(fullContent);
            }

            return ApiResponse.success(Map.of(
                "summary", summary != null ? summary : "",
                "tags", tags != null ? tags : List.of()
            ));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("AI 生成文档信息失败: {}", file.getOriginalFilename(), e);
            return ApiResponse.error("AI 服务暂时不可用，请稍后重试");
        }
    }

    private ApiResponse<Map<String, Object>> validateAiFile(MultipartFile file) {
        FileValidationService.FileValidationResult validationResult = fileValidationService.validateFile(file);
        if (!validationResult.isValid()) {
            return ApiResponse.error(validationResult.getErrorMessage());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!fileTypeRegistryService.isAiSupportedExtension(extension)) {
            return ApiResponse.error("该文件类型暂不支持 AI 一键填写，请手动填写摘要和标签");
        }

        return null;
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
            // If user is not logged in, we might want to allow anonymous chat or throw error
            // For now, let's assume userId can be null for anonymous or handled by service
            return ApiResponse.success(aiService.chat(request, userId));
        } catch (Exception e) {
            log.error("AI聊天服务异常", e);
            return ApiResponse.error("AI服务暂时不可用，请稍后重试");
        }
    }

    @PostMapping(value = "/stream-chat", produces = "text/event-stream")
    @Operation(summary = "智能问答 (流式响应)")
    public SseEmitter streamChat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(httpRequest);
        return aiService.streamChat(request, userId);
    }
    
    @GetMapping("/conversations")
    @Operation(summary = "获取用户对话列表")
    public ApiResponse<PageResponse<ChatConversationResponse>> getUserConversations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        Page<ChatConversationResponse> conversations = aiService.getUserConversations(userId, page, size);
        return ApiResponse.success(PageResponse.fromPage(conversations, conversations.getContent()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取对话消息列表")
    public ApiResponse<List<MessageItem>> getConversationMessages(
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        return ApiResponse.success(aiService.getConversationMessages(conversationId, userId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除对话")
    public ApiResponse<Void> deleteConversation(@PathVariable Long conversationId, HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        aiService.deleteConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/recommend")
    @Operation(summary = "获取AI推荐文档")
    public ApiResponse<List<DocumentInfoResponse>> getRecommendedDocuments(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        return ApiResponse.success(aiService.getRecommendedDocuments(userId));
    }
}
