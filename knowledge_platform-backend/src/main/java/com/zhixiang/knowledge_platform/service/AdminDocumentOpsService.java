package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.AdminPublicDocumentImportRequest;
import com.zhixiang.knowledge_platform.dto.request.AdminUploadsCleanupRequest;
import com.zhixiang.knowledge_platform.dto.request.DocumentUploadRequest;
import com.zhixiang.knowledge_platform.dto.response.AdminPublicDocumentImportResponse;
import com.zhixiang.knowledge_platform.dto.response.AdminUploadsCleanupResponse;
import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.CategoryRepository;
import com.zhixiang.knowledge_platform.repository.CopyrightReportRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import com.zhixiang.knowledge_platform.service.ai.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDocumentOpsService {

    private static final Set<String> DEMO_ROOT_FILES = Set.of(
            "公开文档.txt",
            "公开预览文档.txt",
            "待审核文档.txt",
            "上传者待审文档.txt",
            "待审预览文档.txt",
            "游客预览文档.txt",
            "管理员待审文档.txt",
            "缩略图文档.png",
            "压缩包文档.zip"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CopyrightReportRepository copyrightReportRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentService documentService;
    private final DocumentInitialReviewService initialReviewService;
    private final FileUploadService fileUploadService;
    private final ObjectProvider<DocumentIngestionService> documentIngestionServiceProvider;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public AdminPublicDocumentImportResponse importPublicDocuments(AdminPublicDocumentImportRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (!admin.isAdmin()) {
            throw new RuntimeException("只有管理员可以导入公开文档");
        }

        AdminPublicDocumentImportResponse response = new AdminPublicDocumentImportResponse();
        List<AdminPublicDocumentImportResponse.ImportItemResult> results = new ArrayList<>();

        for (AdminPublicDocumentImportRequest.ImportItem item : request.getItems()) {
            AdminPublicDocumentImportResponse.ImportItemResult itemResult = new AdminPublicDocumentImportResponse.ImportItemResult();
            itemResult.setUrl(item.getUrl());
            itemResult.setTitle(item.getTitle());

            try {
                Document document = importSingleItem(item, adminId);
                itemResult.setSuccess(true);
                itemResult.setMessage("导入成功");
                itemResult.setDocumentId(document.getId());
                itemResult.setRelativePath(document.getFilePath());
                itemResult.setDocumentStatus(document.getStatus().name());
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception e) {
                log.error("导入公开文档失败: url={}, title={}", item.getUrl(), item.getTitle(), e);
                itemResult.setSuccess(false);
                itemResult.setMessage(e.getMessage());
                response.setFailureCount(response.getFailureCount() + 1);
            }

            results.add(itemResult);
        }

        response.setItems(results);
        return response;
    }

    @Transactional
    protected Document importSingleItem(AdminPublicDocumentImportRequest.ImportItem item, Long adminId) throws Exception {
        validateImportItem(item);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(item.getUrl()))
                .GET()
                .timeout(Duration.ofMinutes(2))
                .header("User-Agent", "KnowledgePlatformAdminImporter/1.0")
                .build();

        HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (httpResponse.statusCode() < HttpStatus.OK.value() || httpResponse.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
            throw new IllegalArgumentException("下载失败，HTTP 状态码: " + httpResponse.statusCode());
        }

        String contentType = normalizeContentType(httpResponse.headers().firstValue("Content-Type").orElse(null));
        long contentLength = httpResponse.headers().firstValueAsLong("Content-Length").orElse(-1L);
        String originalFileName = resolveFileName(item.getUrl(), httpResponse, contentType, item.getTitle());

        FileUploadService.FileUploadResult uploadResult;
        try (InputStream bodyStream = httpResponse.body()) {
            uploadResult = fileUploadService.importDocument(bodyStream, originalFileName, contentType, contentLength, UUID.randomUUID().toString());
        }

        try {
            DocumentUploadRequest uploadRequest = new DocumentUploadRequest();
            uploadRequest.setTitle(item.getTitle());
            uploadRequest.setDescription(item.getDescription());
            uploadRequest.setCategoryId(item.getCategoryId());
            uploadRequest.setDownloadPoints(item.getDownloadPoints());
            uploadRequest.setTags(item.getTags());

            Document document = documentService.createDocumentWithFileInfo(
                    uploadRequest,
                    adminId,
                    uploadResult.getOriginalFileName(),
                    uploadResult.getRelativePath(),
                    uploadResult.getFileSize(),
                    uploadResult.getFileType(),
                    uploadResult.getFileExtension(),
                    uploadResult.getMd5()
            );

            initialReviewService.performInitialReview(document);
            if (item.getTags() != null && !item.getTags().isEmpty()) {
                documentService.addTagsToDocument(document.getId(), item.getTags());
            }
            triggerAiIngestion(document);
            return documentService.getDocumentById(document.getId());
        } catch (Exception e) {
            fileUploadService.deleteFile(uploadResult.getRelativePath());
            throw e;
        }
    }

    public AdminUploadsCleanupResponse cleanupUploads(AdminUploadsCleanupRequest request) {
        AdminUploadsCleanupResponse response = new AdminUploadsCleanupResponse();
        response.setDryRun(Boolean.TRUE.equals(request.getDryRun()));

        List<CleanupCandidate> candidates = collectCleanupCandidates(request);
        response.setCandidateCount(candidates.size());

        for (CleanupCandidate candidate : candidates) {
            AdminUploadsCleanupResponse.CleanupItemResult itemResult = new AdminUploadsCleanupResponse.CleanupItemResult();
            itemResult.setRelativePath(candidate.relativePath());
            itemResult.setGroup(candidate.group());
            itemResult.setHasDatabaseReference(candidate.reference().hasReference());
            itemResult.setReferenceDetail(candidate.reference().detail());

            try {
                if (response.isDryRun()) {
                    itemResult.setDeleted(false);
                    itemResult.setMessage(buildDryRunMessage(candidate));
                    response.setSkippedCount(response.getSkippedCount() + 1);
                } else {
                    executeCleanupCandidate(candidate, itemResult, response);
                }
            } catch (Exception e) {
                log.error("清理上传文件失败: {}", candidate.relativePath(), e);
                itemResult.setDeleted(false);
                itemResult.setMessage("清理失败: " + e.getMessage());
                response.setFailedCount(response.getFailedCount() + 1);
            }

            response.getItems().add(itemResult);
        }

        return response;
    }

    private void executeCleanupCandidate(CleanupCandidate candidate,
                                         AdminUploadsCleanupResponse.CleanupItemResult itemResult,
                                         AdminUploadsCleanupResponse response) {
        ReferenceInfo referenceInfo = candidate.reference();

        if (referenceInfo.blocksDeletion()) {
            itemResult.setDeleted(false);
            itemResult.setMessage("已跳过: " + referenceInfo.detail());
            response.setSkippedCount(response.getSkippedCount() + 1);
            return;
        }

        if (!candidate.allowDeleteWhenReferenced() && referenceInfo.hasReference()) {
            itemResult.setDeleted(false);
            itemResult.setMessage("已跳过: 文件仍被引用 - " + referenceInfo.detail());
            response.setSkippedCount(response.getSkippedCount() + 1);
            return;
        }

        if (referenceInfo.document() != null) {
            Document document = referenceInfo.document();
            document.delete("管理员清理历史演示文件");
            documentRepository.save(document);
        }

        boolean deleted = fileUploadService.deleteFile(candidate.relativePath());
        itemResult.setDeleted(deleted);
        itemResult.setMessage(deleted ? "已删除" : "文件不存在或删除失败");

        if (deleted) {
            response.setDeletedCount(response.getDeletedCount() + 1);
        } else {
            response.setFailedCount(response.getFailedCount() + 1);
        }
    }

    private String buildDryRunMessage(CleanupCandidate candidate) {
        ReferenceInfo referenceInfo = candidate.reference();
        if (referenceInfo.blocksDeletion()) {
            return "候选项，但当前被引用阻止删除: " + referenceInfo.detail();
        }
        if (referenceInfo.hasReference()) {
            return candidate.allowDeleteWhenReferenced()
                    ? "候选项，将同步下线关联文档后删除: " + referenceInfo.detail()
                    : "候选项，但仍有引用: " + referenceInfo.detail();
        }
        return "候选项，可直接删除";
    }

    private List<CleanupCandidate> collectCleanupCandidates(AdminUploadsCleanupRequest request) {
        Path uploadBasePath = fileUploadService.getFilePath(null);
        Map<String, CleanupCandidate> candidates = new LinkedHashMap<>();

        if (request.isCleanupPreviews()) {
            collectDirectoryCandidates(uploadBasePath.resolve("previews"), "preview-cache", false, candidates);
        }
        if (request.isCleanupTemp()) {
            collectDirectoryCandidates(uploadBasePath.resolve("temp"), "temp-cache", false, candidates);
        }
        if (request.isCleanupDemoRootFiles()) {
            collectDemoRootFileCandidates(uploadBasePath.resolve("documents"), candidates);
        }
        if (request.isCleanupApril2026DemoFiles()) {
            collectDirectoryCandidates(uploadBasePath.resolve(Paths.get("documents", "2026", "04")), "demo-april-2026", true, candidates);
        }
        if (request.isCleanupOrphanFiles()) {
            collectDirectoryCandidates(uploadBasePath.resolve(Paths.get("documents", "2025")), "documents-2025-orphan", false, candidates);
            collectDirectoryCandidates(uploadBasePath.resolve(Paths.get("documents", "2026", "03")), "documents-2026-03-orphan", false, candidates);
            collectDirectoryCandidates(uploadBasePath.resolve("avatars"), "avatars-orphan", false, candidates);
            collectDirectoryCandidates(uploadBasePath.resolve("reports"), "reports-orphan", false, candidates);
        }

        return new ArrayList<>(candidates.values());
    }

    private void collectDirectoryCandidates(Path directory,
                                            String group,
                                            boolean allowDeleteWhenReferenced,
                                            Map<String, CleanupCandidate> candidates) {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> addCandidate(path, group, allowDeleteWhenReferenced, candidates));
        } catch (IOException e) {
            throw new RuntimeException("扫描目录失败: " + directory, e);
        }
    }

    private void collectDemoRootFileCandidates(Path documentsRoot, Map<String, CleanupCandidate> candidates) {
        if (!Files.exists(documentsRoot)) {
            return;
        }

        try (Stream<Path> stream = Files.list(documentsRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> DEMO_ROOT_FILES.contains(path.getFileName().toString()))
                    .forEach(path -> addCandidate(path, "demo-root", true, candidates));
        } catch (IOException e) {
            throw new RuntimeException("扫描 documents 根目录失败", e);
        }
    }

    private void addCandidate(Path path,
                              String group,
                              boolean allowDeleteWhenReferenced,
                              Map<String, CleanupCandidate> candidates) {
        String relativePath = toRelativeUploadPath(path);
        if (!StringUtils.hasText(relativePath) || "avatars/default.png".equals(relativePath)) {
            return;
        }

        candidates.putIfAbsent(relativePath,
                new CleanupCandidate(relativePath, group, allowDeleteWhenReferenced, inspectReference(relativePath)));
    }

    private ReferenceInfo inspectReference(String relativePath) {
        Document document = documentRepository.findAll().stream()
                .filter(item -> relativePath.equals(normalizeStoredPath(item.getFilePath())))
                .findFirst()
                .orElse(null);
        if (document != null) {
            return new ReferenceInfo(true, false, "documents#" + document.getId() + " status=" + document.getStatus().name(), document);
        }

        User avatarUser = userRepository.findAll().stream()
                .filter(user -> relativePath.equals(normalizeStoredPath(user.getAvatar())))
                .findFirst()
                .orElse(null);
        if (avatarUser != null) {
            return new ReferenceInfo(true, true, "users#" + avatarUser.getId() + " avatar", null);
        }

        CopyrightReport report = copyrightReportRepository.findAll().stream()
                .filter(item -> parseEvidenceUrls(item.getEvidenceUrls()).contains(relativePath))
                .findFirst()
                .orElse(null);
        if (report != null) {
            return new ReferenceInfo(true, true, "copyright_reports#" + report.getId() + " evidence", null);
        }

        return new ReferenceInfo(false, false, "无数据库引用", null);
    }

    private List<String> parseEvidenceUrls(String evidenceUrls) {
        if (!StringUtils.hasText(evidenceUrls)) {
            return List.of();
        }
        return Arrays.stream(evidenceUrls.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeStoredPath)
                .collect(Collectors.toList());
    }

    private String toRelativeUploadPath(Path fullPath) {
        Path uploadBasePath = fileUploadService.getFilePath(null).toAbsolutePath().normalize();
        Path normalized = fullPath.toAbsolutePath().normalize();
        if (!normalized.startsWith(uploadBasePath)) {
            return null;
        }
        return normalizeStoredPath(uploadBasePath.relativize(normalized).toString());
    }

    private String normalizeStoredPath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    private void validateImportItem(AdminPublicDocumentImportRequest.ImportItem item) {
        if (!categoryRepository.existsById(item.getCategoryId())) {
            throw new IllegalArgumentException("分类不存在: " + item.getCategoryId());
        }
        if (!item.getUrl().startsWith("http://") && !item.getUrl().startsWith("https://")) {
            throw new IllegalArgumentException("仅支持 http/https 公开文件URL");
        }
    }

    private String normalizeContentType(String rawContentType) {
        String normalized = rawContentType == null ? null : rawContentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return "application/octet-stream";
        }
        return switch (normalized) {
            case "application/x-zip-compressed" -> "application/zip";
            case "application/rar", "application/vnd.rar" -> "application/x-rar-compressed";
            default -> normalized;
        };
    }

    private String resolveFileName(String url,
                                   HttpResponse<InputStream> response,
                                   String contentType,
                                   String fallbackTitle) {
        String contentDisposition = response.headers().firstValue("Content-Disposition").orElse(null);
        if (StringUtils.hasText(contentDisposition)) {
            for (String part : contentDisposition.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("filename=")) {
                    return trimmed.substring("filename=".length()).replace("\"", "");
                }
            }
        }

        String path = URI.create(url).getPath();
        if (StringUtils.hasText(path)) {
            String candidate = Paths.get(path).getFileName().toString();
            if (candidate.contains(".")) {
                return candidate;
            }
        }

        String extension = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "text/plain" -> ".txt";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "application/zip" -> ".zip";
            case "application/x-rar-compressed" -> ".rar";
            default -> throw new IllegalArgumentException("无法从响应中识别受支持的文件类型: " + contentType);
        };

        return fallbackTitle.replaceAll("[\\\\/:*?\"<>|]", "_") + extension;
    }

    private void triggerAiIngestion(Document document) {
        try {
            DocumentIngestionService documentIngestionService = documentIngestionServiceProvider.getIfAvailable();
            if (documentIngestionService == null) {
                return;
            }
            Path filePath = fileUploadService.getFilePath(document.getFilePath());
            documentIngestionService.ingestDocument(filePath.toFile(), document.getId(), document.getTitle());
        } catch (Exception e) {
            log.warn("触发导入文档 AI 处理失败, documentId={}", document.getId(), e);
        }
    }

    private record CleanupCandidate(String relativePath,
                                    String group,
                                    boolean allowDeleteWhenReferenced,
                                    ReferenceInfo reference) {
    }

    private record ReferenceInfo(boolean hasReference,
                                 boolean blocksDeletion,
                                 String detail,
                                 Document document) {
    }
}
