package com.zhixiang.knowledge_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentPreviewService {

    @Value("${zhixiang.preview.cache-path:${zhixiang.file.upload-path}/previews}")
    private String previewCachePath;

    @Value("${zhixiang.preview.max-text-size:1048576}")
    private long maxTextPreviewSize;

    private static final int MAX_ARCHIVE_ENTRIES = 500;
    private static final int MAX_ARCHIVE_DEPTH = 10;
    private static final Set<String> OFFICE_PDF_EXTENSIONS = Set.of("doc", "docx", "ppt", "pptx", "xls", "xlsx");
    private static final Set<String> ARCHIVE_PREVIEWABLE_TEXT_EXTENSIONS = Set.of("txt", "md", "json", "xml", "csv", "log", "java", "js", "html", "css", "py", "sql");
    private static final Set<String> ARCHIVE_PREVIEWABLE_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final long MAX_ARCHIVE_ENTRY_BINARY_PREVIEW_SIZE = 20L * 1024 * 1024;

    private final FileTypeRegistryService fileTypeRegistryService;
    private final FileUploadService fileUploadService;
    private final OfficePdfPreviewService officePdfPreviewService;

    public boolean isPreviewable(String filename) {
        return fileTypeRegistryService.isPreviewEntrySupported(getExtension(filename));
    }

    public boolean isInlinePreviewable(String filename) {
        String extension = getExtension(filename);
        return fileTypeRegistryService.isInlinePreviewSupported(extension) || OFFICE_PDF_EXTENSIONS.contains(extension);
    }

    public DocumentPreviewInfo getPreviewInfo(String relativePath, Long documentId, Long userId, String userRole) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            String filename = filePath.getFileName().toString();
            String extension = getExtension(filename);
            long fileSize = Files.size(filePath);
            FileTypeRegistryService.PreviewType registryPreviewType = fileTypeRegistryService.getPreviewType(extension);

            DocumentPreviewInfo previewInfo = new DocumentPreviewInfo();
            previewInfo.setFilename(filename);
            previewInfo.setExtension(extension);
            previewInfo.setFileSize(fileSize);
            previewInfo.setPreviewable(isPreviewable(filename));

            switch (registryPreviewType) {
                case TEXT -> fillTextPreview(previewInfo, filePath, PreviewType.TEXT);
                case IMAGE -> {
                    previewInfo.setPreviewType(PreviewType.IMAGE);
                    previewInfo.setImageInfo(getImageInfo(filePath));
                }
                case PDF -> {
                    previewInfo.setPreviewType(PreviewType.PDF);
                    previewInfo.setPdfInfo(getPdfInfo(filePath));
                }
                case OFFICE_TEXT -> fillOfficePreview(previewInfo, relativePath, filePath, extension);
                case ARCHIVE_TREE -> fillArchivePreview(previewInfo, filePath, extension);
                case NOT_SUPPORTED -> {
                    previewInfo.setPreviewType(PreviewType.NOT_SUPPORTED);
                    previewInfo.setPreviewable(false);
                }
            }
            return previewInfo;
        } catch (Exception e) {
            log.error("获取文档预览信息失败: {}", relativePath, e);
            throw new RuntimeException("获取预览信息失败: " + e.getMessage());
        }
    }

    public PreviewContent resolvePreviewContent(String relativePath, String filename) {
        try {
            String extension = getExtension(filename);
            if (OFFICE_PDF_EXTENSIONS.contains(extension)) {
                Path officePdf = officePdfPreviewService.getOrCreatePreviewPdf(fileUploadService.getFilePath(relativePath), relativePath);
                if (officePdf != null && Files.exists(officePdf)) {
                    return new PreviewContent(officePdf, "pdf", buildPdfFilename(filename));
                }
                return null;
            }

            if (!fileTypeRegistryService.isInlinePreviewSupported(extension)) {
                return null;
            }

            Path filePath = fileUploadService.getFilePath(relativePath);
            return Files.exists(filePath) ? new PreviewContent(filePath, extension, filename) : null;
        } catch (Exception e) {
            log.warn("解析预览内容失败: {}", relativePath, e);
            return null;
        }
    }

    public TextPreviewPayload getTextPreview(String relativePath, String filename) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            String extension = getExtension(filename);
            TextPreviewResult result = OFFICE_PDF_EXTENSIONS.contains(extension)
                ? readOfficeTextContent(filePath)
                : readTextContent(filePath);

            return new TextPreviewPayload(
                result.content(),
                result.encoding(),
                result.encodingSource(),
                result.previewMessage(),
                result.contentLength(),
                result.truncated(),
                OFFICE_PDF_EXTENSIONS.contains(extension) ? "OFFICE_EXTRACTED" : "RAW_TEXT"
            );
        } catch (Exception e) {
            log.error("获取文本预览负载失败: {}", relativePath, e);
            throw new RuntimeException("获取文本预览失败: " + e.getMessage());
        }
    }

    public ArchivePreviewPayload getArchivePreview(String relativePath, String filename) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            ArchivePreviewResult result = readArchivePreview(filePath, getExtension(filename));
            return new ArchivePreviewPayload(
                result.archiveTree(),
                result.archiveEntries(),
                result.parseSucceeded(),
                result.warnings(),
                result.truncated(),
                result.depthLimited(),
                result.returnedCount(),
                result.totalCount()
            );
        } catch (Exception e) {
            log.error("获取压缩包预览负载失败: {}", relativePath, e);
            throw new RuntimeException("获取压缩包预览失败: " + e.getMessage());
        }
    }

    public ArchiveEntryPreviewInfo getArchiveEntryPreviewInfo(String relativePath, String archiveFilename, String entryPath) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            String normalizedEntryPath = normalizeArchiveEntryPath(entryPath);
            ArchiveEntryInfo entryInfo = findArchiveEntry(filePath, getExtension(archiveFilename), normalizedEntryPath);
            if (entryInfo == null || entryInfo.directory()) {
                return new ArchiveEntryPreviewInfo(false, "NOT_SUPPORTED", normalizedEntryPath, normalizedEntryPath, 0, null, "该文件类型不支持预览", false, null, null, null);
            }

            String extension = entryInfo.extension() == null ? "" : entryInfo.extension().toLowerCase();
            String previewType = resolveArchiveEntryPreviewType(extension);
            if ("NOT_SUPPORTED".equals(previewType)) {
                return new ArchiveEntryPreviewInfo(false, previewType, entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "该文件类型不支持预览", false, null, null, null);
            }

            if ("OFFICE".equals(previewType)) {
                byte[] officeBytes = extractArchiveEntryBytes(filePath, getExtension(archiveFilename), normalizedEntryPath, MAX_ARCHIVE_ENTRY_BINARY_PREVIEW_SIZE);
                if (officeBytes == null) {
                    return new ArchiveEntryPreviewInfo(false, "NOT_SUPPORTED", entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "压缩包内 Office 文件读取失败", false, null, null, null);
                }
                Path tempEntry = writeArchiveEntryToTempFile(normalizedEntryPath, extension, officeBytes);
                Path pdf = officePdfPreviewService.getOrCreatePreviewPdf(tempEntry, relativePath + "#" + normalizedEntryPath);
                if (pdf == null || !Files.exists(pdf)) {
                    return new ArchiveEntryPreviewInfo(false, "NOT_SUPPORTED", entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "该 Office 文件暂时无法预览", false, null, null, null);
                }
                return new ArchiveEntryPreviewInfo(true, "PDF", entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "已转换为 PDF 预览", false, null, null, null);
            }

            if (!"TEXT".equals(previewType) && entryInfo.size() > MAX_ARCHIVE_ENTRY_BINARY_PREVIEW_SIZE) {
                return new ArchiveEntryPreviewInfo(false, previewType, entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "该文件过大，暂不支持预览", false, null, null, null);
            }

            if ("TEXT".equals(previewType)) {
                TextPreviewResult result = readArchiveEntryTextPreview(filePath, getExtension(archiveFilename), normalizedEntryPath);
                return new ArchiveEntryPreviewInfo(true, previewType, entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, result.previewMessage(), result.truncated(), result.encoding(), result.encodingSource(), result.content());
            }

            return new ArchiveEntryPreviewInfo(true, previewType, entryInfo.path(), entryInfo.name(), entryInfo.size(), extension, "可预览", false, null, null, null);
        } catch (Exception e) {
            log.error("获取压缩包内文件预览信息失败: archive={}, path={}", relativePath, entryPath, e);
            throw new RuntimeException("获取压缩包内文件预览失败: " + e.getMessage());
        }
    }

    public ArchiveEntryContent getArchiveEntryContent(String relativePath, String archiveFilename, String entryPath) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }
            String normalizedEntryPath = normalizeArchiveEntryPath(entryPath);
            ArchiveEntryInfo entryInfo = findArchiveEntry(filePath, getExtension(archiveFilename), normalizedEntryPath);
            if (entryInfo == null || entryInfo.directory()) {
                return null;
            }

            String extension = entryInfo.extension() == null ? "" : entryInfo.extension().toLowerCase();
            String previewType = resolveArchiveEntryPreviewType(extension);
            if ("NOT_SUPPORTED".equals(previewType)) {
                return null;
            }

            byte[] bytes = extractArchiveEntryBytes(filePath, getExtension(archiveFilename), normalizedEntryPath,
                "TEXT".equals(previewType) ? maxTextPreviewSize + 1 : MAX_ARCHIVE_ENTRY_BINARY_PREVIEW_SIZE);
            if (bytes == null) {
                return null;
            }

            if (OFFICE_PDF_EXTENSIONS.contains(extension)) {
                Path tempEntry = writeArchiveEntryToTempFile(normalizedEntryPath, extension, bytes);
                Path pdf = officePdfPreviewService.getOrCreatePreviewPdf(tempEntry, relativePath + "#" + normalizedEntryPath);
                if (pdf != null && Files.exists(pdf)) {
                    return new ArchiveEntryContent(Files.readAllBytes(pdf), "application/pdf", buildPdfFilename(entryInfo.name()));
                }
                return null;
            }

            String contentType = switch (previewType) {
                case "PDF" -> MediaType.APPLICATION_PDF_VALUE;
                case "IMAGE" -> resolveImageContentType(extension);
                case "TEXT" -> "text/plain; charset=utf-8";
                default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
            };

            return new ArchiveEntryContent(bytes, contentType, entryInfo.name());
        } catch (Exception e) {
            log.error("获取压缩包内文件内容失败: archive={}, path={}", relativePath, entryPath, e);
            return null;
        }
    }

    private void fillTextPreview(DocumentPreviewInfo previewInfo, Path filePath, PreviewType previewType) throws IOException {
        previewInfo.setPreviewType(previewType);
        TextPreviewResult result = previewType == PreviewType.OFFICE_TEXT
            ? readOfficeTextContent(filePath)
            : readTextContent(filePath);
        previewInfo.setContent(result.content());
        previewInfo.setTruncated(result.truncated());
        previewInfo.setEncoding(result.encoding());
        previewInfo.setEncodingSource(result.encodingSource());
        previewInfo.setPreviewMessage(result.previewMessage());
        previewInfo.setContentLength(result.contentLength());
    }

    private void fillOfficePreview(DocumentPreviewInfo previewInfo, String relativePath, Path filePath, String extension) throws IOException {
        if (OFFICE_PDF_EXTENSIONS.contains(extension)) {
            Path officePdf = officePdfPreviewService.getOrCreatePreviewPdf(filePath, relativePath);
            if (officePdf != null && Files.exists(officePdf)) {
                previewInfo.setPreviewType(PreviewType.PDF);
                previewInfo.setPdfInfo(getPdfInfo(officePdf));
                previewInfo.setConvertedFromOffice(true);
                previewInfo.setFallbackReason("当前为 Office 转 PDF 预览");
                previewInfo.setPreviewMessage("当前为 Office 转 PDF 预览");
                return;
            }
        }

        fillTextPreview(previewInfo, filePath, PreviewType.OFFICE_TEXT);
        previewInfo.setFallbackReason("PDF 转换失败，已切换为文本提取预览");
    }

    private void fillArchivePreview(DocumentPreviewInfo previewInfo, Path filePath, String extension) {
        ArchivePreviewResult result = readArchivePreview(filePath, extension);
        previewInfo.setPreviewType(PreviewType.ARCHIVE_TREE);
        previewInfo.setArchiveEntries(result.archiveEntries());
        previewInfo.setArchiveTree(result.archiveTree());
        previewInfo.setArchiveParseSucceeded(result.parseSucceeded());
        previewInfo.setArchiveWarnings(result.warnings());
        previewInfo.setArchiveTruncated(result.truncated());
        previewInfo.setArchiveDepthLimited(result.depthLimited());
        previewInfo.setArchiveEntryCountReturned(result.returnedCount());
        previewInfo.setArchiveEntryCountTotal(result.totalCount());
        if (!result.parseSucceeded() && (result.warnings() == null || result.warnings().isEmpty())) {
            previewInfo.setFallbackReason("压缩包目录解析失败，无法展示内容");
        }
    }

    private TextPreviewResult readTextContent(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        int length = (int) Math.min(bytes.length, maxTextPreviewSize);
        byte[] previewBytes = bytes.length == length ? bytes : java.util.Arrays.copyOf(bytes, length);
        String encoding = detectEncoding(previewBytes);
        boolean truncated = bytes.length > length;
        String previewMessage = truncated ? "文本内容已按预览上限截断" : "文本内容预览完整";
        return new TextPreviewResult(
            new String(previewBytes, encoding),
            truncated,
            encoding,
            "BOM_OR_UTF8",
            previewMessage,
            previewBytes.length
        );
    }

    private TextPreviewResult readOfficeTextContent(Path filePath) {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            Resource resource = new InputStreamResource(inputStream);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> documents = reader.get();
            String content = documents.stream()
                .map(org.springframework.ai.document.Document::getFormattedContent)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"))
                .trim();
            String normalized = content.isBlank() ? "未能提取到可预览的文档内容" : content;
            boolean truncated = normalized.length() > maxTextPreviewSize;
            String finalContent = truncateContent(normalized);
            return new TextPreviewResult(
                finalContent,
                truncated,
                "EXTRACTED",
                "TIKA",
                truncated ? "提取内容已截断" : "提取内容预览完整",
                finalContent.length()
            );
        } catch (Exception e) {
            log.warn("抽取 Office 文本内容失败: {}", filePath, e);
            return new TextPreviewResult(
                "未能提取到可预览的文档内容",
                false,
                "EXTRACTED",
                "TIKA",
                "未能提取到可预览的文档内容",
                0
            );
        }
    }

    private String truncateContent(String content) {
        if (content.length() <= maxTextPreviewSize) {
            return content;
        }
        return content.substring(0, (int) maxTextPreviewSize) + "\n\n--- 内容已截断 ---";
    }

    private ArchivePreviewResult readArchivePreview(Path filePath, String extension) {
        List<ArchiveEntryInfo> entries = readArchiveEntries(filePath, extension);
        List<String> warnings = new ArrayList<>();
        boolean parseSucceeded = !entries.isEmpty();
        boolean truncated = entries.size() >= MAX_ARCHIVE_ENTRIES;
        boolean depthLimited = entries.stream().anyMatch(entry -> entry.path().split("/").length >= MAX_ARCHIVE_DEPTH);

        if (truncated) {
            warnings.add("仅展示前 " + MAX_ARCHIVE_ENTRIES + " 项");
        }
        if (depthLimited) {
            warnings.add("超过 " + MAX_ARCHIVE_DEPTH + " 层的路径未显示");
        }
        if (!parseSucceeded) {
            warnings.add("压缩包目录解析失败或内容为空");
        }

        return new ArchivePreviewResult(
            entries,
            buildArchiveTree(entries),
            parseSucceeded,
            warnings,
            truncated,
            depthLimited,
            entries.size(),
            entries.size()
        );
    }

    private List<ArchiveEntryInfo> readArchiveEntries(Path filePath, String extension) {
        return switch (extension) {
            case "zip" -> readZipLikeEntries(filePath);
            case "7z" -> readSevenZEntries(filePath);
            default -> List.of();
        };
    }

    private List<ArchiveEntryInfo> readZipLikeEntries(Path filePath) {
        List<ArchiveEntryInfo> utf8Entries = readZipEntriesWithCharset(filePath, StandardCharsets.UTF_8);
        if (looksLikeGarbledZipNames(utf8Entries)) {
            List<ArchiveEntryInfo> gbkEntries = readZipEntriesWithCharset(filePath, Charset.forName("GBK"));
            if (!gbkEntries.isEmpty()) {
                return sortAndTrimEntries(gbkEntries);
            }
        }
        return sortAndTrimEntries(utf8Entries);
    }

    private List<ArchiveEntryInfo> readZipEntriesWithCharset(Path filePath, Charset charset) {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        try (ZipFile zipFile = ZipFile.builder().setFile(filePath.toFile()).setCharset(charset).get()) {
            zipFile.getEntries().asIterator().forEachRemaining(entry ->
                addArchiveEntry(entries, entry.getName(), entry.isDirectory(), entry.getSize())
            );
        } catch (Exception e) {
            log.warn("使用字符集 {} 读取压缩包目录失败: {}", charset, filePath, e);
        }
        return entries;
    }

    private boolean looksLikeGarbledZipNames(List<ArchiveEntryInfo> entries) {
        if (entries.isEmpty()) {
            return false;
        }
        long suspicious = entries.stream()
            .map(ArchiveEntryInfo::name)
            .filter(name -> name.contains("?") || name.contains("�"))
            .count();
        return suspicious > 0;
    }

    private List<ArchiveEntryInfo> readSevenZEntries(Path filePath) {
        List<ArchiveEntryInfo> entries = new ArrayList<>();
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(filePath.toFile()).get()) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                addArchiveEntry(entries, entry.getName(), entry.isDirectory(), entry.getSize());
            }
        } catch (Exception e) {
            log.warn("读取 7Z 目录失败: {}", filePath, e);
        }
        return sortAndTrimEntries(entries);
    }

    private void addArchiveEntry(List<ArchiveEntryInfo> entries, String path, boolean directory, long size) {
        if (path == null || path.isBlank() || entries.size() >= MAX_ARCHIVE_ENTRIES) {
            return;
        }
        String normalizedPath = path.replace('\\', '/');
        int depth = normalizedPath.split("/").length;
        if (depth > MAX_ARCHIVE_DEPTH || normalizedPath.contains("../")) {
            return;
        }
        String name = normalizedPath.contains("/")
            ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
            : normalizedPath;
        String extension = directory || !name.contains(".")
            ? ""
            : name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        entries.add(new ArchiveEntryInfo(normalizedPath, name, directory, Math.max(size, 0), extension));
    }

    private List<ArchiveEntryInfo> sortAndTrimEntries(List<ArchiveEntryInfo> entries) {
        return entries.stream()
            .sorted(Comparator.comparing(ArchiveEntryInfo::path))
            .limit(MAX_ARCHIVE_ENTRIES)
            .toList();
    }

    private List<ArchiveTreeNode> buildArchiveTree(List<ArchiveEntryInfo> entries) {
        Map<String, ArchiveTreeNode> nodeMap = new HashMap<>();
        List<ArchiveTreeNode> roots = new ArrayList<>();

        for (ArchiveEntryInfo entry : entries) {
            String[] segments = entry.path().split("/");
            String currentPath = "";
            ArchiveTreeNode parent = null;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                if (segment == null || segment.isBlank()) {
                    continue;
                }
                currentPath = currentPath.isEmpty() ? segment : currentPath + "/" + segment;
                boolean isLeaf = i == segments.length - 1;
                boolean directory = isLeaf ? entry.directory() : true;
                ArchiveTreeNode node = nodeMap.computeIfAbsent(
                    currentPath,
                    key -> new ArchiveTreeNode(segment, key, directory, isLeaf ? entry.size() : 0, isLeaf ? entry.extension() : "", new ArrayList<>())
                );

                if (parent == null) {
                    if (roots.stream().noneMatch(root -> root.path().equals(node.path()))) {
                        roots.add(node);
                    }
                } else if (parent.children().stream().noneMatch(child -> child.path().equals(node.path()))) {
                    parent.children().add(node);
                }
                parent = node;
            }
        }

        sortArchiveTree(roots);
        return roots;
    }

    private void sortArchiveTree(List<ArchiveTreeNode> nodes) {
        nodes.sort((a, b) -> {
            if (a.directory() != b.directory()) {
                return a.directory() ? -1 : 1;
            }
            return a.name().compareToIgnoreCase(b.name());
        });
        nodes.forEach(node -> sortArchiveTree(node.children()));
    }

    private String resolveArchiveEntryPreviewType(String extension) {
        if (extension == null || extension.isBlank()) {
            return "NOT_SUPPORTED";
        }
        String normalized = extension.toLowerCase();
        if (ARCHIVE_PREVIEWABLE_TEXT_EXTENSIONS.contains(normalized)) {
            return "TEXT";
        }
        if (ARCHIVE_PREVIEWABLE_IMAGE_EXTENSIONS.contains(normalized)) {
            return "IMAGE";
        }
        if ("pdf".equals(normalized)) {
            return "PDF";
        }
        if (OFFICE_PDF_EXTENSIONS.contains(normalized)) {
            return "OFFICE";
        }
        return "NOT_SUPPORTED";
    }

    private String normalizeArchiveEntryPath(String entryPath) {
        if (entryPath == null || entryPath.isBlank()) {
            throw new IllegalArgumentException("压缩包条目路径不能为空");
        }
        String normalized = entryPath.replace('\\', '/').trim();
        if (normalized.startsWith("/") || normalized.contains("../")) {
            throw new IllegalArgumentException("非法的压缩包条目路径");
        }
        return normalized;
    }

    private ArchiveEntryInfo findArchiveEntry(Path filePath, String extension, String entryPath) {
        return readArchiveEntries(filePath, extension).stream()
            .filter(entry -> !entry.directory())
            .filter(entry -> entry.path().equals(entryPath))
            .findFirst()
            .orElse(null);
    }

    private TextPreviewResult readArchiveEntryTextPreview(Path filePath, String archiveExtension, String entryPath) throws IOException {
        byte[] bytes = extractArchiveEntryBytes(filePath, archiveExtension, entryPath, maxTextPreviewSize + 1);
        if (bytes == null) {
            throw new FileNotFoundException("压缩包内文件不存在: " + entryPath);
        }
        boolean truncated = bytes.length > maxTextPreviewSize;
        byte[] previewBytes = truncated ? java.util.Arrays.copyOf(bytes, (int) maxTextPreviewSize) : bytes;
        String encoding = detectEncoding(previewBytes);
        String previewMessage = truncated ? "压缩包内文本内容已按预览上限截断" : "压缩包内文本内容预览完整";
        return new TextPreviewResult(new String(previewBytes, encoding), truncated, encoding, "BOM_OR_UTF8", previewMessage, previewBytes.length);
    }

    private byte[] extractArchiveEntryBytes(Path filePath, String archiveExtension, String entryPath, long maxBytes) throws IOException {
        return switch (archiveExtension) {
            case "zip" -> extractZipEntryBytes(filePath, entryPath, maxBytes);
            case "7z" -> extractSevenZEntryBytes(filePath, entryPath, maxBytes);
            default -> null;
        };
    }

    private byte[] extractZipEntryBytes(Path filePath, String entryPath, long maxBytes) throws IOException {
        List<Charset> charsets = List.of(StandardCharsets.UTF_8, Charset.forName("GBK"));
        for (Charset charset : charsets) {
            try (ZipFile zipFile = ZipFile.builder().setFile(filePath.toFile()).setCharset(charset).get()) {
                var entries = zipFile.getEntries().asIterator();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    String normalized = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
                    if (!entry.isDirectory() && normalized.equals(entryPath)) {
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            return readLimitedBytes(inputStream, maxBytes);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("使用字符集 {} 提取 ZIP 条目失败: {}", charset, entryPath, e);
            }
        }
        return null;
    }

    private byte[] extractSevenZEntryBytes(Path filePath, String entryPath, long maxBytes) throws IOException {
        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(filePath.toFile()).get()) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                String normalized = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
                if (!entry.isDirectory() && normalized.equals(entryPath)) {
                    return readLimitedBytes(sevenZFile.getInputStream(entry), maxBytes);
                }
            }
        }
        return null;
    }

    private byte[] readLimitedBytes(InputStream inputStream, long maxBytes) throws IOException {
        byte[] buffer = inputStream.readNBytes((int) maxBytes);
        return buffer;
    }

    private Path writeArchiveEntryToTempFile(String entryPath, String extension, byte[] bytes) throws IOException {
        Path tempDir = Paths.get(previewCachePath, "archive-entry-temp");
        Files.createDirectories(tempDir);
        String safeName = Integer.toHexString(entryPath.hashCode());
        Path tempFile = tempDir.resolve(safeName + "." + extension.toLowerCase());
        Files.write(tempFile, bytes);
        return tempFile;
    }

    private String resolveImageContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private String detectEncoding(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8.name();
        }
        if (bytes.length >= 2 && ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) || (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF))) {
            return StandardCharsets.UTF_16.name();
        }
        return StandardCharsets.UTF_8.name();
    }

    private Map<String, Object> getImageInfo(Path filePath) {
        Map<String, Object> imageInfo = new HashMap<>();
        try {
            imageInfo.put("type", "image");
            imageInfo.put("size", Files.size(filePath));
        } catch (Exception e) {
            log.warn("获取图片信息失败: {}", filePath, e);
        }
        return imageInfo;
    }

    private Map<String, Object> getPdfInfo(Path filePath) {
        Map<String, Object> pdfInfo = new HashMap<>();
        try {
            pdfInfo.put("type", "pdf");
            pdfInfo.put("size", Files.size(filePath));
        } catch (Exception e) {
            log.warn("获取PDF信息失败: {}", filePath, e);
        }
        return pdfInfo;
    }

    public String generateThumbnail(String relativePath) {
        try {
            Path filePath = fileUploadService.getFilePath(relativePath);
            String extension = getExtension(filePath.getFileName().toString());
            Path thumbnailDir = Paths.get(previewCachePath, "thumbnails");
            Files.createDirectories(thumbnailDir);
            String thumbnailName = filePath.getFileName() + "_thumb.jpg";
            Path thumbnailPath = thumbnailDir.resolve(thumbnailName);
            if (Files.exists(thumbnailPath) && Files.getLastModifiedTime(thumbnailPath).compareTo(Files.getLastModifiedTime(filePath)) > 0) {
                return thumbnailPath.toString();
            }
            boolean generated = false;
            FileTypeRegistryService.PreviewType previewType = fileTypeRegistryService.getPreviewType(extension);
            if (previewType == FileTypeRegistryService.PreviewType.IMAGE) {
                generated = generateImageThumbnail(filePath, thumbnailPath);
            } else if (previewType == FileTypeRegistryService.PreviewType.PDF) {
                generated = generatePdfThumbnail(filePath, thumbnailPath);
            }
            return generated ? thumbnailPath.toString() : null;
        } catch (Exception e) {
            log.error("生成缩略图失败: {}", relativePath, e);
            return null;
        }
    }

    private boolean generateImageThumbnail(Path imagePath, Path thumbnailPath) {
        try {
            Thumbnails.of(imagePath.toFile()).size(200, 200).outputFormat("jpg").toFile(thumbnailPath.toFile());
            return true;
        } catch (Exception e) {
            log.error("生成图片缩略图失败", e);
            return false;
        }
    }

    private boolean generatePdfThumbnail(Path pdfPath, Path thumbnailPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 72);
            ImageIO.write(image, "jpg", thumbnailPath.toFile());
            return true;
        } catch (Exception e) {
            log.error("生成PDF缩略图失败", e);
            return false;
        }
    }

    public void cleanupPreviewCache() {
        try {
            Path cacheDir = Paths.get(previewCachePath);
            if (!Files.exists(cacheDir)) {
                return;
            }
            Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("删除预览缓存失败: {}", path, e);
                    }
                });
        } catch (Exception e) {
            log.error("清理预览缓存失败", e);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private String buildPdfFilename(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        String baseName = lastDot >= 0 ? originalFilename.substring(0, lastDot) : originalFilename;
        return baseName + ".pdf";
    }

    public record PreviewContent(Path path, String extension, String filename) {}

    public record TextPreviewPayload(
        String content,
        String encoding,
        String encodingSource,
        String previewMessage,
        int contentLength,
        boolean truncated,
        String contentSource
    ) {}

    public record ArchiveTreeNode(
        String name,
        String path,
        boolean directory,
        long size,
        String extension,
        List<ArchiveTreeNode> children
    ) {}

    public record ArchivePreviewPayload(
        List<ArchiveTreeNode> archiveTree,
        List<ArchiveEntryInfo> archiveEntries,
        boolean archiveParseSucceeded,
        List<String> archiveWarnings,
        boolean archiveTruncated,
        boolean archiveDepthLimited,
        int archiveEntryCountReturned,
        int archiveEntryCountTotal
    ) {}

    public record ArchiveEntryPreviewInfo(
        boolean previewable,
        String previewType,
        String entryPath,
        String entryName,
        long entrySize,
        String extension,
        String reason,
        boolean truncated,
        String encoding,
        String encodingSource,
        String content
    ) {}

    public record ArchiveEntryContent(
        byte[] bytes,
        String contentType,
        String filename
    ) {}

    private record TextPreviewResult(
        String content,
        boolean truncated,
        String encoding,
        String encodingSource,
        String previewMessage,
        int contentLength
    ) {}

    private record ArchivePreviewResult(
        List<ArchiveEntryInfo> archiveEntries,
        List<ArchiveTreeNode> archiveTree,
        boolean parseSucceeded,
        List<String> warnings,
        boolean truncated,
        boolean depthLimited,
        int returnedCount,
        int totalCount
    ) {}

    public static class DocumentPreviewInfo {
        private String filename;
        private String extension;
        private long fileSize;
        private boolean previewable;
        private PreviewType previewType;
        private String content;
        private boolean truncated;
        private Map<String, Object> imageInfo;
        private Map<String, Object> pdfInfo;
        private List<ArchiveEntryInfo> archiveEntries;
        private List<ArchiveTreeNode> archiveTree;
        private Boolean archiveParseSucceeded;
        private List<String> archiveWarnings;
        private Boolean archiveTruncated;
        private Boolean archiveDepthLimited;
        private Integer archiveEntryCountReturned;
        private Integer archiveEntryCountTotal;
        private String fallbackReason;
        private boolean convertedFromOffice;
        private String encoding;
        private String encodingSource;
        private String previewMessage;
        private Integer contentLength;

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public boolean isPreviewable() { return previewable; }
        public void setPreviewable(boolean previewable) { this.previewable = previewable; }
        public PreviewType getPreviewType() { return previewType; }
        public void setPreviewType(PreviewType previewType) { this.previewType = previewType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public boolean isTruncated() { return truncated; }
        public void setTruncated(boolean truncated) { this.truncated = truncated; }
        public Map<String, Object> getImageInfo() { return imageInfo; }
        public void setImageInfo(Map<String, Object> imageInfo) { this.imageInfo = imageInfo; }
        public Map<String, Object> getPdfInfo() { return pdfInfo; }
        public void setPdfInfo(Map<String, Object> pdfInfo) { this.pdfInfo = pdfInfo; }
        public List<ArchiveEntryInfo> getArchiveEntries() { return archiveEntries; }
        public void setArchiveEntries(List<ArchiveEntryInfo> archiveEntries) { this.archiveEntries = archiveEntries; }
        public List<ArchiveTreeNode> getArchiveTree() { return archiveTree; }
        public void setArchiveTree(List<ArchiveTreeNode> archiveTree) { this.archiveTree = archiveTree; }
        public Boolean getArchiveParseSucceeded() { return archiveParseSucceeded; }
        public void setArchiveParseSucceeded(Boolean archiveParseSucceeded) { this.archiveParseSucceeded = archiveParseSucceeded; }
        public List<String> getArchiveWarnings() { return archiveWarnings; }
        public void setArchiveWarnings(List<String> archiveWarnings) { this.archiveWarnings = archiveWarnings; }
        public Boolean getArchiveTruncated() { return archiveTruncated; }
        public void setArchiveTruncated(Boolean archiveTruncated) { this.archiveTruncated = archiveTruncated; }
        public Boolean getArchiveDepthLimited() { return archiveDepthLimited; }
        public void setArchiveDepthLimited(Boolean archiveDepthLimited) { this.archiveDepthLimited = archiveDepthLimited; }
        public Integer getArchiveEntryCountReturned() { return archiveEntryCountReturned; }
        public void setArchiveEntryCountReturned(Integer archiveEntryCountReturned) { this.archiveEntryCountReturned = archiveEntryCountReturned; }
        public Integer getArchiveEntryCountTotal() { return archiveEntryCountTotal; }
        public void setArchiveEntryCountTotal(Integer archiveEntryCountTotal) { this.archiveEntryCountTotal = archiveEntryCountTotal; }
        public String getFallbackReason() { return fallbackReason; }
        public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
        public boolean isConvertedFromOffice() { return convertedFromOffice; }
        public void setConvertedFromOffice(boolean convertedFromOffice) { this.convertedFromOffice = convertedFromOffice; }
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
        public String getEncodingSource() { return encodingSource; }
        public void setEncodingSource(String encodingSource) { this.encodingSource = encodingSource; }
        public String getPreviewMessage() { return previewMessage; }
        public void setPreviewMessage(String previewMessage) { this.previewMessage = previewMessage; }
        public Integer getContentLength() { return contentLength; }
        public void setContentLength(Integer contentLength) { this.contentLength = contentLength; }
    }

    public record ArchiveEntryInfo(String path, String name, boolean directory, long size, String extension) {}

    public enum PreviewType {
        TEXT("文本"),
        IMAGE("图片"),
        PDF("PDF"),
        OFFICE_TEXT("Office内容"),
        ARCHIVE_TREE("压缩包目录"),
        NOT_SUPPORTED("不支持");

        private final String description;

        PreviewType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
