package com.zhixiang.knowledge_platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务 - 本地存储
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${zhixiang.file.upload-path:/app/uploads}")
    private String localUploadPath;

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file, String category) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        String filePath = buildFilePath(category, fileName);
        return uploadToLocal(file, filePath);
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String filePath) {
        try {
            return deleteFromLocal(filePath);
        } catch (Exception e) {
            log.error("删除文件失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 获取文件下载URL
     */
    public String getFileUrl(String filePath) {
        return getLocalFileUrl(filePath);
    }

    /**
     * 获取文件流
     */
    public InputStream getFileStream(String filePath) throws IOException {
        return getLocalFileStream(filePath);
    }

    /**
     * 上传到本地存储（安全版本）
     */
    private String uploadToLocal(MultipartFile file, String filePath) throws IOException {
        // 规范化路径，防止路径遍历
        Path basePath = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(filePath.replace("/", File.separator)).normalize();

        // 安全检查：确保文件在上传目录内
        if (!fullPath.startsWith(basePath)) {
            throw new SecurityException("非法的文件路径");
        }

        // 创建目录
        if (fullPath.getParent() != null) {
            Files.createDirectories(fullPath.getParent());
        }

        // 保存文件
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, fullPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传到本地成功: {}", fullPath);
            return filePath;
        }
    }

    /**
     * 从本地删除文件（安全版本）
     */
    private boolean deleteFromLocal(String filePath) {
        try {
            Path basePath = Paths.get(localUploadPath).toAbsolutePath().normalize();
            Path fullPath = basePath.resolve(filePath.replace("/", File.separator)).normalize();

            // 安全检查
            if (!fullPath.startsWith(basePath)) {
                log.warn("检测到路径遍历攻击尝试: {}", filePath);
                return false;
            }

            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                log.info("从本地删除文件成功: {}", fullPath);
            }
            return deleted;
        } catch (Exception e) {
            log.error("从本地删除文件失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 获取本地文件URL
     */
    private String getLocalFileUrl(String filePath) {
        // 本地文件通过API接口提供下载
        return "/api/files/download/" + filePath.replace("\\", "/");
    }

    /**
     * 获取本地文件流（安全版本）
     */
    private InputStream getLocalFileStream(String filePath) throws IOException {
        Path basePath = Paths.get(localUploadPath).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(filePath.replace("/", File.separator)).normalize();

        // 安全检查
        if (!fullPath.startsWith(basePath)) {
            throw new SecurityException("非法的文件路径");
        }

        if (!Files.exists(fullPath)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return Files.newInputStream(fullPath);
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 构建文件路径
     */
    private String buildFilePath(String category, String fileName) {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.join("/", category, dateDir, fileName);
    }
}