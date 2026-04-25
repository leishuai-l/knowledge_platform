package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import com.zhixiang.knowledge_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * 文件管理控制器
 */
@Tag(name = "文件管理", description = "文件上传相关功能")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUploadService fileUploadService;
    private final UserService userService;

    @Operation(summary = "上传用户头像", description = "用户上传头像图片")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Map<String, Object>> uploadAvatar(
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);

        try {
            FileUploadService.FileUploadResult uploadResult = fileUploadService.uploadAvatar(file, userId);
            String avatarUrl = uploadResult.getRelativePath();
            userService.updateUserAvatar(userId, avatarUrl);

            return ApiResponse.success(Map.of(
                    "avatarUrl", avatarUrl,
                    "fileSize", uploadResult.getFileSize(),
                    "fileName", uploadResult.getStoredFileName()
            ), "头像上传成功");
        } catch (IOException e) {
            log.error("头像上传失败，用户ID: {}", userId, e);
            return ApiResponse.error("头像上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("头像上传失败，用户ID: {}", userId, e);
            return ApiResponse.error("头像上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取用户头像", description = "根据用户ID获取头像图片")
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<Resource> getUserAvatar(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        try {
            String[] extensions = {"jpg", "jpeg", "png", "gif"};
            String avatarPath = null;

            for (String ext : extensions) {
                String testPath = "avatars/user_" + userId + "." + ext;
                if (fileUploadService.fileExists(testPath)) {
                    avatarPath = testPath;
                    break;
                }
            }

            if (avatarPath == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileUploadService.getFilePath(avatarPath);
            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (Exception e) {
            log.error("获取用户头像失败，用户ID: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "删除用户头像", description = "删除用户的头像文件")
    @DeleteMapping("/avatar")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> deleteAvatar(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);

        try {
            String[] extensions = {"jpg", "jpeg", "png", "gif"};
            boolean deleted = false;

            for (String ext : extensions) {
                String avatarPath = "avatars/user_" + userId + "." + ext;
                if (fileUploadService.fileExists(avatarPath)) {
                    fileUploadService.deleteFile(avatarPath);
                    deleted = true;
                    break;
                }
            }

            if (!deleted) {
                return ApiResponse.error("头像文件不存在");
            }

            userService.updateUserAvatar(userId, null);
            return ApiResponse.success(null, "头像删除成功");
        } catch (Exception e) {
            log.error("删除头像失败，用户ID: {}", userId, e);
            return ApiResponse.error("删除头像失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取系统文件统计", description = "管理员获取系统文件统计信息")
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> getFileStatistics() {
        try {
            return ApiResponse.success(Map.of(
                    "supportedTypes", fileUploadService.getSupportedFileTypes(),
                    "maxFileSize", fileUploadService.getMaxFileSize(),
                    "maxFileSizeReadable", formatFileSize(fileUploadService.getMaxFileSize())
            ));
        } catch (Exception e) {
            log.error("获取文件统计失败", e);
            return ApiResponse.error("获取文件统计失败: " + e.getMessage());
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
