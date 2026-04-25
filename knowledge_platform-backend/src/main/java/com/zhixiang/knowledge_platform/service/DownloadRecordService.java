package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.DownloadRecordResponse;
import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import com.zhixiang.knowledge_platform.repository.DownloadRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 下载记录服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadRecordService {

    private final DownloadRecordRepository downloadRecordRepository;

    /**
     * 记录用户下载
     */
    @Transactional
    public DownloadRecord recordDownload(Long userId, Long documentId, Integer pointsCost) {
        log.info("记录用户下载，用户ID: {}, 文档ID: {}, 积分消费: {}", userId, documentId, pointsCost);

        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setDocumentId(documentId);
        record.setPointsCost(pointsCost);
        record.setDownloadTime(LocalDateTime.now());

        DownloadRecord savedRecord = downloadRecordRepository.save(record);
        log.info("下载记录保存成功，记录ID: {}", savedRecord.getId());

        return savedRecord;
    }

    /**
     * 获取用户下载历史
     */
    public PageResponse<DownloadRecord> getUserDownloadHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadTime"));
        // 使用JOIN FETCH查询避免懒加载问题
        Page<DownloadRecord> recordPage = downloadRecordRepository.findByUserIdWithDocumentOrderByDownloadTimeDesc(userId, pageable);

        return PageResponse.fromPage(recordPage, recordPage.getContent());
    }

    /**
     * 获取用户下载历史（DTO版本，避免懒加载问题）
     */
    public PageResponse<DownloadRecordResponse> getUserDownloadHistoryDto(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadTime"));
        // 使用JOIN FETCH查询避免懒加载问题
        Page<DownloadRecord> recordPage = downloadRecordRepository.findByUserIdWithDocumentOrderByDownloadTimeDesc(userId, pageable);

        // 转换为DTO
        List<DownloadRecordResponse> dtoList = recordPage.getContent().stream()
                .map(DownloadRecordResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.fromPage(recordPage, dtoList);
    }

    /**
     * 获取文档下载记录
     */
    public PageResponse<DownloadRecord> getDocumentDownloadHistory(Long documentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadTime"));
        Page<DownloadRecord> recordPage = downloadRecordRepository.findByDocumentIdOrderByDownloadTimeDesc(documentId, pageable);

        return PageResponse.fromPage(recordPage, recordPage.getContent());
    }

    /**
     * 检查用户是否已下载过文档
     */
    public boolean hasUserDownloadedDocument(Long userId, Long documentId) {
        return downloadRecordRepository.existsByUserIdAndDocumentId(userId, documentId);
    }

    /**
     * 获取用户下载统计
     */
    public Map<String, Object> getUserDownloadStatistics(Long userId) {
        Long totalDownloads = downloadRecordRepository.countByUserId(userId);
        Integer totalPointsSpent = downloadRecordRepository.getTotalPointsSpentByUser(userId);

        // 获取最近下载的文档数量（最近30天）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long recentDownloads = downloadRecordRepository.countByUserIdAndDownloadTimeAfter(userId, thirtyDaysAgo);

        return Map.of(
            "totalDownloads", totalDownloads,
            "totalPointsSpent", totalPointsSpent != null ? totalPointsSpent : 0,
            "recentDownloads", recentDownloads
        );
    }

    /**
     * 获取文档下载统计
     */
    public Map<String, Object> getDocumentDownloadStatistics(Long documentId) {
        Long totalDownloads = downloadRecordRepository.countByDocumentId(documentId);
        Integer totalPointsEarned = downloadRecordRepository.getTotalPointsEarnedByDocument(documentId);

        // 获取最近下载数量（最近30天）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long recentDownloads = downloadRecordRepository.countByDocumentIdAndDownloadTimeAfter(documentId, thirtyDaysAgo);

        // 获取独立下载用户数
        Long uniqueDownloaders = downloadRecordRepository.countDistinctUsersByDocumentId(documentId);

        return Map.of(
            "totalDownloads", totalDownloads,
            "totalPointsEarned", totalPointsEarned != null ? totalPointsEarned : 0,
            "recentDownloads", recentDownloads,
            "uniqueDownloaders", uniqueDownloaders
        );
    }

    /**
     * 获取热门下载文档
     */
    public List<Object[]> getPopularDocuments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return downloadRecordRepository.findMostDownloadedDocuments(pageable);
    }

    /**
     * 获取活跃下载用户
     */
    public List<Object[]> getActiveDownloaders(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return downloadRecordRepository.findMostActiveDownloaders(pageable);
    }

    /**
     * 获取下载趋势数据
     */
    public List<Object[]> getDownloadTrendData(LocalDateTime startTime, LocalDateTime endTime) {
        return downloadRecordRepository.getDownloadTrendData(startTime, endTime);
    }

    /**
     * 获取最近下载记录
     */
    public List<DownloadRecord> getRecentDownloads(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return downloadRecordRepository.findRecentDownloads(pageable);
    }

    /**
     * 删除用户的所有下载记录
     */
    @Transactional
    public void deleteUserDownloadRecords(Long userId) {
        log.info("删除用户下载记录，用户ID: {}", userId);
        downloadRecordRepository.deleteByUserId(userId);
    }

    /**
     * 删除文档的所有下载记录
     */
    @Transactional
    public void deleteDocumentDownloadRecords(Long documentId) {
        log.info("删除文档下载记录，文档ID: {}", documentId);
        downloadRecordRepository.deleteByDocumentId(documentId);
    }

    /**
     * 获取系统下载统计
     */
    public Map<String, Object> getSystemDownloadStatistics() {
        Long totalRecords = downloadRecordRepository.count();
        Integer totalPointsTransferred = downloadRecordRepository.getTotalPointsTransferred();

        // 今日下载数
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long todayDownloads = downloadRecordRepository.countByDownloadTimeAfter(todayStart);

        // 本月下载数
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        Long monthDownloads = downloadRecordRepository.countByDownloadTimeAfter(monthStart);

        return Map.of(
            "totalRecords", totalRecords,
            "totalPointsTransferred", totalPointsTransferred != null ? totalPointsTransferred : 0,
            "todayDownloads", todayDownloads,
            "monthDownloads", monthDownloads
        );
    }

    /**
     * 清理历史下载记录
     */
    @Transactional
    public void cleanHistoryRecords(int retainMonths) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMonths(retainMonths);
        downloadRecordRepository.deleteByDownloadTimeBefore(cutoffTime);
        log.info("清理{}个月前的下载记录完成", retainMonths);
    }
}