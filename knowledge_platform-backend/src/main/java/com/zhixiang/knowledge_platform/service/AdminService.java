package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.entity.*;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.PointsType;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.*;
import com.zhixiang.knowledge_platform.service.ai.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final DownloadRecordRepository downloadRecordRepository;
    private final CopyrightReportRepository copyrightReportRepository;
    private final ForumTopicRepository forumTopicRepository;

    private final PointsService pointsService;
    private final FileUploadService fileUploadService;
    private final RagService ragService;

    /**
     * 获取管理员仪表盘数据
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        // 用户统计
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long todayNewUsers = userRepository.countByCreatedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay());

        dashboard.put("userStats", Map.of(
            "total", totalUsers,
            "active", activeUsers,
            "today", todayNewUsers
        ));

        // 文档统计
        long totalDocuments = documentRepository.count();
        long approvedDocuments = documentRepository.countByStatus(DocumentStatus.APPROVED);
        long pendingDocuments = documentRepository.countByStatus(DocumentStatus.PENDING);
        long rejectedDocuments = documentRepository.countByStatus(DocumentStatus.REJECTED);

        dashboard.put("documentStats", Map.of(
            "total", totalDocuments,
            "approved", approvedDocuments,
            "pending", pendingDocuments,
            "rejected", rejectedDocuments
        ));

        // 今日活动统计
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayUploads = documentRepository.countByCreatedAtAfter(todayStart);
        long todayDownloads = downloadRecordRepository.countByDownloadTimeAfter(todayStart);
        long todayComments = commentRepository.countByCreatedAtAfter(todayStart);

        dashboard.put("todayActivity", Map.of(
            "uploads", todayUploads,
            "downloads", todayDownloads,
            "comments", todayComments
        ));

        // 系统统计
        long totalCategories = categoryRepository.count();
        long totalTags = tagRepository.count();
        Long totalFileSize = documentRepository.getTotalFileSizeByStatus(DocumentStatus.APPROVED);

        Map<String, Object> systemStats = new HashMap<>();
        systemStats.put("categories", totalCategories);
        systemStats.put("tags", totalTags);
        systemStats.put("totalFileSize", totalFileSize != null ? totalFileSize : 0);
        systemStats.put("totalFileSizeReadable", formatFileSize(totalFileSize != null ? totalFileSize : 0));

        try {
            long totalDownloads = downloadRecordRepository.count();
            systemStats.put("totalDownloads", totalDownloads);
        } catch (Exception e) {
            log.warn("Failed to get total downloads: {}", e.getMessage());
            systemStats.put("totalDownloads", 0L);
        }

        try {
            Double averageRating = ratingRepository.getAverageRating();
            systemStats.put("averageRating", averageRating != null ? averageRating : 0.0);
        } catch (Exception e) {
            log.warn("Failed to get average rating: {}", e.getMessage());
            systemStats.put("averageRating", 0.0);
        }

        dashboard.put("systemStats", systemStats);

        // 趋势数据 - 最近7天
        List<Map<String, Object>> uploadTrend = new java.util.ArrayList<>();
        List<Map<String, Object>> userTrend = new java.util.ArrayList<>();
        List<Map<String, Object>> topicTrend = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            long uploads = documentRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long users = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long topics = forumTopicRepository.countByCreatedAtBetween(dayStart, dayEnd);

            String dateStr = dayStart.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
            uploadTrend.add(Map.of("date", dateStr, "value", uploads));
            userTrend.add(Map.of("date", dateStr, "value", users));
            topicTrend.add(Map.of("date", dateStr, "value", topics));
        }

        dashboard.put("trends", Map.of("uploads", uploadTrend, "users", userTrend, "topics", topicTrend));

        // 分类分布 - 包含所有子分类的文档统计，汇总到一级分类
        List<Map<String, Object>> categoryDist = getCategoryDistributionStats(DocumentStatus.APPROVED);
        dashboard.put("categoryDistribution", categoryDist);

        return dashboard;
    }

    /**
     * 辅助方法：获取包含子分类的一级分类文档分布统计
     */
    private List<Map<String, Object>> getCategoryDistributionStats(DocumentStatus status) {
        List<Object[]> categoryStats = documentRepository.countDocumentsByCategory(status);
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<String, Long> topLevelCategoryCounts = new HashMap<>();

        for (Object[] stat : categoryStats) {
            Long categoryId = ((Number) stat[0]).longValue();
            Long count = ((Number) stat[1]).longValue();

            Category category = categoryMap.get(categoryId);
            if (category != null) {
                // 向上查找到一级分类
                Category topLevel = category;
                while (topLevel.getParentId() != null && categoryMap.containsKey(topLevel.getParentId())) {
                    topLevel = categoryMap.get(topLevel.getParentId());
                }

                String topLevelName = topLevel.getName();
                topLevelCategoryCounts.put(topLevelName,
                        topLevelCategoryCounts.getOrDefault(topLevelName, 0L) + count);
            }
        }

        return topLevelCategoryCounts.entrySet().stream()
                .map(e -> Map.of("name", (Object) e.getKey(), "value", (Object) e.getValue()))
                .filter(m -> (Long) m.get("value") > 0)
                .collect(Collectors.toList());
    }

    /**
     * 获取管理员文档列表（支持筛选）
     */
    public PageResponse<com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse> getDocumentsForAdmin(int page, int size, String status, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> documentPage;

        DocumentStatus documentStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                documentStatus = DocumentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态值
            }
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 关键词搜索
            if (documentStatus != null) {
                documentPage = documentRepository.advancedSearchDocuments(keyword, null, null, documentStatus, pageable);
            } else {
                // 搜索时排除已删除的文档
                documentPage = documentRepository.searchDocumentsExcludeDeleted(keyword, pageable);
            }
        } else if (documentStatus != null) {
            // 按状态筛选
            documentPage = documentRepository.findByStatus(documentStatus, pageable);
        } else {
            // 获取所有非已删除的文档
            documentPage = documentRepository.findByStatusNot(DocumentStatus.DELETED, pageable);
        }

        List<com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse> responseList = documentPage.getContent().stream()
                .map(this::convertToDocumentInfoResponse)
                .toList();

        return PageResponse.fromPage(documentPage, responseList);
    }

    /**
     * 获取待审核文档列表
     */
    public PageResponse<Document> getPendingDocuments(int page, int size, String sortBy) {
        Sort sort = switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "largest" -> Sort.by(Sort.Direction.DESC, "fileSize");
            case "smallest" -> Sort.by(Sort.Direction.ASC, "fileSize");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // latest
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Document> documentPage = documentRepository.findByStatus(DocumentStatus.PENDING, pageable);

        return PageResponse.fromPage(documentPage, documentPage.getContent());
    }

    /**
     * 审核通过文档
     */
    @Transactional
    public void approveDocument(Long documentId, Long adminId, String note) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (!document.isPending()) {
            throw new RuntimeException("只能审核待审核状态的文档");
        }

        // 更新文档状态
        document.approve(adminId);
        documentRepository.save(document);

        // 给上传者奖励积分
        pointsService.addPoints(document.getUploaderId(), 10, PointsType.EARN,
                "文档《" + document.getTitle() + "》审核通过奖励");

        // 自动导入向量库（RAG）
        if (ragService.isAvailable()) {
            try {
                ragService.reingestDocument(documentId);
                log.info("文档审核通过后自动向量化完成，ID: {}", documentId);
            } catch (Exception e) {
                log.warn("文档向量化失败，ID: {}", documentId, e);
            }
        } else {
            log.warn("RAG向量存储不可用，跳过自动向量化，请检查Ollama和Qdrant服务");
        }

        log.info("文档审核通过，ID: {}, 管理员: {}, 备注: {}", documentId, adminId, note);
    }

    /**
     * 拒绝文档
     */
    @Transactional
    public void rejectDocument(Long documentId, String reason, String note) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (!document.isPending()) {
            throw new RuntimeException("只能审核待审核状态的文档");
        }

        // 更新文档状态
        document.reject(reason);
        documentRepository.save(document);

        // 删除文档文件
        if (document.getFilePath() != null) {
            fileUploadService.deleteFile(document.getFilePath());
        }

        log.info("文档审核拒绝，ID: {}, 原因: {}, 备注: {}", documentId, reason, note);
    }

    /**
     * 批量审核文档
     */
    @Transactional
    public Map<String, Integer> batchApproveDocuments(List<Long> documentIds, Long adminId) {
        int successCount = 0;
        int failCount = 0;

        for (Long documentId : documentIds) {
            try {
                approveDocument(documentId, adminId, "批量审核通过");
                successCount++;
            } catch (Exception e) {
                log.error("批量审核文档失败，ID: {}", documentId, e);
                failCount++;
            }
        }

        return Map.of("success", successCount, "failed", failCount);
    }

    /**
     * 获取所有用户列表
     */
    public List<com.zhixiang.knowledge_platform.dto.response.UserInfoResponse> getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return users.stream()
                .map(this::convertToUserInfoResponse)
                .toList();
    }

    /**
     * 获取用户管理列表
     */
    public PageResponse<User> getUsersForManagement(int page, int size, UserStatus status, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;

        if (status != null && keyword != null && !keyword.trim().isEmpty()) {
            // 按状态和关键词筛选
            userPage = userRepository.findByStatusAndUsernameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
                    status, keyword, status, keyword, pageable);
        } else if (status != null) {
            // 按状态筛选
            userPage = userRepository.findByStatus(status, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 按关键词搜索
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                    keyword, keyword, keyword, pageable);
        } else {
            // 获取所有
            userPage = userRepository.findAll(pageable);
        }

        return PageResponse.fromPage(userPage, userPage.getContent());
    }

    /**
     * 禁用用户
     */
    @Transactional
    public void disableUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("不能禁用管理员账户");
        }

        user.setStatus(UserStatus.DISABLED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.warn("用户已被禁用，ID: {}, 原因: {}", userId, reason);
    }

    /**
     * 启用用户
     */
    @Transactional
    public void enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户已被启用，ID: {}", userId);
    }

    /**
     * 调整用户积分
     */
    @Transactional
    public void adjustUserPoints(Long userId, Integer points, String reason) {
        if (points > 0) {
            pointsService.addPoints(userId, points, PointsType.EARN, reason);
        } else if (points < 0) {
            pointsService.deductPoints(userId, Math.abs(points), PointsType.SPEND, reason);
        }
        log.info("管理员调整用户积分，用户ID: {}, 积分: {}, 原因: {}", userId, points, reason);
    }

    /**
     * 获取系统操作日志
     */
    public PageResponse<PointsRecord> getSystemLogs(int page, int size, PointsType type, LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PointsRecord> logPage;

        if (type != null && startTime != null && endTime != null) {
            logPage = pointsRecordRepository.findByTypeAndCreatedAtBetween(type, startTime, endTime, pageable);
        } else if (type != null) {
            logPage = pointsRecordRepository.findByType(type, pageable);
        } else if (startTime != null && endTime != null) {
            logPage = pointsRecordRepository.findByCreatedAtBetween(startTime, endTime, pageable);
        } else {
            logPage = pointsRecordRepository.findAll(pageable);
        }

        return PageResponse.fromPage(logPage, logPage.getContent());
    }

    /**
     * 获取内容统计报告
     */
    public Map<String, Object> getContentStatisticsReport() {
        Map<String, Object> report = new HashMap<>();

        // 文档状态分布
        List<Object[]> docStatusStats = documentRepository.countDocumentsByStatus();
        Map<String, Long> statusDistribution = new HashMap<>();
        for (Object[] stat : docStatusStats) {
            statusDistribution.put(stat[0].toString(), (Long) stat[1]);
        }
        report.put("documentStatusDistribution", statusDistribution);

        // 分类文档分布 - 汇总到一级分类
        List<Map<String, Object>> categoryStats = getCategoryDistributionStats(DocumentStatus.APPROVED);
        // 需要将其转换为 List<Object[]> 格式以保持与原有接口的兼容性
        List<Object[]> categoryStatsArray = categoryStats.stream()
                .map(m -> new Object[]{m.get("name"), m.get("value")})
                .collect(Collectors.toList());
        report.put("categoryDistribution", categoryStatsArray);

        // 用户活跃度统计
        List<Object[]> uploaderStats = documentRepository.countDocumentsByUploader(DocumentStatus.APPROVED);
        report.put("uploaderActivity", uploaderStats);

        // 评分分布
        List<Object[]> ratingStats = ratingRepository.getRatingDistribution();
        report.put("ratingDistribution", ratingStats);

        // 近期活动趋势（最近30天）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long recentUploads = documentRepository.countByCreatedAtAfter(thirtyDaysAgo);
        Long recentDownloads = downloadRecordRepository.countByDownloadTimeAfter(thirtyDaysAgo);
        Long recentComments = commentRepository.countByCreatedAtAfter(thirtyDaysAgo);

        report.put("recentActivity", Map.of(
            "uploads", recentUploads,
            "downloads", recentDownloads,
            "comments", recentComments
        ));

        return report;
    }

    /**
     * 删除不当内容
     */
    @Transactional
    public void removeInappropriateContent(String contentType, Long contentId, String reason) {
        switch (contentType.toLowerCase()) {
            case "document" -> {
                Document document = documentRepository.findById(contentId)
                        .orElseThrow(() -> new RuntimeException("文档不存在"));

                // 软删除文档记录
                document.delete(reason);
                documentRepository.save(document);
                
                // 不直接物理删除文件，保留作后续申诉/证据用途，或通过定时任务清理
                // if (document.getFilePath() != null) {
                //     fileUploadService.deleteFile(document.getFilePath());
                // }

                log.warn("管理员软删除不当文档，ID: {}, 原因: {}", contentId, reason);
            }
            case "comment" -> {
                Comment comment = commentRepository.findById(contentId)
                        .orElseThrow(() -> new RuntimeException("评论不存在"));

                comment.setIsDeleted(true);
                comment.setUpdatedAt(LocalDateTime.now());
                commentRepository.save(comment);
                log.warn("管理员删除不当评论，ID: {}, 原因: {}", contentId, reason);
            }
            default -> throw new IllegalArgumentException("不支持的内容类型: " + contentType);
        }
    }

    /**
     * 获取举报处理队列
     */
    public Map<String, Object> getReportQueue() {
        long pendingCount = copyrightReportRepository.countByStatus(com.zhixiang.knowledge_platform.enums.ReportStatus.PENDING);
        long processedCount = copyrightReportRepository.count() - pendingCount;
        List<CopyrightReport> reports = copyrightReportRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return Map.of(
            "pendingReports", pendingCount,
            "processedReports", processedCount,
            "reports", reports
        );
    }

    /**
     * 转换用户实体为响应DTO
     */
    private com.zhixiang.knowledge_platform.dto.response.UserInfoResponse convertToUserInfoResponse(User user) {
        com.zhixiang.knowledge_platform.dto.response.UserInfoResponse response = new com.zhixiang.knowledge_platform.dto.response.UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        // response.setNickname(user.getUsername()); // nickname字段已删除
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setPoints(user.getPoints());
        response.setAvatar(user.getAvatar());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginTime(user.getLastLoginTime());
        return response;
    }

    /**
     * 转换文档实体为响应DTO
     */
    private com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse convertToDocumentInfoResponse(Document document) {
        com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse response = new com.zhixiang.knowledge_platform.dto.response.DocumentInfoResponse();
        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setDescription(document.getDescription());
        response.setFileName(document.getFileName());
        response.setFileSize(document.getFileSize());
        response.setFileExtension(document.getFileExtension());
        response.setDownloadPoints(document.getDownloadPoints());
        response.setStatus(document.getStatus());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setRatingAverage(document.getRatingAverage());
        response.setRatingCount(document.getRatingCount());
        response.setDownloadCount(document.getDownloadCount());

        // 设置上传者信息
        if (document.getUploader() != null) {
            com.zhixiang.knowledge_platform.dto.response.UserInfoResponse uploaderInfo = new com.zhixiang.knowledge_platform.dto.response.UserInfoResponse();
            uploaderInfo.setId(document.getUploader().getId());
            uploaderInfo.setUsername(document.getUploader().getUsername());
            // uploaderInfo.setNickname(document.getUploader().getUsername()); // nickname字段已删除
            response.setUploader(uploaderInfo);
        }

        // 设置分类信息
        if (document.getCategory() != null) {
            com.zhixiang.knowledge_platform.dto.response.CategoryInfoResponse categoryInfo = new com.zhixiang.knowledge_platform.dto.response.CategoryInfoResponse();
            categoryInfo.setId(document.getCategory().getId());
            categoryInfo.setName(document.getCategory().getName());
            categoryInfo.setDescription(document.getCategory().getDescription());
            response.setCategory(categoryInfo);
        }

        return response;
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
}