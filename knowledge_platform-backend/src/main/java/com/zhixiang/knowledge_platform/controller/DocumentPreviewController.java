package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.DocumentPreviewService;
import com.zhixiang.knowledge_platform.service.DocumentService;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档预览控制器
 * 提供文档预览相关的API接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档预览", description = "文档预览相关接口")
@SecurityRequirement(name = "bearerAuth")
public class DocumentPreviewController {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreviewController.class);

    private final DocumentPreviewService documentPreviewService;
    private final DocumentService documentService;
    private final FileUploadService fileUploadService;

    public DocumentPreviewController(DocumentPreviewService documentPreviewService,
                                   DocumentService documentService,
                                   FileUploadService fileUploadService) {
        this.documentPreviewService = documentPreviewService;
        this.documentService = documentService;
        this.fileUploadService = fileUploadService;
    }

    /**
     * 获取文档预览信息
     */
    @GetMapping("/{id}/preview-info")
    @Operation(summary = "获取文档预览信息")
    public ResponseEntity<ApiResponse<DocumentPreviewService.DocumentPreviewInfo>> getPreviewInfo(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            log.info("获取文档预览信息请求: documentId={}, userId={}", id, userId);

            // 获取文档信息
            Document document;
            try {
                document = documentService.getDocumentById(id);
            } catch (RuntimeException e) {
                return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.DocumentPreviewInfo>error("文档不存在"));
            }

            // 检查文档访问权限 - 公开文档允许游客访问
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.DocumentPreviewInfo>error("无权限访问该文档"));
            }

            // 获取预览信息 - 游客也可以查看预览信息
            DocumentPreviewService.DocumentPreviewInfo previewInfo =
                documentPreviewService.getPreviewInfo(document.getFilePath(), id, userId, userRole);

            log.info("文档预览信息获取成功: documentId={}, previewable={}", id, previewInfo.isPreviewable());
            return ResponseEntity.ok(ApiResponse.success(previewInfo));

        } catch (Exception e) {
            log.error("获取文档预览信息失败: documentId={}", id, e);
            return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.DocumentPreviewInfo>error("获取预览信息失败: " + e.getMessage()));
        }
    }

    /**
     * 预览文档内容（用于图片和文本文件）
     */
    @GetMapping("/{id}/preview-content")
    @Operation(summary = "预览文档内容")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> previewDocument(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);

            log.info("预览文档内容请求: documentId={}, userId={}", id, userId);

            // 获取文档信息
            Document document;
            try {
                document = documentService.getDocumentById(id);
            } catch (RuntimeException e) {
                return ResponseEntity.notFound().build();
            }

            // 检查文档访问权限
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                log.warn("用户无权访问文档: documentId={}, userId={}", id, userId);
                return ResponseEntity.status(403).build();
            }

            DocumentPreviewService.PreviewContent previewContent =
                documentPreviewService.resolvePreviewContent(document.getFilePath(), document.getFileName());
            if (previewContent == null || !Files.exists(previewContent.path())) {
                log.warn("预览文件不存在或不可生成: documentId={}", id);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(previewContent.path());
            String contentType = resolvePreviewContentType(previewContent.path(), previewContent.extension());

            log.info("文档预览成功: documentId={}, contentType={}", id, contentType);

            String encodedFileName;
            try {
                encodedFileName = URLEncoder.encode(previewContent.filename(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            } catch (Exception e) {
                encodedFileName = "document";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .body(resource);

        } catch (Exception e) {
            log.error("预览文档失败: documentId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文档缩略图
     */
    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "获取文档缩略图")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> getDocumentThumbnail(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            log.info("获取文档缩略图请求: documentId={}, userId={}", id, userId);

            // 获取文档信息
            Document document;
            try {
                document = documentService.getDocumentById(id);
            } catch (RuntimeException e) {
                return ResponseEntity.notFound().build();
            }

            // 检查文档访问权限
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.badRequest().build();
            }

            // 生成缩略图
            String thumbnailPath = documentPreviewService.generateThumbnail(document.getFilePath());

            if (thumbnailPath == null) {
                // 如果无法生成缩略图，返回默认图标
                return getDefaultThumbnail(document.getFileName());
            }

            Path thumbnail = Paths.get(thumbnailPath);
            if (!Files.exists(thumbnail)) {
                return getDefaultThumbnail(document.getFileName());
            }

            Resource resource = new FileSystemResource(thumbnail);

            log.info("文档缩略图获取成功: documentId={}", id);

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''thumbnail.jpg")
                .body(resource);

        } catch (Exception e) {
            log.error("获取文档缩略图失败: documentId={}", id, e);
            return getDefaultThumbnail("unknown");
        }
    }

    /**
     * 检查文档是否支持预览
     */
    @GetMapping("/{id}/previewable")
    @Operation(summary = "检查文档是否支持预览")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> isDocumentPreviewable(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {

        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);

            // 获取文档信息
            Document document;
            try {
                document = documentService.getDocumentById(id);
            } catch (RuntimeException e) {
                return ResponseEntity.ok(ApiResponse.<Boolean>error("文档不存在"));
            }

            // 检查文档访问权限
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.ok(ApiResponse.<Boolean>error("无权限访问该文档"));
            }

            boolean previewable = documentPreviewService.isPreviewable(document.getFileName());

            return ResponseEntity.ok(ApiResponse.success(previewable));

        } catch (Exception e) {
            log.error("检查文档预览支持失败: documentId={}", id, e);
            return ResponseEntity.ok(ApiResponse.<Boolean>error("检查预览支持失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文本预览内容
     */
    @GetMapping("/{id}/preview-text")
    @Operation(summary = "获取文本预览内容")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentPreviewService.TextPreviewPayload>> getTextPreview(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            Document document = documentService.getDocumentById(id);
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.TextPreviewPayload>error("无权限访问该文档"));
            }
            DocumentPreviewService.TextPreviewPayload payload = documentPreviewService.getTextPreview(document.getFilePath(), document.getFileName());
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("获取文本预览内容失败: documentId={}", id, e);
            return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.TextPreviewPayload>error("获取文本预览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取压缩包预览内容
     */
    @GetMapping("/{id}/preview-archive")
    @Operation(summary = "获取压缩包预览内容")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentPreviewService.ArchivePreviewPayload>> getArchivePreview(
            @Parameter(description = "文档ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            Document document = documentService.getDocumentById(id);
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.ArchivePreviewPayload>error("无权限访问该文档"));
            }
            DocumentPreviewService.ArchivePreviewPayload payload = documentPreviewService.getArchivePreview(document.getFilePath(), document.getFileName());
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("获取压缩包预览内容失败: documentId={}", id, e);
            return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.ArchivePreviewPayload>error("获取压缩包预览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取压缩包内文件预览信息
     */
    @GetMapping("/{id}/preview-archive-entry-info")
    @Operation(summary = "获取压缩包内文件预览信息")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentPreviewService.ArchiveEntryPreviewInfo>> getArchiveEntryPreviewInfo(
            @Parameter(description = "文档ID") @PathVariable Long id,
            @RequestParam String path,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            Document document = documentService.getDocumentById(id);
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.ArchiveEntryPreviewInfo>error("无权限访问该文档"));
            }
            DocumentPreviewService.ArchiveEntryPreviewInfo payload = documentPreviewService.getArchiveEntryPreviewInfo(document.getFilePath(), document.getFileName(), path);
            return ResponseEntity.ok(ApiResponse.success(payload));
        } catch (Exception e) {
            log.error("获取压缩包内文件预览信息失败: documentId={}, path={}", id, path, e);
            return ResponseEntity.ok(ApiResponse.<DocumentPreviewService.ArchiveEntryPreviewInfo>error("获取压缩包内文件预览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取压缩包内文件预览内容
     */
    @GetMapping("/{id}/preview-archive-entry-content")
    @Operation(summary = "获取压缩包内文件预览内容")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> getArchiveEntryPreviewContent(
            @Parameter(description = "文档ID") @PathVariable Long id,
            @RequestParam String path,
            HttpServletRequest request) {
        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            String userRole = JwtAuthenticationFilter.getCurrentUserRole(request);
            Document document = documentService.getDocumentById(id);
            if (!documentService.canUserAccessDocument(document, userId, userRole)) {
                return ResponseEntity.status(403).build();
            }
            DocumentPreviewService.ArchiveEntryContent content = documentPreviewService.getArchiveEntryContent(document.getFilePath(), document.getFileName(), path);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + URLEncoder.encode(content.filename(), StandardCharsets.UTF_8).replaceAll("\\+", "%20"))
                .body(new org.springframework.core.io.ByteArrayResource(content.bytes()));
        } catch (Exception e) {
            log.error("获取压缩包内文件预览内容失败: documentId={}, path={}", id, path, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 管理员清理预览缓存
     */
    @DeleteMapping("/preview-cache/cleanup")
    @Operation(summary = "清理预览缓存")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cleanupPreviewCache() {
        try {
            documentPreviewService.cleanupPreviewCache();
            log.info("预览缓存清理完成");
            return ResponseEntity.ok(ApiResponse.<Void>success());
        } catch (Exception e) {
            log.error("清理预览缓存失败", e);
            return ResponseEntity.ok(ApiResponse.<Void>error("清理预览缓存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取默认缩略图
     */
    private ResponseEntity<Resource> getDefaultThumbnail(String filename) {
        try {
            String extension = getFileExtension(filename).toLowerCase();
            Resource resource = new ClassPathResource("static/icons/file-" + extension + ".png");
            if (!resource.exists()) {
                resource = new ClassPathResource("static/icons/file-default.png");
            }

            if (resource.exists()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
            }
        } catch (Exception e) {
            log.warn("获取默认缩略图失败", e);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 解析预览内容类型
     */
    private String resolvePreviewContentType(Path filePath, String extension) {
        if (isTextFile(extension)) {
            return "text/plain; charset=utf-8";
        }

        if ("pdf".equals(extension)) {
            return MediaType.APPLICATION_PDF_VALUE;
        }

        String imageContentType = resolveImageContentType(extension);
        if (imageContentType != null) {
            return imageContentType;
        }

        try {
            String probedType = Files.probeContentType(filePath);
            if (probedType != null && !probedType.isBlank() && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(probedType)) {
                return probedType;
            }
        } catch (Exception e) {
            log.warn("探测文件Content-Type失败: path={}", filePath, e);
        }

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String resolveImageContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> null;
        };
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 检查是否为文本文件
     */
    private boolean isTextFile(String extension) {
        return java.util.Set.of("txt", "md", "json", "xml", "csv", "log",
                "java", "js", "html", "css", "py", "sql").contains(extension);
    }
}