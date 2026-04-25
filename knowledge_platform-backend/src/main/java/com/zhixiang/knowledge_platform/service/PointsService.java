package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.PointsRecordResponse;
import com.zhixiang.knowledge_platform.entity.PointsRecord;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.PointsSource;
import com.zhixiang.knowledge_platform.enums.PointsType;
import com.zhixiang.knowledge_platform.repository.PointsRecordRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分系统服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointsService {

    private final PointsRecordRepository pointsRecordRepository;
    private final UserRepository userRepository;

    @Value("${zhixiang.points.initial}")
    private int initialPoints;

    @Value("${zhixiang.points.upload-reward}")
    private int uploadReward;

    // 常量配置
    private static final int MAX_POINTS = 10000; // 最大积分持有量
    private static final int MAX_DAILY_EARN = 50; // 每日获得积分上限
    private static final int APPROVAL_BONUS = 5; // 审核通过奖励积分
    private static final int DOWNLOAD_REWARD = 2; // 被下载奖励积分
    private static final int RATING_BONUS = 1; // 高评分奖励积分

    /**
     * 获取用户当前积分
     */
    public Integer getUserCurrentPoints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getPoints();
    }

    /**
     * 获取用户积分记录
     */
    public PageResponse<PointsRecordResponse> getUserPointsRecords(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PointsRecord> recordPage = pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PointsRecordResponse> recordList = recordPage.getContent().stream()
                .map(this::convertToPointsRecordResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(recordPage, recordList);
    }

    /**
     * 根据类型获取用户积分记录
     */
    public PageResponse<PointsRecordResponse> getUserPointsRecordsByType(Long userId, PointsType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PointsRecord> recordPage = pointsRecordRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        List<PointsRecordResponse> recordList = recordPage.getContent().stream()
                .map(this::convertToPointsRecordResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(recordPage, recordList);
    }

    /**
     * 用户注册积分奖励
     */
    @Transactional
    public void rewardRegisterPoints(Long userId) {
        log.info("用户注册积分奖励，用户ID: {}, 积分: {}", userId, initialPoints);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, initialPoints, PointsType.EARN, PointsSource.REGISTER, null, "新用户注册奖励"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + initialPoints);
        user.setTotalPoints(user.getTotalPoints() + initialPoints);
        userRepository.save(user);

        log.info("注册积分奖励完成，用户ID: {}, 当前积分: {}", userId, user.getPoints());
    }

    /**
     * 文档上传积分奖励
     */
    @Transactional
    public void rewardUploadPoints(Long userId, Long documentId) {
        log.info("文档上传积分奖励，用户ID: {}, 文档ID: {}, 积分: {}", userId, documentId, uploadReward);

        // 检查今日积分获得是否超限
        if (!canEarnPointsToday(userId, uploadReward)) {
            log.warn("用户今日积分获得已达上限，用户ID: {}", userId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分上限
        if (user.getPoints() + uploadReward > MAX_POINTS) {
            log.warn("用户积分将超过最大持有量，用户ID: {}, 当前积分: {}", userId, user.getPoints());
            return;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, uploadReward, PointsType.EARN, PointsSource.UPLOAD, documentId, "上传文档奖励"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + uploadReward);
        user.setTotalPoints(user.getTotalPoints() + uploadReward);
        userRepository.save(user);

        log.info("上传积分奖励完成，用户ID: {}, 当前积分: {}", userId, user.getPoints());
    }

    /**
     * 文档审核通过积分奖励
     */
    @Transactional
    public void rewardApprovalPoints(Long userId, Long documentId) {
        log.info("文档审核通过积分奖励，用户ID: {}, 文档ID: {}, 积分: {}", userId, documentId, APPROVAL_BONUS);

        // 检查今日积分获得是否超限
        if (!canEarnPointsToday(userId, APPROVAL_BONUS)) {
            log.warn("用户今日积分获得已达上限，用户ID: {}", userId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分上限
        if (user.getPoints() + APPROVAL_BONUS > MAX_POINTS) {
            log.warn("用户积分将超过最大持有量，用户ID: {}, 当前积分: {}", userId, user.getPoints());
            return;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, APPROVAL_BONUS, PointsType.EARN, PointsSource.APPROVED, documentId, "文档审核通过奖励"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + APPROVAL_BONUS);
        user.setTotalPoints(user.getTotalPoints() + APPROVAL_BONUS);
        userRepository.save(user);

        log.info("审核通过积分奖励完成，用户ID: {}, 当前积分: {}", userId, user.getPoints());
    }

    /**
     * 文档被下载积分奖励
     */
    @Transactional
    public void rewardDownloadPoints(Long uploaderId, Long documentId) {
        log.info("文档被下载积分奖励，上传者ID: {}, 文档ID: {}, 积分: {}", uploaderId, documentId, DOWNLOAD_REWARD);

        // 检查今日积分获得是否超限
        if (!canEarnPointsToday(uploaderId, DOWNLOAD_REWARD)) {
            log.warn("用户今日积分获得已达上限，用户ID: {}", uploaderId);
            return;
        }

        User user = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分上限
        if (user.getPoints() + DOWNLOAD_REWARD > MAX_POINTS) {
            log.warn("用户积分将超过最大持有量，用户ID: {}, 当前积分: {}", uploaderId, user.getPoints());
            return;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                uploaderId, DOWNLOAD_REWARD, PointsType.EARN, PointsSource.DOWNLOAD_REWARD, documentId, "文档被下载奖励"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + DOWNLOAD_REWARD);
        user.setTotalPoints(user.getTotalPoints() + DOWNLOAD_REWARD);
        userRepository.save(user);

        log.info("下载奖励积分完成，用户ID: {}, 当前积分: {}", uploaderId, user.getPoints());
    }

    /**
     * 文档获得高评分积分奖励
     */
    @Transactional
    public void rewardRatingPoints(Long uploaderId, Long documentId) {
        log.info("文档获得高评分积分奖励，上传者ID: {}, 文档ID: {}, 积分: {}", uploaderId, documentId, RATING_BONUS);

        // 检查今日积分获得是否超限
        if (!canEarnPointsToday(uploaderId, RATING_BONUS)) {
            log.warn("用户今日积分获得已达上限，用户ID: {}", uploaderId);
            return;
        }

        User user = userRepository.findById(uploaderId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分上限
        if (user.getPoints() + RATING_BONUS > MAX_POINTS) {
            log.warn("用户积分将超过最大持有量，用户ID: {}, 当前积分: {}", uploaderId, user.getPoints());
            return;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                uploaderId, RATING_BONUS, PointsType.EARN, PointsSource.RATING_REWARD, documentId, "文档高评分奖励"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + RATING_BONUS);
        user.setTotalPoints(user.getTotalPoints() + RATING_BONUS);
        userRepository.save(user);

        log.info("高评分积分奖励完成，用户ID: {}, 当前积分: {}", uploaderId, user.getPoints());
    }

    /**
     * 下载文档消费积分
     */
    @Transactional
    public boolean consumeDownloadPoints(Long userId, Long documentId, Integer points) {
        log.info("下载文档消费积分，用户ID: {}, 文档ID: {}, 积分: {}", userId, documentId, points);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分是否充足
        if (user.getPoints() < points) {
            log.warn("用户积分不足，用户ID: {}, 当前积分: {}, 需要积分: {}", userId, user.getPoints(), points);
            return false;
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, points, PointsType.SPEND, PointsSource.DOWNLOAD_COST, documentId, "下载文档消费积分"
        );

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() - points);
        userRepository.save(user);

        log.info("下载积分消费完成，用户ID: {}, 剩余积分: {}", userId, user.getPoints());
        return true;
    }

    /**
     * 添加积分
     */
    @Transactional
    public void addPoints(Long userId, Integer points, PointsType type, String description) {
        if (points <= 0) {
            throw new IllegalArgumentException("积分数量必须大于0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分上限
        if (user.getPoints() + points > MAX_POINTS) {
            throw new RuntimeException("积分将超过最大持有量");
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, points, type, PointsSource.ADMIN_ADJUST, null, description
        );
        pointsRecordRepository.save(record);

        // 更新用户积分
        user.addPoints(points);
        userRepository.save(user);

        log.info("积分添加成功，用户ID: {}, 积分: {}, 当前积分: {}", userId, points, user.getPoints());
    }

    /**
     * 扣除积分
     */
    @Transactional
    public void deductPoints(Long userId, Integer points, PointsType type, String description) {
        if (points <= 0) {
            throw new IllegalArgumentException("积分数量必须大于0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分是否充足
        if (!user.hasEnoughPoints(points)) {
            throw new RuntimeException("用户积分不足");
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, points, type, PointsSource.ADMIN_ADJUST, null, description
        );
        pointsRecordRepository.save(record);

        // 扣除用户积分
        user.deductPoints(points);
        userRepository.save(user);

        log.info("积分扣除成功，用户ID: {}, 积分: {}, 当前积分: {}", userId, points, user.getPoints());
    }

    /**
     * 管理员调整积分
     */
    @Transactional
    public void adminAdjustPoints(Long userId, Integer points, String description) {
        log.info("管理员调整积分，用户ID: {}, 积分: {}, 描述: {}", userId, points, description);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        PointsType type = points > 0 ? PointsType.EARN : PointsType.SPEND;
        Integer absPoints = Math.abs(points);

        // 如果是扣分，检查积分是否充足
        if (points < 0 && user.getPoints() < absPoints) {
            throw new RuntimeException("用户积分不足，无法扣减");
        }

        // 如果是加分，检查积分上限
        if (points > 0 && user.getPoints() + points > MAX_POINTS) {
            throw new RuntimeException("积分将超过最大持有量");
        }

        // 创建积分记录
        PointsRecord record = PointsRecord.createRecord(
                userId, absPoints, type, PointsSource.ADMIN_ADJUST, null, description);

        pointsRecordRepository.save(record);

        // 更新用户积分
        user.setPoints(user.getPoints() + points);
        if (points > 0) {
            user.setTotalPoints(user.getTotalPoints() + points);
        }
        userRepository.save(user);

        log.info("管理员积分调整完成，用户ID: {}, 当前积分: {}", userId, user.getPoints());
    }

    /**
     * 检查用户今日是否还能获得积分
     */
    private boolean canEarnPointsToday(Long userId, Integer points) {
        Integer todayEarned = pointsRecordRepository.getTodayEarnedPoints(userId);
        int currentEarned = todayEarned != null ? todayEarned : 0;
        return (currentEarned + points) <= MAX_DAILY_EARN;
    }

    /**
     * 获取用户今日积分统计
     */
    public Object getTodayPointsStatistics(Long userId) {
        Integer earnedToday = pointsRecordRepository.getTodayEarnedPoints(userId);
        Integer spentToday = pointsRecordRepository.getTodaySpentPoints(userId);
        int currentEarned = earnedToday != null ? earnedToday : 0;
        int currentSpent = spentToday != null ? spentToday : 0;

        return Map.of(
                "earnedToday", currentEarned,
                "spentToday", currentSpent,
                "netToday", currentEarned - currentSpent,
                "remainingDailyEarn", MAX_DAILY_EARN - currentEarned
        );
    }

    /**
     * 获取用户积分统计信息
     */
    public Object getUserPointsStatistics(Long userId) {
        Object[] stats = pointsRecordRepository.getUserPointsStatistics(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return Map.of(
                "currentPoints", user.getPoints(),
                "totalPoints", user.getTotalPoints(),
                "totalEarned", stats[0] != null ? stats[0] : 0,
                "totalSpent", stats[1] != null ? stats[1] : 0,
                "earnRecordCount", stats[2] != null ? stats[2] : 0,
                "spendRecordCount", stats[3] != null ? stats[3] : 0
        );
    }

    /**
     * 获取积分流水统计
     */
    public List<Object[]> getPointsFlowStatistics() {
        return pointsRecordRepository.getPointsFlowStatistics();
    }

    /**
     * 获取积分排行榜
     */
    public List<Object[]> getPointsLeaderboard(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return pointsRecordRepository.getPointsLeaderboard(pageable);
    }

    /**
     * 获取最近积分记录
     */
    public List<PointsRecordResponse> getRecentRecords(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<PointsRecord> records = pointsRecordRepository.findRecentRecords(pageable);

        return records.stream()
                .map(this::convertToPointsRecordResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户月度积分统计
     */
    public List<Object[]> getUserMonthlyStatistics(Long userId) {
        return pointsRecordRepository.getUserMonthlyPointsStatistics(userId);
    }

    /**
     * 获取指定时间范围的积分记录
     */
    public PageResponse<PointsRecordResponse> getRecordsByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(page, size);
        Page<PointsRecord> recordPage = pointsRecordRepository.findRecordsByDateRange(startTime, endTime, pageable);

        List<PointsRecordResponse> recordList = recordPage.getContent().stream()
                .map(this::convertToPointsRecordResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(recordPage, recordList);
    }

    /**
     * 清理历史积分记录
     */
    @Transactional
    public void cleanHistoryRecords(int retainMonths) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMonths(retainMonths);
        pointsRecordRepository.deleteByCreatedAtBefore(cutoffTime);
        log.info("清理{}个月前的积分记录完成", retainMonths);
    }

    /**
     * 转换为积分记录响应DTO
     */
    private PointsRecordResponse convertToPointsRecordResponse(PointsRecord record) {
        return PointsRecordResponse.fromEntity(record);
    }
}