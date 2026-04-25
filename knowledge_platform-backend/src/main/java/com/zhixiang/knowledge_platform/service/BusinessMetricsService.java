package com.zhixiang.knowledge_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务监控服务
 * 收集和分析平台的业务指标数据
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessMetricsService {

    // 实时计数器
    private final AtomicLong totalVisits = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicLong totalUploads = new AtomicLong(0);
    private final AtomicLong totalDownloads = new AtomicLong(0);

    // 业务指标存储
    private final Map<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimeHistory = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userActionCounts = new ConcurrentHashMap<>();

    // 每日统计
    private final Map<String, DailyMetrics> dailyMetrics = new ConcurrentHashMap<>();

    /**
     * 记录API调用
     */
    public void recordApiCall(String endpoint, long responseTime, boolean success) {
        totalApiCalls.incrementAndGet();

        // 记录API调用次数
        apiCallCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();

        // 记录响应时间
        responseTimeHistory.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(responseTime);

        // 限制历史记录大小
        List<Long> times = responseTimeHistory.get(endpoint);
        if (times.size() > 1000) {
            times.subList(0, times.size() - 1000).clear();
        }

        // 记录错误次数
        if (!success) {
            errorCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        }

        // 更新每日指标
        updateDailyMetrics("api_calls", 1);

        log.debug("API调用记录: endpoint={}, responseTime={}ms, success={}", endpoint, responseTime, success);
    }

    /**
     * 记录用户访问
     */
    public void recordUserVisit(Long userId, String action) {
        totalVisits.incrementAndGet();

        String actionKey = action != null ? action : "visit";
        userActionCounts.computeIfAbsent(actionKey, k -> new AtomicLong(0)).incrementAndGet();

        updateDailyMetrics("visits", 1);

        log.debug("用户访问记录: userId={}, action={}", userId, action);
    }

    /**
     * 记录文档上传
     */
    public void recordDocumentUpload(Long userId, String documentType) {
        totalUploads.incrementAndGet();
        userActionCounts.computeIfAbsent("upload", k -> new AtomicLong(0)).incrementAndGet();

        // 按文档类型统计
        String typeKey = "upload_" + (documentType != null ? documentType : "unknown");
        userActionCounts.computeIfAbsent(typeKey, k -> new AtomicLong(0)).incrementAndGet();

        updateDailyMetrics("uploads", 1);

        log.info("文档上传记录: userId={}, documentType={}", userId, documentType);
    }

    /**
     * 记录文档下载
     */
    public void recordDocumentDownload(Long userId, String documentType, int pointsCost) {
        totalDownloads.incrementAndGet();
        userActionCounts.computeIfAbsent("download", k -> new AtomicLong(0)).incrementAndGet();

        // 按文档类型统计
        String typeKey = "download_" + (documentType != null ? documentType : "unknown");
        userActionCounts.computeIfAbsent(typeKey, k -> new AtomicLong(0)).incrementAndGet();

        updateDailyMetrics("downloads", 1);
        updateDailyMetrics("points_consumed", pointsCost);

        log.info("文档下载记录: userId={}, documentType={}, pointsCost={}", userId, documentType, pointsCost);
    }

    /**
     * 记录用户注册
     */
    public void recordUserRegistration(String registrationSource) {
        userActionCounts.computeIfAbsent("registration", k -> new AtomicLong(0)).incrementAndGet();

        String sourceKey = "registration_" + (registrationSource != null ? registrationSource : "direct");
        userActionCounts.computeIfAbsent(sourceKey, k -> new AtomicLong(0)).incrementAndGet();

        updateDailyMetrics("registrations", 1);

        log.info("用户注册记录: source={}", registrationSource);
    }

    /**
     * 记录积分变动
     */
    public void recordPointsTransaction(Long userId, int pointsChange, String type) {
        String actionKey = pointsChange > 0 ? "points_earned" : "points_spent";
        userActionCounts.computeIfAbsent(actionKey, k -> new AtomicLong(0)).addAndGet(Math.abs(pointsChange));

        String typeKey = actionKey + "_" + (type != null ? type : "unknown");
        userActionCounts.computeIfAbsent(typeKey, k -> new AtomicLong(0)).addAndGet(Math.abs(pointsChange));

        updateDailyMetrics(actionKey, Math.abs(pointsChange));

        log.debug("积分变动记录: userId={}, change={}, type={}", userId, pointsChange, type);
    }

    /**
     * 更新活跃用户数
     */
    public void updateActiveUsers(int count) {
        activeUsers.set(count);
    }

    /**
     * 获取实时指标
     */
    @Cacheable(value = "metrics", key = "'realtime'")
    public Map<String, Object> getRealtimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalVisits", totalVisits.get());
        metrics.put("totalApiCalls", totalApiCalls.get());
        metrics.put("activeUsers", activeUsers.get());
        metrics.put("totalUploads", totalUploads.get());
        metrics.put("totalDownloads", totalDownloads.get());

        // 今日指标
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DailyMetrics todayMetrics = dailyMetrics.get(today);
        if (todayMetrics != null) {
            metrics.put("todayVisits", todayMetrics.visits);
            metrics.put("todayUploads", todayMetrics.uploads);
            metrics.put("todayDownloads", todayMetrics.downloads);
            metrics.put("todayRegistrations", todayMetrics.registrations);
            metrics.put("todayApiCalls", todayMetrics.apiCalls);
        } else {
            metrics.put("todayVisits", 0);
            metrics.put("todayUploads", 0);
            metrics.put("todayDownloads", 0);
            metrics.put("todayRegistrations", 0);
            metrics.put("todayApiCalls", 0);
        }

        metrics.put("timestamp", LocalDateTime.now());

        return metrics;
    }

    /**
     * 获取API性能指标
     */
    @Cacheable(value = "metrics", key = "'api_performance'")
    public Map<String, Object> getApiPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // API调用统计
        Map<String, Long> callCounts = new HashMap<>();
        apiCallCounts.forEach((k, v) -> callCounts.put(k, v.get()));
        metrics.put("apiCallCounts", callCounts);

        // 错误统计
        Map<String, Long> errors = new HashMap<>();
        errorCounts.forEach((k, v) -> errors.put(k, v.get()));
        metrics.put("errorCounts", errors);

        // 响应时间统计
        Map<String, Map<String, Double>> responseTimeStats = new HashMap<>();
        responseTimeHistory.forEach((endpoint, times) -> {
            if (!times.isEmpty()) {
                List<Long> sortedTimes = new ArrayList<>(times);
                Collections.sort(sortedTimes);

                Map<String, Double> stats = new HashMap<>();
                stats.put("avg", sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
                stats.put("min", (double) sortedTimes.get(0));
                stats.put("max", (double) sortedTimes.get(sortedTimes.size() - 1));
                stats.put("p50", (double) sortedTimes.get(sortedTimes.size() / 2));
                stats.put("p95", (double) sortedTimes.get((int) (sortedTimes.size() * 0.95)));

                responseTimeStats.put(endpoint, stats);
            }
        });
        metrics.put("responseTimeStats", responseTimeStats);

        return metrics;
    }

    /**
     * 获取用户行为指标
     */
    @Cacheable(value = "metrics", key = "'user_behavior'")
    public Map<String, Object> getUserBehaviorMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> actions = new HashMap<>();
        userActionCounts.forEach((k, v) -> actions.put(k, v.get()));
        metrics.put("userActions", actions);

        // 计算转化率
        long visits = userActionCounts.getOrDefault("visit", new AtomicLong(0)).get();
        long registrations = userActionCounts.getOrDefault("registration", new AtomicLong(0)).get();
        long uploads = userActionCounts.getOrDefault("upload", new AtomicLong(0)).get();
        long downloads = userActionCounts.getOrDefault("download", new AtomicLong(0)).get();

        Map<String, Double> conversionRates = new HashMap<>();
        if (visits > 0) {
            conversionRates.put("visitToRegistration", (double) registrations / visits * 100);
            conversionRates.put("visitToUpload", (double) uploads / visits * 100);
            conversionRates.put("visitToDownload", (double) downloads / visits * 100);
        }
        if (registrations > 0) {
            conversionRates.put("registrationToUpload", (double) uploads / registrations * 100);
            conversionRates.put("registrationToDownload", (double) downloads / registrations * 100);
        }

        metrics.put("conversionRates", conversionRates);

        return metrics;
    }

    /**
     * 获取历史趋势数据
     */
    public Map<String, Object> getHistoricalTrends(int days) {
        Map<String, Object> trends = new HashMap<>();

        List<Map<String, Object>> dailyData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = days - 1; i >= 0; i--) {
            String date = now.minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            DailyMetrics metrics = dailyMetrics.get(date);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date);
            dayData.put("visits", metrics != null ? metrics.visits : 0);
            dayData.put("uploads", metrics != null ? metrics.uploads : 0);
            dayData.put("downloads", metrics != null ? metrics.downloads : 0);
            dayData.put("registrations", metrics != null ? metrics.registrations : 0);
            dayData.put("apiCalls", metrics != null ? metrics.apiCalls : 0);

            dailyData.add(dayData);
        }

        trends.put("dailyData", dailyData);
        trends.put("period", days + " days");

        return trends;
    }

    /**
     * 更新每日指标
     */
    private void updateDailyMetrics(String metricName, long value) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        DailyMetrics metrics = dailyMetrics.computeIfAbsent(today, k -> new DailyMetrics());

        switch (metricName) {
            case "visits" -> metrics.visits += value;
            case "uploads" -> metrics.uploads += value;
            case "downloads" -> metrics.downloads += value;
            case "registrations" -> metrics.registrations += value;
            case "api_calls" -> metrics.apiCalls += value;
            case "points_earned" -> metrics.pointsEarned += value;
            case "points_consumed" -> metrics.pointsConsumed += value;
        }
    }

    /**
     * 定时清理过期数据
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredData() {
        try {
            // 清理30天前的每日指标
            String cutoffDate = LocalDateTime.now().minusDays(30)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            dailyMetrics.entrySet().removeIf(entry -> entry.getKey().compareTo(cutoffDate) < 0);

            // 清理过长的响应时间历史
            responseTimeHistory.values().forEach(times -> {
                if (times.size() > 1000) {
                    times.subList(0, times.size() - 1000).clear();
                }
            });

            log.debug("业务指标数据清理完成");

        } catch (Exception e) {
            log.error("清理业务指标数据失败", e);
        }
    }

    /**
     * 定时报告系统状态
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void reportSystemStatus() {
        try {
            Map<String, Object> metrics = getRealtimeMetrics();
            log.info("系统状态报告: 总访问量={}, 活跃用户={}, 今日上传={}, 今日下载={}",
                metrics.get("totalVisits"),
                metrics.get("activeUsers"),
                metrics.get("todayUploads"),
                metrics.get("todayDownloads"));

        } catch (Exception e) {
            log.error("系统状态报告失败", e);
        }
    }

    /**
     * 异步处理事件
     */
    @Async
    @EventListener
    public void handleBusinessEvent(BusinessEvent event) {
        try {
            switch (event.getType()) {
                case DOCUMENT_UPLOADED -> recordDocumentUpload(event.getUserId(), event.getDocumentType());
                case DOCUMENT_DOWNLOADED -> recordDocumentDownload(event.getUserId(), event.getDocumentType(), event.getPointsCost());
                case USER_REGISTERED -> recordUserRegistration(event.getSource());
                case POINTS_CHANGED -> recordPointsTransaction(event.getUserId(), event.getPointsChange(), event.getTransactionType());
                case USER_LOGIN -> recordUserVisit(event.getUserId(), "login");
            }
        } catch (Exception e) {
            log.error("处理业务事件失败: {}", event, e);
        }
    }

    /**
     * 每日指标数据类
     */
    private static class DailyMetrics {
        public long visits = 0;
        public long uploads = 0;
        public long downloads = 0;
        public long registrations = 0;
        public long apiCalls = 0;
        public long pointsEarned = 0;
        public long pointsConsumed = 0;
    }

    /**
     * 业务事件类
     */
    public static class BusinessEvent {
        private final BusinessEventType type;
        private final Long userId;
        private final String documentType;
        private final int pointsCost;
        private final int pointsChange;
        private final String source;
        private final String transactionType;

        public BusinessEvent(BusinessEventType type, Long userId) {
            this(type, userId, null, 0, 0, null, null);
        }

        public BusinessEvent(BusinessEventType type, Long userId, String documentType, int pointsCost, int pointsChange, String source, String transactionType) {
            this.type = type;
            this.userId = userId;
            this.documentType = documentType;
            this.pointsCost = pointsCost;
            this.pointsChange = pointsChange;
            this.source = source;
            this.transactionType = transactionType;
        }

        // Getters
        public BusinessEventType getType() { return type; }
        public Long getUserId() { return userId; }
        public String getDocumentType() { return documentType; }
        public int getPointsCost() { return pointsCost; }
        public int getPointsChange() { return pointsChange; }
        public String getSource() { return source; }
        public String getTransactionType() { return transactionType; }

        @Override
        public String toString() {
            return "BusinessEvent{type=" + type + ", userId=" + userId + "}";
        }
    }

    /**
     * 业务事件类型枚举
     */
    public enum BusinessEventType {
        DOCUMENT_UPLOADED,
        DOCUMENT_DOWNLOADED,
        USER_REGISTERED,
        USER_LOGIN,
        POINTS_CHANGED
    }
}