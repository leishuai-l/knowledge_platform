package com.zhixiang.knowledge_platform.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一维护平台文件类型与能力矩阵。
 */
@Service
public class FileTypeRegistryService {

    public static final String SEARCH_GROUP_PDF = "pdf";
    public static final String SEARCH_GROUP_WORD = "word";
    public static final String SEARCH_GROUP_POWERPOINT = "powerpoint";
    public static final String SEARCH_GROUP_EXCEL = "excel";
    public static final String SEARCH_GROUP_IMAGE = "image";
    public static final String SEARCH_GROUP_TEXT = "text";
    public static final String SEARCH_GROUP_ARCHIVE = "archive";

    private final Map<String, FileTypeDefinition> definitionsByCode;
    private final Map<String, FileTypeDefinition> definitionsByExtension;

    public FileTypeRegistryService() {
        List<FileTypeDefinition> definitions = List.of(
            definition("PDF", "PDF", "pdf", true, true, true, PreviewType.PDF, true, true,
                Set.of("pdf"), Set.of("application/pdf"), Set.of("25504446"), "PDF 文档"),
            definition("DOC", "WORD", SEARCH_GROUP_WORD, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("doc"), Set.of("application/msword"), Set.of("D0CF11E0A1B11AE1"), "Word 文档"),
            definition("DOCX", "WORD", SEARCH_GROUP_WORD, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("docx"), Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"), Set.of("504B0304", "504B030414"), "Word 文档"),
            definition("XLS", "EXCEL", SEARCH_GROUP_EXCEL, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("xls"), Set.of("application/vnd.ms-excel"), Set.of("D0CF11E0A1B11AE1"), "Excel 表格"),
            definition("XLSX", "EXCEL", SEARCH_GROUP_EXCEL, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("xlsx"), Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), Set.of("504B0304", "504B030414"), "Excel 表格"),
            definition("PPT", "POWERPOINT", SEARCH_GROUP_POWERPOINT, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("ppt"), Set.of("application/vnd.ms-powerpoint"), Set.of("D0CF11E0A1B11AE1"), "PowerPoint 演示文稿"),
            definition("PPTX", "POWERPOINT", SEARCH_GROUP_POWERPOINT, true, true, true, PreviewType.OFFICE_TEXT, false, true,
                Set.of("pptx"), Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"), Set.of("504B0304", "504B030414"), "PowerPoint 演示文稿"),
            definition("TXT", "TEXT", SEARCH_GROUP_TEXT, true, true, true, PreviewType.TEXT, true, true,
                Set.of("txt", "md", "json", "xml", "csv", "log", "java", "js", "html", "css", "py", "sql"),
                Set.of("text/plain", "text/markdown", "application/json", "application/xml", "text/xml", "text/csv", "text/html", "text/css", "application/sql"),
                Set.of("EFBBBF", "FEFF", "FFFE"), "文本文件"),
            definition("JPG", "IMAGE", SEARCH_GROUP_IMAGE, true, true, true, PreviewType.IMAGE, true, false,
                Set.of("jpg", "jpeg"), Set.of("image/jpeg"), Set.of("FFD8FF"), "JPEG 图片"),
            definition("PNG", "IMAGE", SEARCH_GROUP_IMAGE, true, true, true, PreviewType.IMAGE, true, false,
                Set.of("png"), Set.of("image/png"), Set.of("89504E47"), "PNG 图片"),
            definition("GIF", "IMAGE", SEARCH_GROUP_IMAGE, true, true, true, PreviewType.IMAGE, true, false,
                Set.of("gif"), Set.of("image/gif"), Set.of("47494638"), "GIF 图片"),
            definition("ZIP", "ARCHIVE", SEARCH_GROUP_ARCHIVE, true, true, true, PreviewType.ARCHIVE_TREE, false, false,
                Set.of("zip"), Set.of("application/zip", "application/x-zip-compressed"), Set.of("504B0304", "504B0506", "504B0708"), "ZIP 压缩包"),
            definition("RAR", "ARCHIVE", SEARCH_GROUP_ARCHIVE, true, true, false, PreviewType.NOT_SUPPORTED, false, false,
                Set.of("rar"), Set.of("application/x-rar-compressed", "application/vnd.rar"), Set.of("526172211A0700", "526172211A070100"), "RAR 压缩包"),
            definition("SEVEN_Z", "ARCHIVE", SEARCH_GROUP_ARCHIVE, true, true, true, PreviewType.ARCHIVE_TREE, false, false,
                Set.of("7z"), Set.of("application/x-7z-compressed"), Set.of("377ABCAF271C"), "7Z 压缩包")
        );

        Map<String, FileTypeDefinition> byCode = new LinkedHashMap<>();
        Map<String, FileTypeDefinition> byExtension = new LinkedHashMap<>();
        for (FileTypeDefinition definition : definitions) {
            byCode.put(definition.getCode(), definition);
            for (String extension : definition.getExtensions()) {
                byExtension.put(extension, definition);
            }
        }
        this.definitionsByCode = Collections.unmodifiableMap(byCode);
        this.definitionsByExtension = Collections.unmodifiableMap(byExtension);
    }

    private FileTypeDefinition definition(String code,
                                          String group,
                                          String searchGroup,
                                          boolean uploadSupported,
                                          boolean initialReviewSupported,
                                          boolean previewEntrySupported,
                                          PreviewType previewType,
                                          boolean thumbnailSupported,
                                          boolean aiSupported,
                                          Set<String> extensions,
                                          Set<String> mimeTypes,
                                          Set<String> signatures,
                                          String displayName) {
        return new FileTypeDefinition(
            code,
            group,
            searchGroup,
            displayName,
            new LinkedHashSet<>(normalizeAll(extensions)),
            new LinkedHashSet<>(normalizeAll(mimeTypes)),
            new LinkedHashSet<>(normalizeAll(signatures)),
            uploadSupported,
            initialReviewSupported,
            previewEntrySupported,
            thumbnailSupported,
            aiSupported,
            true,
            previewType
        );
    }

    public Optional<FileTypeDefinition> findByExtension(String extension) {
        return Optional.ofNullable(definitionsByExtension.get(normalize(extension)));
    }

    public Optional<FileTypeDefinition> findByFilename(String filename) {
        return findByExtension(getFileExtension(filename));
    }

    public Optional<FileTypeDefinition> findByCode(String code) {
        return Optional.ofNullable(definitionsByCode.get(normalize(code).toUpperCase(Locale.ROOT)));
    }

    public Set<String> getSupportedUploadExtensions() {
        return definitionsByCode.values().stream()
            .filter(FileTypeDefinition::isUploadSupported)
            .flatMap(definition -> definition.getExtensions().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getSupportedUploadMimeTypes() {
        return definitionsByCode.values().stream()
            .filter(FileTypeDefinition::isUploadSupported)
            .flatMap(definition -> definition.getMimeTypes().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getAiSupportedExtensions() {
        return definitionsByCode.values().stream()
            .filter(FileTypeDefinition::isAiSupported)
            .flatMap(definition -> definition.getExtensions().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isSupportedUploadExtension(String extension) {
        return findByExtension(extension).map(FileTypeDefinition::isUploadSupported).orElse(false);
    }

    public boolean isAiSupportedExtension(String extension) {
        return findByExtension(extension).map(FileTypeDefinition::isAiSupported).orElse(false);
    }

    public boolean isInitialReviewSupported(String extension) {
        return findByExtension(extension).map(FileTypeDefinition::isInitialReviewSupported).orElse(false);
    }

    public boolean isPreviewEntrySupported(String extension) {
        return findByExtension(extension).map(FileTypeDefinition::isPreviewEntrySupported).orElse(false);
    }

    public boolean isInlinePreviewSupported(String extension) {
        return findByExtension(extension)
            .map(definition -> definition.getPreviewType().isInline())
            .orElse(false);
    }

    public boolean isThumbnailSupported(String extension) {
        return findByExtension(extension).map(FileTypeDefinition::isThumbnailSupported).orElse(false);
    }

    public PreviewType getPreviewType(String extension) {
        return findByExtension(extension)
            .map(FileTypeDefinition::getPreviewType)
            .orElse(PreviewType.NOT_SUPPORTED);
    }

    public Set<String> getFileSignaturesByMimeType(String mimeType) {
        return definitionsByCode.values().stream()
            .filter(definition -> definition.getMimeTypes().contains(normalize(mimeType)))
            .findFirst()
            .map(FileTypeDefinition::getSignatures)
            .orElse(Collections.emptySet());
    }

    public boolean isAllowedMimeType(String mimeType) {
        return definitionsByCode.values().stream()
            .anyMatch(definition -> definition.getMimeTypes().contains(normalize(mimeType)));
    }

    public boolean isMimeTypeAllowedForExtension(String extension, String mimeType) {
        return findByExtension(extension)
            .map(definition -> definition.getMimeTypes().contains(normalize(mimeType)))
            .orElse(false);
    }

    public List<String> getExtensionsForSearchGroup(String searchGroup) {
        String normalizedGroup = normalize(searchGroup);
        return definitionsByCode.values().stream()
            .filter(FileTypeDefinition::isSearchSupported)
            .filter(definition -> normalizedGroup.equals(definition.getSearchGroup()))
            .flatMap(definition -> definition.getExtensions().stream())
            .distinct()
            .toList();
    }

    public String buildUploadAccept() {
        return getSupportedUploadExtensions().stream()
            .map(extension -> "." + extension)
            .collect(Collectors.joining(","));
    }

    public CapabilityResponse buildCapabilityResponse(long maxFileSize) {
        List<FileTypeCapabilityItem> items = definitionsByCode.values().stream()
            .map(definition -> new FileTypeCapabilityItem(
                definition.getCode(),
                definition.getGroup(),
                definition.getSearchGroup(),
                definition.getDisplayName(),
                new ArrayList<>(definition.getExtensions()),
                new ArrayList<>(definition.getMimeTypes()),
                definition.isUploadSupported(),
                definition.isInitialReviewSupported(),
                definition.isPreviewEntrySupported(),
                definition.getPreviewType().name(),
                definition.getPreviewType().isInline(),
                definition.getPreviewType().isDownloadOnly(),
                definition.isThumbnailSupported(),
                definition.isAiSupported(),
                definition.isSearchSupported(),
                buildNotes(definition)
            ))
            .toList();

        List<SearchGroupItem> searchGroups = List.of(
            searchGroup(SEARCH_GROUP_PDF, "PDF"),
            searchGroup(SEARCH_GROUP_WORD, "Word"),
            searchGroup(SEARCH_GROUP_POWERPOINT, "PowerPoint"),
            searchGroup(SEARCH_GROUP_EXCEL, "Excel"),
            searchGroup(SEARCH_GROUP_IMAGE, "图片"),
            searchGroup(SEARCH_GROUP_TEXT, "文本文件"),
            searchGroup(SEARCH_GROUP_ARCHIVE, "压缩包")
        );

        return new CapabilityResponse(
            items,
            new ArrayList<>(getSupportedUploadExtensions()),
            new ArrayList<>(getAiSupportedExtensions()),
            buildUploadAccept(),
            maxFileSize,
            searchGroups,
            "支持 PDF、Word、PowerPoint、Excel、TXT、图片、压缩包，单个文件不超过 100MB"
        );
    }

    private SearchGroupItem searchGroup(String value, String label) {
        return new SearchGroupItem(value, label, getExtensionsForSearchGroup(value));
    }

    private String buildNotes(FileTypeDefinition definition) {
        return switch (definition.getPreviewType()) {
            case PDF, IMAGE, TEXT -> "支持在线预览";
            case OFFICE_TEXT -> "支持内容预览，当前展示抽取文本内容";
            case ARCHIVE_TREE -> "支持目录预览，当前展示压缩包内部条目";
            case NOT_SUPPORTED -> "暂不支持预览";
        };
    }

    private Set<String> normalizeAll(Collection<String> values) {
        return values.stream()
            .filter(Objects::nonNull)
            .map(this::normalize)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return normalize(filename.substring(filename.lastIndexOf('.') + 1));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public enum PreviewType {
        PDF,
        IMAGE,
        TEXT,
        OFFICE_TEXT,
        ARCHIVE_TREE,
        NOT_SUPPORTED;

        public boolean isInline() {
            return this == PDF || this == IMAGE || this == TEXT;
        }

        public boolean isDownloadOnly() {
            return false;
        }
    }

    public static class FileTypeDefinition {
        private final String code;
        private final String group;
        private final String searchGroup;
        private final String displayName;
        private final Set<String> extensions;
        private final Set<String> mimeTypes;
        private final Set<String> signatures;
        private final boolean uploadSupported;
        private final boolean initialReviewSupported;
        private final boolean previewEntrySupported;
        private final boolean thumbnailSupported;
        private final boolean aiSupported;
        private final boolean searchSupported;
        private final PreviewType previewType;

        public FileTypeDefinition(String code, String group, String searchGroup, String displayName,
                                  Set<String> extensions, Set<String> mimeTypes, Set<String> signatures,
                                  boolean uploadSupported, boolean initialReviewSupported,
                                  boolean previewEntrySupported, boolean thumbnailSupported,
                                  boolean aiSupported, boolean searchSupported, PreviewType previewType) {
            this.code = code;
            this.group = group;
            this.searchGroup = searchGroup;
            this.displayName = displayName;
            this.extensions = extensions;
            this.mimeTypes = mimeTypes;
            this.signatures = signatures;
            this.uploadSupported = uploadSupported;
            this.initialReviewSupported = initialReviewSupported;
            this.previewEntrySupported = previewEntrySupported;
            this.thumbnailSupported = thumbnailSupported;
            this.aiSupported = aiSupported;
            this.searchSupported = searchSupported;
            this.previewType = previewType;
        }

        public String getCode() { return code; }
        public String getGroup() { return group; }
        public String getSearchGroup() { return searchGroup; }
        public String getDisplayName() { return displayName; }
        public Set<String> getExtensions() { return extensions; }
        public Set<String> getMimeTypes() { return mimeTypes; }
        public Set<String> getSignatures() { return signatures; }
        public boolean isUploadSupported() { return uploadSupported; }
        public boolean isInitialReviewSupported() { return initialReviewSupported; }
        public boolean isPreviewEntrySupported() { return previewEntrySupported; }
        public boolean isThumbnailSupported() { return thumbnailSupported; }
        public boolean isAiSupported() { return aiSupported; }
        public boolean isSearchSupported() { return searchSupported; }
        public PreviewType getPreviewType() { return previewType; }
    }

    public record FileTypeCapabilityItem(
        String code,
        String group,
        String searchGroup,
        String displayName,
        List<String> extensions,
        List<String> mimeTypes,
        boolean uploadSupported,
        boolean initialReviewSupported,
        boolean previewEntrySupported,
        String previewType,
        boolean inlinePreviewSupported,
        boolean downloadOnlyPreview,
        boolean thumbnailSupported,
        boolean aiSupported,
        boolean searchSupported,
        String notes
    ) {}

    public record SearchGroupItem(String value, String label, List<String> extensions) {}

    public record CapabilityResponse(
        List<FileTypeCapabilityItem> types,
        List<String> uploadExtensions,
        List<String> aiSupportedExtensions,
        String uploadAccept,
        long maxFileSize,
        List<SearchGroupItem> searchGroups,
        String uploadTipText
    ) {}
}
