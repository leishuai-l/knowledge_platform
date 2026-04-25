package com.zhixiang.knowledge_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${zhixiang.file.upload-path}")
    private String uploadBasePath;

    @Value("${zhixiang.file.max-size}")
    private long maxFileSize;

    private final FileValidationService fileValidationService;
    private final FileTypeRegistryService fileTypeRegistryService;

    /**
     * 上传文档文件
     */
    public FileUploadResult uploadDocument(MultipartFile file, String documentId) throws IOException {
        log.info("开始上传文档文件，文档ID: {}, 文件名: {}, 大小: {} bytes",
                documentId, file.getOriginalFilename(), file.getSize());

        validateFile(file);
        return saveDocumentFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize(), documentId);
    }

    /**
     * 导入公开文档文件（非 MultipartFile 入口）
     */
    public FileUploadResult importDocument(InputStream inputStream,
                                           String originalFileName,
                                           String contentType,
                                           long fileSize,
                                           String documentId) throws IOException {
        log.info("开始导入文档文件，文档ID: {}, 文件名: {}, 大小: {} bytes", documentId, originalFileName, fileSize);

        validateImportedFile(originalFileName, contentType, fileSize);
        return saveDocumentFile(inputStream, originalFileName, contentType, fileSize, documentId);
    }

    /**
     * 上传用户头像
     */
    public FileUploadResult uploadAvatar(MultipartFile file, Long userId) throws IOException {
        log.info("开始上传用户头像，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename());

        // 头像文件验证（仅允许图片）
        validateImageFile(file);

        // 生成头像文件路径
        String fileName = "user_" + userId + "." + FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        String relativePath = "avatars/" + fileName;
        String absolutePath = uploadBasePath + "/" + relativePath;

        // 创建目录
        Path uploadPath = Paths.get(absolutePath).getParent();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 保存文件
        Path targetPath = Paths.get(absolutePath);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        FileUploadResult result = new FileUploadResult();
        result.setOriginalFileName(file.getOriginalFilename());
        result.setStoredFileName(fileName);
        result.setRelativePath(relativePath);
        result.setAbsolutePath(absolutePath);
        result.setFileSize(file.getSize());
        result.setFileType(file.getContentType());
        result.setFileExtension(FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase());

        log.info("头像上传成功，保存路径: {}", absolutePath);
        return result;
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadBasePath, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", relativePath);
                return true;
            } else {
                log.warn("要删除的文件不存在: {}", relativePath);
                return false;
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", relativePath, e);
            return false;
        }
    }

    /**
     * 获取文件下载路径（安全版本，防止路径遍历攻击）
     */
    public Path getFilePath(String relativePath) {
        if (relativePath == null) {
            return Paths.get(uploadBasePath);
        }

        // 规范化路径，统一使用系统分隔符
        String normalizedPath = relativePath.replace("/", java.io.File.separator)
                                          .replace("\\", java.io.File.separator);

        // 构建完整路径
        Path basePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(normalizedPath).normalize();

        // 安全检查：确保解析后的路径仍在上传目录内
        if (!fullPath.startsWith(basePath)) {
            log.warn("检测到路径遍历攻击尝试: {}", relativePath);
            throw new SecurityException("非法的文件路径");
        }

        return fullPath;
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String relativePath) {
        if (relativePath == null) return false;
        Path filePath = getFilePath(relativePath);
        return Files.exists(filePath);
    }

    /**
     * 获取文件大小
     */
    public long getFileSize(String relativePath) throws IOException {
        if (relativePath == null) return 0;
        Path filePath = getFilePath(relativePath);
        if (Files.exists(filePath)) {
            return Files.size(filePath);
        }
        return 0;
    }

    /**
     * 验证文件（使用增强的安全验证服务）
     */
    private void validateFile(MultipartFile file) {
        FileValidationService.FileValidationResult result = fileValidationService.validateFile(file);

        if (!result.isValid()) {
            String errorMessage = result.getErrorMessage();
            log.warn("文件验证失败: {}, 错误: {}", file.getOriginalFilename(), errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.info("文件验证通过: {}", file.getOriginalFilename());
    }

    /**
     * 验证导入文件的基础信息
     */
    private void validateImportedFile(String originalFileName, String contentType, long fileSize) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException("文件大小超出限制");
        }

        String extension = FilenameUtils.getExtension(originalFileName).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(extension) || !fileTypeRegistryService.isSupportedUploadExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }

        if (!StringUtils.hasText(contentType) || !fileTypeRegistryService.isMimeTypeAllowedForExtension(extension, contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
    }

    /**
     * 保存文档文件并返回统一结果
     */
    private FileUploadResult saveDocumentFile(InputStream inputStream,
                                              String originalFileName,
                                              String contentType,
                                              long fileSize,
                                              String documentId) throws IOException {
        String fileName = generateFileName(originalFileName);
        String relativePath = generateDocumentPath(documentId) + "/" + fileName;
        Path targetPath = Paths.get(uploadBasePath).resolve(relativePath).normalize();
        Path uploadPath = targetPath.getParent();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("创建上传目录: {}", uploadPath);
        }

        try (InputStream in = inputStream) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String md5;
        try (InputStream is = Files.newInputStream(targetPath)) {
            md5 = org.springframework.util.DigestUtils.md5DigestAsHex(is);
        }

        FileUploadResult result = new FileUploadResult();
        result.setOriginalFileName(originalFileName);
        result.setStoredFileName(fileName);
        result.setRelativePath(relativePath.replace(java.io.File.separatorChar, '/'));
        result.setAbsolutePath(targetPath.toString());
        result.setFileSize(fileSize);
        result.setFileType(contentType);
        result.setFileExtension(FilenameUtils.getExtension(originalFileName).toLowerCase(Locale.ROOT));
        result.setMd5(md5);

        log.info("文件保存成功，保存路径: {}, MD5: {}", targetPath, md5);
        return result;
    }

    /**
     * 验证图片文件（用于头像上传）
     */
    private void validateImageFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检查文件名
        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件大小（头像限制为5MB）
        long maxAvatarSize = 5 * 1024 * 1024;
        if (file.getSize() > maxAvatarSize) {
            throw new IllegalArgumentException("头像文件大小不能超过 5MB");
        }

        // 检查是否为图片文件
        String extension = FilenameUtils.getExtension(originalFileName).toLowerCase();
        if (!Set.of("jpg", "jpeg", "png", "gif").contains(extension)) {
            throw new IllegalArgumentException("头像只支持 JPG、PNG、GIF 格式");
        }

        // 检查MIME类型
        String contentType = file.getContentType();
        if (!Set.of("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IllegalArgumentException("不支持的图片格式: " + contentType);
        }
    }

    /**
     * 生成文件名（避免重名冲突）
     */
    private String generateFileName(String originalFileName) {
        String extension = FilenameUtils.getExtension(originalFileName);
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s_%s_%s.%s", baseName, timestamp, uuid, extension);
    }

    /**
     * 生成文档存储路径（按年月组织）
     */
    private String generateDocumentPath(String documentId) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("documents/%d/%02d/doc_%s",
                now.getYear(), now.getMonthValue(), documentId);
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
     * 获取支持的文件类型列表
     */
    public Set<String> getSupportedFileTypes() {
        return new HashSet<>(fileTypeRegistryService.getSupportedUploadExtensions());
    }

    /**
     * 获取最大文件大小
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * 文件上传结果类
     */
    public static class FileUploadResult {
        private String originalFileName;
        private String storedFileName;
        private String relativePath;
        private String absolutePath;
        private long fileSize;
        private String fileType;
        private String fileExtension;
        private String md5;

        // Getters and Setters
        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public String getStoredFileName() {
            return storedFileName;
        }

        public void setStoredFileName(String storedFileName) {
            this.storedFileName = storedFileName;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        public String getAbsolutePath() {
            return absolutePath;
        }

        public void setAbsolutePath(String absolutePath) {
            this.absolutePath = absolutePath;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }
}