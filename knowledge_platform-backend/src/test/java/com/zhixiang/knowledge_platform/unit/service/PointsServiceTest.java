package com.zhixiang.knowledge_platform.unit.service;

import com.zhixiang.knowledge_platform.entity.PointsRecord;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.PointsSource;
import com.zhixiang.knowledge_platform.enums.PointsType;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.PointsRecordRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import com.zhixiang.knowledge_platform.service.PointsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 积分系统单元测试
 * 测试积分获取与消耗规则
 */
@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock
    private PointsRecordRepository pointsRecordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointsService pointsService;

    @BeforeEach
    void setUp() {
        // 设置私有静态常量
        ReflectionTestUtils.setField(pointsService, "initialPoints", 100);
        ReflectionTestUtils.setField(pointsService, "uploadReward", 10);
    }

    private User createTestUser(Long id, int currentPoints) {
        User user = new User();
        user.setId(id);
        user.setUsername("testUser" + id);
        user.setPoints(currentPoints);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmail("test" + id + "@test.com");
        return user;
    }

    @Test
    @DisplayName("TC-010: 新用户注册应获得初始积分奖励")
    void rewardRegisterPoints_ShouldAddInitialPoints() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, 0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        pointsService.rewardRegisterPoints(userId);

        // Then
        assertEquals(100, user.getPoints());
        verify(pointsRecordRepository).save(any(PointsRecord.class));
    }

    @Test
    @DisplayName("TC-011: 文档审核通过应获得积分奖励")
    void rewardDocumentApprovedPoints_ShouldAddPoints() {
        // Given
        Long uploaderId = 1L;
        Long documentId = 100L;
        User user = createTestUser(uploaderId, 50);
        when(userRepository.findById(uploaderId)).thenReturn(Optional.of(user));
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        pointsService.rewardApprovalPoints(uploaderId, documentId);

        // Then
        assertEquals(55, user.getPoints()); // APPROVAL_BONUS = 5
        verify(pointsRecordRepository).save(argThat(record ->
            record.getSource() == PointsSource.APPROVED
        ));
    }

    @Test
    @DisplayName("TC-012: 文档获得高分评价应获得额外积分")
    void rewardRatingPoints_ShouldAddPointsForHighRating() {
        // Given
        Long uploaderId = 1L;
        Long documentId = 100L;
        User user = createTestUser(uploaderId, 100);
        when(userRepository.findById(uploaderId)).thenReturn(Optional.of(user));
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When - 评分>=4分触发奖励
        pointsService.rewardRatingPoints(uploaderId, documentId);

        // Then
        assertEquals(101, user.getPoints()); // RATING_BONUS = 1
    }

    @Test
    @DisplayName("TC-013: 积分每日获取应有上限限制")
    void rewardPoints_WhenDailyLimitReached_ShouldNotAddPoints() {
        // Given
        Long userId = 1L;
        User user = createTestUser(userId, 100);
        // 设置今日已获取50积分（已达上限）
        when(pointsRecordRepository.getTodayEarnedPoints(userId))
            .thenReturn(50);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When - 尝试奖励更多积分
        pointsService.rewardApprovalPoints(userId, 100L);

        // Then - 积分不应增加，因为已达每日上限
        assertEquals(100, user.getPoints());
    }

    @Test
    @DisplayName("TC-014: 下载文档时应扣除相应积分")
    void consumePoints_ShouldDeductPoints() {
        // Given
        Long userId = 1L;
        int initialPoints = 50;
        int cost = 10;
        User user = createTestUser(userId, initialPoints);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(pointsRecordRepository.save(any(PointsRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = pointsService.consumeDownloadPoints(userId, 100L, cost);

        // Then
        assertTrue(result);
        assertEquals(40, user.getPoints());
        verify(pointsRecordRepository).save(argThat(record ->
            record.getType() == PointsType.SPEND &&
            record.getPoints() == cost
        ));
    }

    @Test
    @DisplayName("TC-015: 积分不足时不应允许消费")
    void consumePoints_WhenInsufficientPoints_ShouldReturnFalse() {
        // Given
        Long userId = 1L;
        int cost = 10;
        User user = createTestUser(userId, 5); // 只有5积分，不足以支付10积分
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean result = pointsService.consumeDownloadPoints(userId, 100L, cost);

        // Then - consumeDownloadPoints 在积分不足时返回 false
        assertFalse(result);
        assertEquals(5, user.getPoints()); // 积分不应改变
    }
}
