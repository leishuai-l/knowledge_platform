package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.DocumentSearchRequest;
import com.zhixiang.knowledge_platform.dto.request.DocumentUploadRequest;
import com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import com.zhixiang.knowledge_platform.entity.Tag;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.NotificationType;
import com.zhixiang.knowledge_platform.repository.CategoryRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DownloadRecordRepository;
import com.zhixiang.knowledge_platform.repository.TagRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文档管理服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final DownloadRecordRepository downloadRecordRepository;
    private final PointsService pointsService;
    private final NotificationService notificationService;
    private final DownloadRecordService downloadRecordService;
    private final FileTypeRegistryService fileTypeRegistryService;

    /**
     * 创建文档记录
     */
    @Transactional
    public Document createDocument(DocumentUploadRequest request, Long uploaderId) {
        throw new IllegalStateException("请使用 createDocumentWithFileInfo 方法保存包含文件信息的文档");
    }
    

    /**
     * 创建包含文件信息的文档记录
     */
    @Transactional
    public Document createDocumentWithFileInfo(DocumentUploadRequest request, Long uploaderId,
                                             String fileName, String filePath, Long fileSize,
                                             String fileType, String fileExtension, String md5) {
        log.info("创建带文件信息的文档记录，上传者ID: {}, 文档标题: {}, MD5: {}", uploaderId, request.getTitle(), md5);

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setCategoryId(request.getCategoryId());
        document.setUploaderId(uploaderId);
        document.setDownloadPoints(request.getDownloadPoints() != null ? request.getDownloadPoints() : 0);
        document.setStatus(DocumentStatus.PENDING);

        // 设置文件信息
        document.setFileName(fileName);
        document.setFilePath(filePath);
        document.setFileSize(fileSize);
        document.setFileType(fileType);
        document.setFileExtension(fileExtension);
        document.setMd5(md5);

        Document savedDocument = documentRepository.save(document);
        log.info("带文件信息的文档记录创建成功，ID: {}", savedDocument.getId());
        return savedDocument;
    }
    

    /**
     * 更新文档文件信息
     */
    @Transactional
    public void updateDocumentFileInfo(Long documentId, String fileName, String filePath,
                                     Long fileSize, String fileType, String fileExtension) {
        Document document = getDocumentById(documentId);
        document.setFileName(fileName);
        document.setFilePath(filePath);
        document.setFileSize(fileSize);
        document.setFileType(fileType);
        document.setFileExtension(fileExtension);

        documentRepository.save(document);
        log.info("文档文件信息更新成功，文档ID: {}, 文件名: {}", documentId, fileName);
    }
    

    /**
     * 为文档添加标签
     */
    @Transactional
    public void addTagsToDocument(Long documentId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        Document document = getDocumentById(documentId);
        List<Tag> tags = new ArrayList<>();

        for (String tagName : tagNames) {
            // Trim whitespace and ignore empty strings
            String cleanedTagName = tagName.trim();
            if (cleanedTagName.isEmpty()) {
                continue;
            }
            
            Tag tag = tagRepository.findByName(cleanedTagName)
                    .orElseGet(() -> createNewTag(cleanedTagName));
            tags.add(tag);
        }

        document.setTags(tags);
        documentRepository.save(document);

        // 更新标签使用次数
        tags.forEach(tag -> {
            tag.incrementUsage();
            tagRepository.save(tag);
        });

        log.info("为文档 {} 添加了 {} 个标签", documentId, tags.size());
    }

    /**
     * 创建新标签
     */
    private Tag createNewTag(String tagName) {
        Tag tag = new Tag();
        tag.setName(tagName);
        tag.setColor("#409EFF"); // 默认颜色
        // usageCount is already initialized to 0 in the entity
        return tagRepository.save(tag);
    }

    /**
     * 根据ID获取文档
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在，ID: " + id));
    }

    /**
     * 获取文档详细信息
     */
    public DocumentInfoResponse getDocumentInfo(Long id) {
        Document document = getDocumentById(id);

        // 如果文档已被删除，抛出异常
        if (DocumentStatus.DELETED.equals(document.getStatus())) {
            throw new RuntimeException("文档不存在或已被删除");
        }

        // 允许获取未审核文档的详情（为了预览和审核）
        // 如果需要限制未审核文档的访问，可以在Controller层控制

        return convertToDocumentInfoResponse(document);
    }

    /**
     * 检查用户是否可以访问文档
     */
    public boolean canUserAccessDocument(Document document, Long userId, String userRole) {
        boolean isAdmin = userRole != null && userRole.equalsIgnoreCase("ADMIN");

        if (document.getStatus() != DocumentStatus.APPROVED) {
            if (isAdmin) {
                return true;
            }
            return userId != null
                    && document.getUploaderId() != null
                    && document.getUploaderId().equals(userId);
        }

        return true;
    }

    /**
     * 分页查询已审核的文档
     */
    public PageResponse<DocumentInfoResponse> getApprovedDocuments(int page, int size, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Document> documentPage = documentRepository.findByStatus(DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 根据分类查询文档
     */
    public PageResponse<DocumentInfoResponse> getDocumentsByCategory(Long categoryId, int page, int size, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Document> documentPage = documentRepository.findByCategoryIdAndStatus(
                categoryId, DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 搜索文档
     */
    public PageResponse<DocumentInfoResponse> searchDocuments(DocumentSearchRequest request) {
        Sort sort = createSort(request.getSortBy(), request.getSortDirection());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 处理分类ID - 获取包含子分类的所有分类ID
        List<Long> categoryIds = null;
        if (request.getCategoryId() != null) {
            categoryIds = categoryRepository.findCategoryAndDescendantIds(request.getCategoryId());
            log.info("分类 {} 及其子分类: {}", request.getCategoryId(), categoryIds);
        }

        List<String> fileExtensions = resolveSearchExtensions(request.getFileType());
        log.info("文件类型 {} 转换为扩展名集合: {}", request.getFileType(), fileExtensions);

        Page<Document> documentPage;
        if (fileExtensions == null || fileExtensions.isEmpty()) {
            documentPage = documentRepository.searchDocumentsAdvanced(
                    request.getKeyword(),
                    categoryIds,
                    request.getMinRating(),
                    DocumentStatus.APPROVED,
                    pageable
            );
        } else {
            documentPage = documentRepository.searchDocumentsAdvancedWithExtensions(
                    request.getKeyword(),
                    categoryIds,
                    fileExtensions,
                    request.getMinRating(),
                    DocumentStatus.APPROVED,
                    pageable
            );
        }

        return convertToPageResponse(documentPage);
    }

    private List<String> resolveSearchExtensions(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return null;
        }

        List<String> extensions = fileTypeRegistryService.getExtensionsForSearchGroup(fileType);
        if (!extensions.isEmpty()) {
            return extensions;
        }

        return List.of(fileType.toLowerCase());
    }

    /**
     * 根据标签查询文档
     */
    public PageResponse<DocumentInfoResponse> getDocumentsByTags(List<Long> tagIds, int page, int size, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Document> documentPage = documentRepository.findDocumentsByTags(
                tagIds, DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 获取热门文档
     */
    public PageResponse<DocumentInfoResponse> getPopularDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage = documentRepository.findPopularDocuments(DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 获取最新文档
     */
    public PageResponse<DocumentInfoResponse> getLatestDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage = documentRepository.findLatestDocuments(DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 获取高评分文档
     */
    public PageResponse<DocumentInfoResponse> getTopRatedDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage = documentRepository.findTopRatedDocuments(DocumentStatus.APPROVED, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 查询用户上传的文档
     */
    public PageResponse<DocumentInfoResponse> getUserDocuments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage = documentRepository.findByUploaderId(userId, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 查询用户上传的指定状态文档
     */
    public PageResponse<DocumentInfoResponse> getUserDocumentsByStatus(Long userId, DocumentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage = documentRepository.findByUploaderIdAndStatus(userId, status, pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 管理员：获取待审核文档
     */
    public PageResponse<DocumentInfoResponse> getPendingDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentPage = documentRepository.findPendingDocuments(pageable);
        return convertToPageResponse(documentPage);
    }

    /**
     * 管理员：审核通过文档
     */
    @Transactional
    public void approveDocument(Long documentId, Long approverId) {
        Document document = getDocumentById(documentId);

        if (!document.isPending()) {
            throw new RuntimeException("只能审核待审核状态的文档");
        }

        document.approve(approverId);
        documentRepository.save(document);

        // 奖励文档上传者审核通过积分
        try {
            pointsService.rewardApprovalPoints(document.getUploaderId(), documentId);
        } catch (Exception e) {
            log.warn("审核通过积分奖励失败，上传者ID: {}, 文档ID: {}", document.getUploaderId(), documentId, e);
            // 不影响审核流程，只记录警告日志
        }

        // 发送审核通过通知
        try {
            String title = "文档审核通过";
            String content = String.format("您上传的文档《%s》已通过审核，可以开始下载了！", document.getTitle());
            notificationService.createAndSendNotification(
                document.getUploaderId(),
                NotificationType.DOCUMENT_APPROVED,
                title,
                content,
                documentId
            );
        } catch (Exception e) {
            log.warn("发送审核通过通知失败，上传者ID: {}, 文档ID: {}", document.getUploaderId(), documentId, e);
        }

        log.info("文档审核通过，文档ID: {}, 审核人ID: {}", documentId, approverId);
    }

    /**
     * 管理员：拒绝文档
     */
    @Transactional
    public void rejectDocument(Long documentId, String reason) {
        Document document = getDocumentById(documentId);

        if (!document.isPending()) {
            throw new RuntimeException("只能审核待审核状态的文档");
        }

        document.reject(reason);
        documentRepository.save(document);

        // 发送审核拒绝通知
        try {
            String title = "文档审核被拒";
            String content = String.format("您上传的文档《%s》审核未通过。原因：%s", document.getTitle(), reason);
            notificationService.createAndSendNotification(
                document.getUploaderId(),
                NotificationType.DOCUMENT_REJECTED,
                title,
                content,
                documentId
            );
        } catch (Exception e) {
            log.warn("发送审核拒绝通知失败，上传者ID: {}, 文档ID: {}", document.getUploaderId(), documentId, e);
        }

        log.info("文档审核拒绝，文档ID: {}, 拒绝原因: {}", documentId, reason);
    }

    /**
     * 记录文档下载
     */
    @Transactional
    public boolean recordDownload(Long documentId, Long downloaderId) {
        Document document = getDocumentById(documentId);

        if (!document.isApproved()) {
            throw new RuntimeException("只能下载已审核通过的文档");
        }

        // 消费下载者的积分
        boolean consumeSuccess = pointsService.consumeDownloadPoints(
                downloaderId, documentId, document.getDownloadPoints());

        if (!consumeSuccess) {
            log.warn("积分不足，下载失败，用户ID: {}, 文档ID: {}, 需要积分: {}",
                    downloaderId, documentId, document.getDownloadPoints());
            return false;
        }

        document.incrementDownloadCount();
        documentRepository.save(document);

        // 创建下载记录（失败时抛出异常，确保数据一致性）
        try {
            downloadRecordService.recordDownload(downloaderId, documentId, document.getDownloadPoints());
            log.info("下载记录创建成功，文档ID: {}, 下载者ID: {}", documentId, downloaderId);
        } catch (Exception e) {
            log.error("创建下载记录失败，文档ID: {}, 下载者ID: {}, 错误: {}", documentId, downloaderId, e.getMessage(), e);
            throw new RuntimeException("下载记录创建失败，请重试", e);
        }

        // 奖励文档上传者下载积分（失败时抛出异常）
        try {
            pointsService.rewardDownloadPoints(document.getUploaderId(), documentId);
        } catch (Exception e) {
            log.error("下载奖励积分失败，上传者ID: {}, 文档ID: {}, 错误: {}", document.getUploaderId(), documentId, e.getMessage(), e);
            throw new RuntimeException("上传者奖励积分失败，请重试", e);
        }

        log.info("文档下载成功，文档ID: {}, 下载者ID: {}, 当前下载次数: {}",
                documentId, downloaderId, document.getDownloadCount());
        return true;
    }

    /**
     * 删除文档（软删除，更新状态为拒绝）
     */
    @Transactional
    public void deleteDocument(Long documentId, Long operatorId) {
        Document document = getDocumentById(documentId);

        // 检查权限：只有上传者或管理员可以删除
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));

        if (!document.getUploaderId().equals(operatorId) && !operator.isAdmin()) {
            throw new RuntimeException("没有权限删除此文档");
        }

        document.delete("文档已删除");
        documentRepository.save(document);

        log.info("文档删除成功，文档ID: {}, 操作员ID: {}", documentId, operatorId);
    }

    /**
     * 创建排序对象
     */
    private Sort createSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy.toLowerCase()) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "downloadCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAverage", "ratingCount");
            case "size" -> Sort.by(Sort.Direction.ASC, "fileSize");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * 创建排序对象（支持自定义排序方向）
     */
    private Sort createSort(String sortBy, String sortDirection) {
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (StringUtils.hasText(sortDirection) && "asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        String actualSortBy = switch (sortBy.toLowerCase()) {
            case "createdat" -> "createdAt";
            case "downloadcount" -> "downloadCount";
            case "ratingaverage" -> "ratingAverage";
            case "title" -> "title";
            case "filesize" -> "fileSize";
            default -> "createdAt";
        };

        return Sort.by(direction, actualSortBy);
    }

    /**
     * 转换为分页响应
     */
    private PageResponse<DocumentInfoResponse> convertToPageResponse(Page<Document> documentPage) {
        List<DocumentInfoResponse> documentList = documentPage.getContent().stream()
                .map(this::convertToDocumentInfoResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(documentPage, documentList);
    }

    /**
     * 转换为文档信息响应
     */
    private DocumentInfoResponse convertToDocumentInfoResponse(Document document) {
        return DocumentInfoResponse.fromEntity(document);
    }

    /**
     * 获取文档统计信息
     */
    public List<Object[]> getDocumentStatsByStatus() {
        return documentRepository.countDocumentsByStatus();
    }

    /**
     * 获取分类文档统计
     */
    public List<Object[]> getDocumentStatsByCategory() {
        return documentRepository.countDocumentsByCategory(DocumentStatus.APPROVED);
    }

    /**
     * 获取总文件大小
     */
    public Long getTotalFileSize() {
        return documentRepository.getTotalFileSizeByStatus(DocumentStatus.APPROVED);
    }

    /**
     * 获取平台统计数据
     */
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();

        // 文档总数 (只统计已审核的文档)
        long documentCount = documentRepository.countByStatus(DocumentStatus.APPROVED);
        stats.put("documentCount", documentCount);

        // 用户总数
        long userCount = userRepository.count();
        stats.put("userCount", userCount);

        // 下载总次数
        long downloadCount = documentRepository.getTotalDownloadCount();
        stats.put("downloadCount", downloadCount);

        // 平均评分
        Double averageRating = documentRepository.getAverageRating();
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);

        return stats;
    }

    /**
     * 更新文档信息
     */
    @Transactional
    public Document updateDocument(Long documentId, String title, String description,
                                 Long categoryId, List<String> tagNames, Integer downloadPoints, Long operatorId) {
        Document document = getDocumentById(documentId);

        // 检查权限：只有上传者或管理员可以编辑
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));

        if (!document.getUploaderId().equals(operatorId) && !operator.isAdmin()) {
            throw new RuntimeException("没有权限编辑此文档");
        }

        // 更新基本信息
        if (title != null && !title.trim().isEmpty()) {
            document.setTitle(title);
        }
        if (description != null) {
            document.setDescription(description);
        }
        if (categoryId != null) {
            document.setCategoryId(categoryId);
        }
        if (downloadPoints != null && downloadPoints >= 0) {
            document.setDownloadPoints(downloadPoints);
        }

        document.setUpdatedAt(LocalDateTime.now());
        Document savedDocument = documentRepository.save(document);

        // 更新标签
        if (tagNames != null) {
            addTagsToDocument(documentId, tagNames);
        }

        log.info("文档信息更新成功，文档ID: {}, 操作员ID: {}", documentId, operatorId);
        return savedDocument;
    }

    /**
     * 增加文档浏览次数
     */
    @Transactional
    public void incrementViewCount(Long documentId) {
        Document document = getDocumentById(documentId);
        document.incrementViewCount();
        documentRepository.save(document);
        log.debug("文档浏览次数+1，文档ID: {}, 当前浏览次数: {}", documentId, document.getViewCount());
    }

    /**
     * 检查用户是否可以下载文档
     */
    public boolean canUserDownloadDocument(Long documentId, Long userId) {
        Document document = getDocumentById(documentId);

        // 检查文档状态
        if (!document.isApproved()) {
            return false;
        }

        // 检查用户积分是否足够
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return user.hasEnoughPoints(document.getDownloadPoints());
    }

    /**
     * 获取文档推荐列表（基于分类和标签的相似性）
     */
    public List<DocumentInfoResponse> getRecommendedDocuments(Long documentId, int limit) {
        Document sourceDocument = getDocumentById(documentId);
        
        List<Long> tagIds = sourceDocument.getTags().stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
                
        // 防止空列表导致SQL报错
        if (tagIds.isEmpty()) {
            tagIds.add(-1L);
        }

        // 使用基于分类和标签的混合推荐
        List<Document> recommendedDocs = documentRepository.findSimilarDocuments(
                documentId,
                sourceDocument.getCategoryId(),
                tagIds,
                DocumentStatus.APPROVED,
                PageRequest.of(0, limit));

        return recommendedDocs.stream()
                .map(this::convertToDocumentInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户下载历史
     */
    public PageResponse<DocumentInfoResponse> getUserDownloadHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadTime"));
        Page<DownloadRecord> recordPage = downloadRecordRepository
                .findByUserIdWithDocumentOrderByDownloadTimeDesc(userId, pageable);

        List<DocumentInfoResponse> documents = recordPage.getContent().stream()
                .map(DownloadRecord::getDocument)
                .filter(Objects::nonNull)
                .map(this::convertToDocumentInfoResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(recordPage, documents);
    }

    /**
     * 批量更新文档状态（管理员功能）
     */
    @Transactional
    public void batchUpdateDocumentStatus(List<Long> documentIds, DocumentStatus status, Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("操作员不存在"));

        if (!operator.isAdmin()) {
            throw new RuntimeException("只有管理员可以批量操作");
        }

        List<Document> documents = documentRepository.findAllById(documentIds);
        documents.forEach(document -> {
            document.setStatus(status);
            document.setUpdatedAt(LocalDateTime.now());
        });

        documentRepository.saveAll(documents);
        log.info("批量更新文档状态完成，操作员ID: {}, 文档数量: {}, 新状态: {}",
                operatorId, documents.size(), status);
    }

    /**
     * 获取文档的详细统计信息
     */
    public Map<String, Object> getDocumentDetailStats(Long documentId) {
        Document document = getDocumentById(documentId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("viewCount", document.getViewCount());
        stats.put("downloadCount", document.getDownloadCount());
        stats.put("ratingCount", document.getRatingCount());
        stats.put("ratingAverage", document.getRatingAverage());
        stats.put("createdAt", document.getCreatedAt());
        stats.put("fileSize", document.getFileSize());
        stats.put("fileType", document.getFileType());

        return stats;
    }

    /**
     * 获取文档总数
     */
    public Long getTotalDocumentCount() {
        return documentRepository.count();
    }

    /**
     * 获取今日上传文档数
     */
    public Long getTodayUploadCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return documentRepository.countByCreatedAtAfter(startOfDay);
    }

    /**
     * 获取今日下载文档数
     */
    public Long getTodayDownloadCount() {
        LocalDateTime startOfDay = LocalDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return downloadRecordRepository.countByDownloadTimeAfter(startOfDay);
    }

    /**
     * 获取待审核文档数量
     */
    public Long getPendingDocumentCount() {
        return documentRepository.countByStatus(DocumentStatus.PENDING);
    }

    /**
     * 获取今日处理文档数量
     */
    public Long getTodayProcessedCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        // 统计今日审核通过和拒绝的文档数量
        return documentRepository.countByApprovedAtAfter(startOfDay);
    }
}






