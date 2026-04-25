package com.zhixiang.knowledge_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 文件验证服务
 * 提供文件安全性验证、类型检查、大小限制等功能
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileValidationService {

    @Value("${zhixiang.file.max-size:104857600}")
    private long maxFileSize;

    private final FileTypeRegistryService fileTypeRegistryService;

    // 文件签名映射 - 通过文件头字节验证真实文件类型
    private static final Map<String, List<String>> FILE_SIGNATURES = new HashMap<>();

    // 危险文件扩展名
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "scr", "pif", "com", "vbs", "js", "jar",
        "dll", "sys", "msi", "ps1", "sh", "bin", "app", "deb", "rpm"
    );

    static {
        FILE_SIGNATURES.put("application/pdf", Arrays.asList("25504446"));
        FILE_SIGNATURES.put("application/msword", Arrays.asList("D0CF11E0A1B11AE1"));
        FILE_SIGNATURES.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Arrays.asList("504B0304", "504B030414", "D0CF11E0A1B11AE1")); // Added OLE2 signature
        FILE_SIGNATURES.put("application/vnd.ms-excel", Arrays.asList("D0CF11E0A1B11AE1"));
        FILE_SIGNATURES.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            Arrays.asList("504B0304", "504B030414"));
        FILE_SIGNATURES.put("application/vnd.ms-powerpoint", Arrays.asList("D0CF11E0A1B11AE1"));
        FILE_SIGNATURES.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
            Arrays.asList("504B0304", "504B030414"));
        FILE_SIGNATURES.put("text/plain", Arrays.asList("EFBBBF", "FEFF", "FFFE"));
        // Allow text files without BOM
        FILE_SIGNATURES.put("image/jpeg", Arrays.asList("FFD8FF"));
        FILE_SIGNATURES.put("image/png", Arrays.asList("89504E47"));
        FILE_SIGNATURES.put("image/gif", Arrays.asList("47494638"));
        FILE_SIGNATURES.put("application/zip", Arrays.asList("504B0304", "504B0506", "504B0708"));
        FILE_SIGNATURES.put("application/x-rar-compressed", Arrays.asList("526172211A0700", "526172211A070100"));
        // Add YAML support
        FILE_SIGNATURES.put("application/x-yaml", Arrays.asList("2D2D2D")); // "---"
        FILE_SIGNATURES.put("text/yaml", Arrays.asList("2D2D2D"));
    }

    /**
     * 验证上传文件的安全性和合规性
     */
    public FileValidationResult validateFile(MultipartFile file) {
        log.info("开始验证文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        FileValidationResult result = new FileValidationResult();

        try {
            // 1. 基础检查
            if (!validateBasicInfo(file, result)) {
                return result;
            }

            // 2. 文件大小检查
            if (!validateFileSize(file, result)) {
                return result;
            }

            // 3. 文件扩展名检查
            if (!validateFileExtension(file, result)) {
                return result;
            }

            // 4. MIME类型检查
            if (!validateMimeType(file, result)) {
                return result;
            }

            // 5. 文件头签名验证
            if (!validateFileSignature(file, result)) {
                return result;
            }

            // 6. 文件名安全检查
            if (!validateFileName(file, result)) {
                return result;
            }

            // 7. 内容安全扫描
            if (!validateFileContent(file, result)) {
                return result;
            }

            result.setValid(true);
            result.addMessage("文件验证通过");
            log.info("文件验证成功: {}", file.getOriginalFilename());

        } catch (Exception e) {
            log.error("文件验证过程中发生异常: {}", file.getOriginalFilename(), e);
            result.setValid(false);
            result.addError("文件验证过程中发生错误: " + e.getMessage());
        }

        return result;
    }

    /**
     * 基础信息检查
     */
    private boolean validateBasicInfo(MultipartFile file, FileValidationResult result) {
        if (file == null || file.isEmpty()) {
            result.addError("文件不能为空");
            return false;
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            result.addError("文件名不能为空");
            return false;
        }

        return true;
    }

    /**
     * 文件大小检查
     */
    private boolean validateFileSize(MultipartFile file, FileValidationResult result) {
        if (file.getSize() > maxFileSize) {
            result.addError(String.format("文件大小超出限制，最大允许 %d MB", maxFileSize / (1024 * 1024)));
            return false;
        }

        if (file.getSize() == 0) {
            result.addError("文件内容为空");
            return false;
        }

        return true;
    }

    /**
     * 文件扩展名检查
     */
    private boolean validateFileExtension(MultipartFile file, FileValidationResult result) {
        String filename = file.getOriginalFilename();
        String extension = getFileExtension(filename).toLowerCase();

        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            result.addError("不允许上传可执行文件或脚本文件");
            return false;
        }

        // 检查双重扩展名（如：virus.txt.exe）
        String[] parts = filename.split("\\.");
        if (parts.length > 2) {
            for (int i = 1; i < parts.length - 1; i++) {
                if (DANGEROUS_EXTENSIONS.contains(parts[i].toLowerCase())) {
                    result.addError("检测到可疑的双重扩展名");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * MIME类型检查
     */
    private boolean validateMimeType(MultipartFile file, FileValidationResult result) {
        String mimeType = file.getContentType();

        if (mimeType == null) {
            result.addError("无法识别文件类型");
            return false;
        }

        if (!fileTypeRegistryService.isAllowedMimeType(mimeType)) {
            result.addError("不支持的文件类型: " + mimeType);
            result.addMessage("支持的类型: " + String.join(", ", fileTypeRegistryService.getSupportedUploadMimeTypes()));
            return false;
        }

        return true;
    }

    /**
     * 文件头签名验证
     */
    private boolean validateFileSignature(MultipartFile file, FileValidationResult result) {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                result.addError("文件内容过短，无法验证文件类型");
                return false;
            }

            String fileSignature = bytesToHex(Arrays.copyOf(fileBytes, Math.min(8, fileBytes.length))).toUpperCase();
            String mimeType = file.getContentType();

            List<String> expectedSignatures = new ArrayList<>(fileTypeRegistryService.getFileSignaturesByMimeType(mimeType));
            if (expectedSignatures.isEmpty()) {
                expectedSignatures = FILE_SIGNATURES.get(mimeType);
            }
            if (expectedSignatures != null) {
                // 对于文本文件，跳过签名验证（文本文件的内容可以是任何字符）
                if ("text/plain".equals(mimeType)) {
                    log.info("跳过文本文件的签名验证: {}", file.getOriginalFilename());
                } else {
                    boolean signatureMatch = expectedSignatures.stream()
                        .map(String::toUpperCase)
                        .anyMatch(sig -> fileSignature.startsWith(sig));

                    if (!signatureMatch) {
                        result.addError("文件头签名与声明的文件类型不匹配，可能是恶意文件");
                        log.warn("文件签名不匹配 - 文件: {}, 声明类型: {}, 实际签名: {}",
                            file.getOriginalFilename(), mimeType, fileSignature);
                        return false;
                    }
                }
            }

        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            result.addError("无法读取文件内容进行验证");
            return false;
        }

        return true;
    }

    /**
     * 文件名安全检查
     */
    private boolean validateFileName(MultipartFile file, FileValidationResult result) {
        String filename = file.getOriginalFilename();

        // 检查文件名长度
        if (filename.length() > 255) {
            result.addError("文件名过长，最大支持255个字符");
            return false;
        }

        // 检查非法字符
        String[] illegalChars = {"<", ">", ":", "\"", "|", "?", "*", "\0"};
        for (String illegalChar : illegalChars) {
            if (filename.contains(illegalChar)) {
                result.addError("文件名包含非法字符: " + illegalChar);
                return false;
            }
        }

        // 检查路径遍历攻击
        if (filename.contains("../") || filename.contains("..\\")) {
            result.addError("文件名包含非法路径字符");
            return false;
        }

        // 检查保留名称
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
            "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5",
            "LPT6", "LPT7", "LPT8", "LPT9"};

        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.') > 0 ?
            filename.lastIndexOf('.') : filename.length()).toUpperCase();

        for (String reserved : reservedNames) {
            if (nameWithoutExt.equals(reserved)) {
                result.addError("文件名使用了系统保留名称");
                return false;
            }
        }

        return true;
    }

    /**
     * 文件内容安全扫描
     */
    private boolean validateFileContent(MultipartFile file, FileValidationResult result) {
        try {
            byte[] content = file.getBytes();

            // 检查文件是否包含可疑的二进制模式
            if (containsSuspiciousPatterns(content)) {
                result.addError("文件内容包含可疑模式，可能是恶意文件");
                return false;
            }

            // 对于文本文件，检查是否包含脚本代码
            if (file.getContentType() != null && file.getContentType().startsWith("text/")) {
                String textContent = new String(content);
                if (containsScriptContent(textContent)) {
                    result.addError("文本文件包含可疑的脚本内容");
                    return false;
                }
            }

        } catch (IOException e) {
            log.error("读取文件内容进行安全扫描失败", e);
            result.addError("无法完成文件内容安全检查");
            return false;
        }

        return true;
    }

    /**
     * 检查是否包含可疑的二进制模式
     */
    private boolean containsSuspiciousPatterns(byte[] content) {
        // 检查常见的恶意软件签名
        String[] suspiciousHexPatterns = {
            "4D5A", // PE文件头
            "7F454C46", // ELF文件头
            "CAFEBABE", // Java class文件
            "504B0304", // ZIP文件（需要进一步检查内容）
        };

        String contentHex = bytesToHex(Arrays.copyOf(content, Math.min(1024, content.length))).toUpperCase();

        for (String pattern : suspiciousHexPatterns) {
            if (contentHex.contains(pattern)) {
                // 对于ZIP格式，需要特殊处理（因为Office文件也是ZIP格式）
                if (pattern.equals("504B0304")) {
                    // 这里可以添加更详细的ZIP内容检查
                    continue;
                }
                log.warn("发现可疑的二进制模式: {}", pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查文本内容是否包含脚本
     */
    private boolean containsScriptContent(String content) {
        String lowerContent = content.toLowerCase();

        String[] scriptPatterns = {
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "onclick=", "onmouseover=", "eval(", "document.write",
            "window.location", "document.cookie"
        };

        for (String pattern : scriptPatterns) {
            if (lowerContent.contains(pattern)) {
                log.warn("文本文件包含可疑脚本内容: {}", pattern);
                return true;
            }
        }

        return false;
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
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 文件验证结果类
     */
    public static class FileValidationResult {
        private boolean valid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> messages = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            this.errors.add(error);
        }

        public List<String> getMessages() {
            return messages;
        }

        public void addMessage(String message) {
            this.messages.add(message);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public String getAllMessages() {
            List<String> allMessages = new ArrayList<>(errors);
            allMessages.addAll(messages);
            return String.join("; ", allMessages);
        }
    }
}