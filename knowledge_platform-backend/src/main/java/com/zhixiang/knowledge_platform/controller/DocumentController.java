package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.request.DocumentSearchRequest;
import com.zhixiang.knowledge_platform.dto.request.DocumentUploadRequest;
import com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.UploadDocumentResponse;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.DocumentService;
import com.zhixiang.knowledge_platform.service.DocumentInitialReviewService;
import com.zhixiang.knowledge_platform.service.FileTypeRegistryService;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import com.zhixiang.knowledge_platform.service.UserService;
import com.zhixiang.knowledge_platform.service.ai.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档管理控制器
 *
 * @author leishuai
 * @version 0.0.1-SNAPSHOT
 * @since 2025-09-10
 */
@Tag(name = "文档管理", description = "文档上传、下载、搜索等功能")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final FileUploadService fileUploadService;
    private final DocumentInitialReviewService initialReviewService;
    private final ObjectProvider<DocumentIngestionService> documentIngestionServiceProvider;
    private final UserService userService;
    private final FileTypeRegistryService fileTypeRegistryService;

    @Operation(summary = "上传文档", description = "用户上传文档文件和信息")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<UploadDocumentResponse> uploadDocument(
            @Parameter(description = "文档文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档标题", required = true)
            @RequestParam("title") String title,
            @Parameter(description = "文档描述")
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "分类ID", required = true)
            @RequestParam("categoryId") Long categoryId,
            @Parameter(description = "下载积分", required = false)
            @RequestParam(value = "downloadPoints", required = false, defaultValue = "0") Integer downloadPoints,
            @Parameter(description = "标签列表，逗号分隔")
            @RequestParam(value = "tags", required = false) String tags,
            HttpServletRequest request) {

        FileUploadService.FileUploadResult uploadResult = null;

        try {
            log.info("开始处理文档上传请求 - 文件名: {}, 标题: {}, 分类ID: {}, 积分: {}",
                    file.getOriginalFilename(), title, categoryId, downloadPoints);

            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            log.info("当前用户ID: {}", userId);
            if (userId == null) {
                log.warn("用户身份验证失败");
                return uploadError("用户身份验证失败");
            }

            // 验证必需参数
            if (file.isEmpty()) {
                log.warn("上传文件为空");
                return uploadError("请选择要上传的文件");
            }

            if (title == null || title.trim().isEmpty()) {
                log.warn("文档标题为空");
                return uploadError("请输入文档标题");
            }

            if (categoryId == null || categoryId <= 0) {
                log.warn("分类ID无效: {}", categoryId);
                return uploadError("请选择有效的文档分类");
            }

            // 先验证并获取文件信息
            String fileExtension = getFileExtension(file.getOriginalFilename());
            log.info("文件扩展名: {}", fileExtension);
            if (fileExtension == null || fileExtension.isEmpty()) {
                log.warn("无法识别文件类型: {}", file.getOriginalFilename());
                return uploadError("无法识别文件类型");
            }

            // 创建上传请求对象
            DocumentUploadRequest uploadRequest = new DocumentUploadRequest();
            uploadRequest.setTitle(title);
            uploadRequest.setDescription(description);
            uploadRequest.setCategoryId(categoryId);
            uploadRequest.setDownloadPoints(downloadPoints);
            log.info("上传请求对象创建成功: {}", uploadRequest);

            // 使用UUID生成临时ID，避免时间戳冲突
            String tempDocumentId = UUID.randomUUID().toString();
            log.info("开始上传文件，临时文档ID: {}", tempDocumentId);
            uploadResult = fileUploadService.uploadDocument(file, tempDocumentId);
            log.info("文件上传完成: {}", uploadResult.getRelativePath());

            // 创建完整的文档记录（包含文件信息）
            log.info("开始创建文档记录");
            Document document = documentService.createDocumentWithFileInfo(
                uploadRequest,
                userId,
                uploadResult.getOriginalFileName(),
                uploadResult.getRelativePath(),
                uploadResult.getFileSize(),
                uploadResult.getFileType(),
                uploadResult.getFileExtension(),
                uploadResult.getMd5()
            );
            log.info("文档记录创建成功，ID: {}", document.getId());

            DocumentReview review = null;
            log.info("开始执行文档初审，文档ID: {}", document.getId());
            review = initialReviewService.performInitialReview(document);
            log.info("文档初审完成，结果: {}", review.getStatus());

            // 处理标签
            if (tags != null && !tags.trim().isEmpty()) {
                log.info("开始处理标签: {}", tags);
                List<String> tagList = Arrays.asList(tags.split(","));
                tagList.replaceAll(String::trim);
                documentService.addTagsToDocument(document.getId(), tagList);
                log.info("标签处理完成");
            }

            // 触发 AI 处理流程 (异步)
            try {
                DocumentIngestionService documentIngestionService = documentIngestionServiceProvider.getIfAvailable();
                if (documentIngestionService != null) {
                    Path filePath = fileUploadService.getFilePath(document.getFilePath());
                    documentIngestionService.ingestDocument(filePath.toFile(), document.getId(), document.getTitle());
                    log.info("AI 文档处理任务已提交");
                } else {
                    log.info("AI 功能未启用，跳过文档 AI 处理");
                }
            } catch (Exception e) {
                log.error("触发 AI 处理失败", e);
                // 不影响主流程
            }

            UploadDocumentResponse response = buildUploadDocumentResponse(document, review);
            log.info("文档上传成功，文档ID: {}", document.getId());
            return ApiResponse.success(response, buildUploadSuccessMessage(review));

        } catch (IOException e) {
            log.error("文件上传IO异常", e);
            // 清理已上传的文件
            if (uploadResult != null) {
                try {
                    fileUploadService.deleteFile(uploadResult.getRelativePath());
                    log.info("已清理上传失败的文件: {}", uploadResult.getRelativePath());
                } catch (Exception cleanupEx) {
                    log.error("清理上传文件失败", cleanupEx);
                }
            }
            return uploadError("文件上传失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("参数验证异常", e);
            // 清理已上传的文件
            if (uploadResult != null) {
                try {
                    fileUploadService.deleteFile(uploadResult.getRelativePath());
                    log.info("已清理上传失败的文件: {}", uploadResult.getRelativePath());
                } catch (Exception cleanupEx) {
                    log.error("清理上传文件失败", cleanupEx);
                }
            }
            return uploadError("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档上传未知异常", e);
            // 清理已上传的文件
            if (uploadResult != null) {
                try {
                    fileUploadService.deleteFile(uploadResult.getRelativePath());
                    log.info("已清理上传失败的文件: {}", uploadResult.getRelativePath());
                } catch (Exception cleanupEx) {
                    log.error("清理上传文件失败", cleanupEx);
                }
            }
            return uploadError("文档上传失败: " + e.getMessage());
        }
    }

    private ApiResponse<UploadDocumentResponse> uploadError(String message) {
        return ApiResponse.error(message);
    }

    private UploadDocumentResponse buildUploadDocumentResponse(Document document, DocumentReview review) {
        UploadDocumentResponse response = new UploadDocumentResponse();
        response.setDocument(documentService.getDocumentInfo(document.getId()));
        response.setInitialReviewOutcome(resolveInitialReviewOutcome(review));
        response.setRejectionReason(review != null ? review.getRejectionReason() : null);
        response.setSuggestions(review != null ? review.getSuggestions() : null);
        response.setShouldRedirectToMyUploads(true);
        response.setCanRetryUpload(false);
        return response;
    }

    private String buildUploadSuccessMessage(DocumentReview review) {
        UploadDocumentResponse.InitialReviewOutcome outcome = resolveInitialReviewOutcome(review);
        return switch (outcome) {
            case PASSED -> "文档已上传并通过初审，等待管理员复审";
            case REJECTED -> "文档已上传，但初审未通过，请到我的上传查看原因";
            case SYSTEM_REJECTED -> "文档已上传，但初审暂未完成，请到我的上传查看状态，不要重复上传";
        };
    }

    private UploadDocumentResponse.InitialReviewOutcome resolveInitialReviewOutcome(DocumentReview review) {
        if (review == null) {
            return UploadDocumentResponse.InitialReviewOutcome.SYSTEM_REJECTED;
        }
        if (review.isPassed()) {
            return UploadDocumentResponse.InitialReviewOutcome.PASSED;
        }
        if (review.getReviewComment() != null && review.getReviewComment().contains("初审执行异常")) {
            return UploadDocumentResponse.InitialReviewOutcome.SYSTEM_REJECTED;
        }
        return UploadDocumentResponse.InitialReviewOutcome.REJECTED;
    }

    @Operation(summary = "搜索文档", description = "根据关键词和条件搜索文档")
    @GetMapping("/search")
    public ApiResponse<PageResponse<DocumentInfoResponse>> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // 构建搜索请求对象
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setKeyword(keyword);
            request.setFileType(fileType);
            request.setCategoryId(categoryId);
            request.setTagIds(tagIds);
            request.setMinRating(minRating);
            request.setSortBy(sortBy);
            request.setSortDirection(sortOrder);
            request.setPage(Math.max(1, page) - 1); // 前端页码从1开始，Spring从0开始
            request.setSize(size);

            // 调试日志
            log.info("搜索参数 - keyword: {}, fileType: {}, categoryId: {}, tagIds: {}, minRating: {}, sortBy: {}, sortOrder: {}, page: {}, size: {}",
                     keyword, fileType, categoryId, tagIds, minRating, sortBy, sortOrder, page, size);

            PageResponse<DocumentInfoResponse> response = documentService.searchDocuments(request);
            log.info("搜索结果数量: {}", response.getTotal());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("搜索文档失败", e);
            return ApiResponse.error("搜索文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文档详情", description = "根据ID获取文档详细信息")
    @GetMapping("/{id}")
    public ApiResponse<DocumentInfoResponse> getDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Document document = documentService.getDocumentById(id);
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);

            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ApiResponse.error("文档不存在或无权访问");
            }

            DocumentInfoResponse response = documentService.getDocumentInfo(id);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取文档详情失败, ID: {}", id, e);
            return ApiResponse.error("获取文档详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取已审核文档列表", description = "分页获取所有已审核通过的文档")
    @GetMapping("/approved")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getApprovedDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getApprovedDocuments(Math.max(1, page) - 1, size, sortBy);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取已审核文档列表失败", e);
            return ApiResponse.error("获取文档列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据分类获取文档", description = "分页获取指定分类的已审核文档")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getDocumentsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getDocumentsByCategory(categoryId, Math.max(1, page) - 1, size, sortBy);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("根据分类获取文档失败, 分类ID: {}", categoryId, e);
            return ApiResponse.error("获取文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据标签获取文档", description = "分页获取包含指定标签的文档")
    @GetMapping("/tags")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getDocumentsByTags(
            @RequestParam List<Long> tagIds,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortBy) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getDocumentsByTags(tagIds, Math.max(1, page) - 1, size, sortBy);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("根据标签获取文档失败, 标签IDs: {}", tagIds, e);
            return ApiResponse.error("获取文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取热门文档", description = "按下载量排序获取热门文档")
    @GetMapping("/popular")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getPopularDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getPopularDocuments(Math.max(1, page) - 1, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取热门文档失败", e);
            return ApiResponse.error("获取热门文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取最新文档", description = "按创建时间排序获取最新文档")
    @GetMapping("/latest")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getLatestDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getLatestDocuments(Math.max(1, page) - 1, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取最新文档失败", e);
            return ApiResponse.error("获取最新文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取高评分文档", description = "按评分排序获取高评分文档")
    @GetMapping("/top-rated")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getTopRatedDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getTopRatedDocuments(Math.max(1, page) - 1, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取高评分文档失败", e);
            return ApiResponse.error("获取高评分文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户上传的文档", description = "获取当前用户上传的所有文档")
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getUserDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("用户身份验证失败");
            }
            PageResponse<DocumentInfoResponse> response = documentService.getUserDocuments(userId, Math.max(1, page) - 1, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户文档失败", e);
            return ApiResponse.error("获取用户文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "下载文档", description = "下载指定ID的文档文件")
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }

            Document document = documentService.getDocumentById(id);
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.status(403).build();
            }

            boolean isApprovedDocument = document.getStatus() == DocumentStatus.APPROVED;
            boolean isOwner = document.getUploaderId() != null && document.getUploaderId().equals(userId);
            boolean isAdmin = userRole != null && userRole.equalsIgnoreCase("ADMIN");

            if (isApprovedDocument) {
                boolean downloadSuccess = documentService.recordDownload(id, userId);
                if (!downloadSuccess) {
                    return ResponseEntity.status(402).build();
                }
            } else if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).build();
            }

            Path filePath = fileUploadService.getFilePath(document.getFilePath());
            if (!fileUploadService.fileExists(document.getFilePath())) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // 使用RFC 5987标准编码文件名，支持非ASCII字符
            String fileName = document.getFileName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CONTENT_TYPE, document.getFileType())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(document.getFileSize()))
                    .body(resource);

        } catch (Exception e) {
            log.error("文档下载失败, ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "删除文档", description = "删除用户上传的文档")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("用户身份验证失败");
            }
            documentService.deleteDocument(id, userId);
            return ApiResponse.success(null, "文档删除成功");
        } catch (Exception e) {
            log.error("删除文档失败, ID: {}", id, e);
            return ApiResponse.error("删除文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取待审核文档", description = "管理员获取所有待审核的文档")
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<DocumentInfoResponse>> getPendingDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResponse<DocumentInfoResponse> response = documentService.getPendingDocuments(Math.max(1, page) - 1, size);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取待审核文档失败", e);
            return ApiResponse.error("获取待审核文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：审核通过文档", description = "管理员审核通过指定文档")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approveDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (adminId == null) {
                return ApiResponse.error("管理员身份验证失败");
            }
            documentService.approveDocument(id, adminId);
            return ApiResponse.success(null, "文档审核通过");
        } catch (Exception e) {
            log.error("文档审核失败, ID: {}", id, e);
            return ApiResponse.error("文档审核失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：拒绝文档", description = "管理员拒绝指定文档")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectDocument(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            documentService.rejectDocument(id, reason);
            return ApiResponse.success(null, "文档审核拒绝");
        } catch (Exception e) {
            log.error("文档审核拒绝失败, ID: {}", id, e);
            return ApiResponse.error("文档审核拒绝失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：删除文档", description = "管理员删除任意文档")
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> adminDeleteDocument(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId(request);
            if (adminId == null) {
                return ApiResponse.error("管理员身份验证失败");
            }
            documentService.deleteDocument(id, adminId);
            return ApiResponse.success(null, "文档删除成功");
        } catch (Exception e) {
            log.error("管理员删除文档失败, ID: {}", id, e);
            return ApiResponse.error("删除文档失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取平台统计数据", description = "获取平台统计数据（公开接口）")
    @GetMapping("/stats")
    public ApiResponse<Object> getPlatformStats() {
        try {
            Map<String, Object> stats = documentService.getPlatformStats();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取平台统计数据失败", e);
            return ApiResponse.error("获取平台统计数据失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取支持的文件类型", description = "获取系统支持的文件类型列表")
    @GetMapping("/supported-types")
    public ApiResponse<Object> getSupportedFileTypes() {
        try {
            return ApiResponse.success(Map.of(
                "fileTypes", fileUploadService.getSupportedFileTypes(),
                "maxFileSize", fileUploadService.getMaxFileSize(),
                "maxFileSizeReadable", formatFileSize(fileUploadService.getMaxFileSize())
            ));
        } catch (Exception e) {
            log.error("获取支持的文件类型失败", e);
            return ApiResponse.error("获取支持的文件类型失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文件类型能力矩阵", description = "获取平台支持的文件类型、能力矩阵与搜索分组")
    @GetMapping("/file-types/capabilities")
    public ApiResponse<Object> getFileTypeCapabilities() {
        try {
            return ApiResponse.success(fileTypeRegistryService.buildCapabilityResponse(fileUploadService.getMaxFileSize()));
        } catch (Exception e) {
            log.error("获取文件类型能力矩阵失败", e);
            return ApiResponse.error("获取文件类型能力矩阵失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文档统计信息", description = "获取文档相关的统计数据")
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Object> getDocumentStatistics() {
        try {
            return ApiResponse.success(Map.of(
                "statusStats", documentService.getDocumentStatsByStatus(),
                "categoryStats", documentService.getDocumentStatsByCategory(),
                "totalFileSize", documentService.getTotalFileSize(),
                "totalFileSizeReadable", formatFileSize(documentService.getTotalFileSize())
            ));
        } catch (Exception e) {
            log.error("获取文档统计信息失败", e);
            return ApiResponse.error("获取文档统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }
}