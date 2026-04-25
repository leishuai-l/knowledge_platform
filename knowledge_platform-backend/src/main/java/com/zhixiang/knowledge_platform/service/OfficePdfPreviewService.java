package com.zhixiang.knowledge_platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OfficePdfPreviewService {

    @Value("${zhixiang.file.preview.cache-path:${zhixiang.file.upload-path}/previews}")
    private String previewCachePath;

    @Value("${zhixiang.preview.office-pdf.enabled:false}")
    private boolean enabled;

    @Value("${zhixiang.preview.office-pdf.libreoffice-path:}")
    private String libreOfficePath;

    @Value("${zhixiang.preview.office-pdf.timeout-ms:60000}")
    private long timeoutMs;

    @Value("${zhixiang.preview.office-pdf.profile-dir:}")
    private String profileDir;

    public Path getOrCreatePreviewPdf(Path sourceFile, String relativePath) {
        log.info("=== Office PDF 转换开始 === sourceFile={}, relativePath={}, enabled={}, libreOfficePath={}, profileDir={}",
            sourceFile, relativePath, enabled, libreOfficePath, profileDir);

        if (!enabled || libreOfficePath == null || libreOfficePath.isBlank()) {
            log.warn("Office PDF 转换未启用: enabled={}, libreOfficePath={}", enabled, libreOfficePath);
            return null;
        }

        try {
            Path executable = Paths.get(libreOfficePath);
            if (!Files.exists(executable)) {
                log.warn("LibreOffice 可执行文件不存在: {}", libreOfficePath);
                return null;
            }

            Path cacheDir = Paths.get(previewCachePath, "office-pdf");
            Files.createDirectories(cacheDir);

            String cacheKey = buildCacheKey(sourceFile, relativePath);
            Path targetPdf = cacheDir.resolve(cacheKey + ".pdf");
            if (Files.exists(targetPdf)) {
                log.info("使用缓存的 PDF: {}", targetPdf);
                return targetPdf;
            }

            Path workDir = Files.createTempDirectory(cacheDir, "lo-");
            try {
                String extension = getExtension(sourceFile.getFileName().toString());
                Path tempInput = workDir.resolve("source." + extension);
                Path tempOutput = workDir.resolve("source.pdf");
                Files.copy(sourceFile, tempInput, StandardCopyOption.REPLACE_EXISTING);

                List<String> command = new ArrayList<>();
                command.add(executable.toString());
                if (profileDir != null && !profileDir.isBlank()) {
                    Path userProfile = Paths.get(profileDir).toAbsolutePath().normalize();
                    Files.createDirectories(userProfile);
                    command.add("-env:UserInstallation=" + userProfile.toUri());
                    log.info("使用 LibreOffice profile 目录: {}", userProfile);
                }
                command.add("--headless");
                command.add("--convert-to");
                command.add("pdf");
                command.add("--outdir");
                command.add(workDir.toString());
                command.add(tempInput.toString());

                log.info("执行 LibreOffice 命令: {}", String.join(" ", command));

                Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

                String output;
                try (InputStream stream = process.getInputStream()) {
                    output = new String(stream.readAllBytes());
                }

                log.info("LibreOffice 进程输出: {}", output);

                boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    log.warn("LibreOffice 转换超时: source={}", sourceFile);
                    return null;
                }

                if (process.exitValue() != 0 || !Files.exists(tempOutput)) {
                    log.warn("LibreOffice 转换失败: source={}, exitCode={}, output={}", sourceFile, process.exitValue(), output);
                    return null;
                }

                Files.move(tempOutput, targetPdf, StandardCopyOption.REPLACE_EXISTING);
                log.info("PDF 转换成功: {}", targetPdf);
                return targetPdf;
            } finally {
                deleteDirectoryQuietly(workDir);
            }
        } catch (Exception e) {
            log.error("生成 Office PDF 预览异常: sourceFile={}", sourceFile, e);
            return null;
        }
    }

    private String buildCacheKey(Path sourceFile, String relativePath) throws Exception {
        String raw = relativePath + "|" + Files.size(sourceFile) + "|" + Files.getLastModifiedTime(sourceFile).toMillis();
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(digest.digest(raw.getBytes()));
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (var walk = Files.walk(directory)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.debug("删除临时目录失败: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.debug("清理临时目录失败: {}", directory, e);
        }
    }
}