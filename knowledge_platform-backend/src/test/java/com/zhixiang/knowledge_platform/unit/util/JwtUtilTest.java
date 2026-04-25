package com.zhixiang.knowledge_platform.unit.util;

import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具类单元测试
 * 验证双Token认证机制：AccessToken(24h) + RefreshToken(3天)
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 设置JwtUtil的@Value字段
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "test-secret-key-for-jwt-unit-testing-must-be-long-enough-256bits");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 86400000L); // 24小时 = 86400000ms
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpirationMs", 259200000L); // 3天 = 259200000ms
        ReflectionTestUtils.setField(jwtUtil, "officePreviewTokenExpirationMs", 300000L);
    }

    private User createTestUser(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(UserRole.valueOf(role));
        user.setEmail(username + "@test.com");
        return user;
    }

    @Test
    @DisplayName("TC-001: 生成Access Token应包含用户信息")
    void generateAccessToken_ShouldContainUserInfo() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");

        // When
        String token = jwtUtil.generateAccessToken(user);

        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT应包含3部分");
        assertEquals("testUser", jwtUtil.getUsernameFromToken(token));
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("TC-002: Access Token有效期应为24小时")
    void generateAccessToken_ShouldHave24HourExpiration() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");

        // When
        String token = jwtUtil.generateAccessToken(user);

        // Then
        Date expiration = jwtUtil.getExpirationDateFromToken(token);
        long diff = expiration.getTime() - System.currentTimeMillis();
        // 24小时 = 86400000ms，允许1分钟误差
        assertTrue(diff > 86340000L && diff <= 86400000L,
                "Access Token有效期应为24小时，实际: " + diff + "ms");
    }

    @Test
    @DisplayName("TC-003: Refresh Token有效期应为3天")
    void generateRefreshToken_ShouldHave3DayExpiration() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");

        // When
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Then
        Date expiration = jwtUtil.getExpirationDateFromToken(refreshToken);
        long diff = expiration.getTime() - System.currentTimeMillis();
        // 3天 = 259200000ms，允许1分钟误差
        assertTrue(diff > 259140000L && diff <= 259200000L,
                "Refresh Token有效期应为3天，实际: " + diff + "ms");
    }

    @Test
    @DisplayName("TC-004: Refresh Token类型声明应为refresh")
    void generateRefreshToken_ShouldContainRefreshType() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");

        // When
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Then
        assertTrue(jwtUtil.validateRefreshToken(refreshToken), "应为Refresh Token");
        assertFalse(jwtUtil.validateAccessToken(refreshToken), "不是Access Token");
    }

    @Test
    @DisplayName("TC-005: Access Token类型声明应为access")
    void generateAccessToken_ShouldContainAccessType() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");

        // When
        String accessToken = jwtUtil.generateAccessToken(user);

        // Then
        assertTrue(jwtUtil.validateAccessToken(accessToken), "应为Access Token");
        assertFalse(jwtUtil.validateRefreshToken(accessToken), "不是Refresh Token");
    }

    @Test
    @DisplayName("TC-006: 过期Token应验证失败")
    void validateToken_WhenExpired_ShouldReturnFalse() throws InterruptedException {
        // Given: 创建一个极短有效期的JwtUtil
        JwtUtil shortLivedJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedJwtUtil, "jwtSecret", "test-secret-key-for-jwt-unit-testing-must-be-long-enough-256bits");
        ReflectionTestUtils.setField(shortLivedJwtUtil, "jwtExpirationMs", 10L); // 10毫秒
        ReflectionTestUtils.setField(shortLivedJwtUtil, "jwtRefreshExpirationMs", 10L);
        ReflectionTestUtils.setField(shortLivedJwtUtil, "officePreviewTokenExpirationMs", 10L);

        User user = createTestUser(1L, "test", "USER");
        String token = shortLivedJwtUtil.generateAccessToken(user);

        // When: 等待Token过期
        Thread.sleep(100);

        // Then
        assertFalse(shortLivedJwtUtil.validateToken(token), "过期Token应验证失败");
    }

    @Test
    @DisplayName("TC-007: 无效Token应抛出异常")
    void validateToken_WithInvalidFormat_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.format";

        // Then
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    @DisplayName("TC-008: 使用Refresh Token刷新Access Token应生成新Token")
    void refreshAccessToken_WithValidRefreshToken_ShouldGenerateNewAccessToken() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user);
        String originalAccessToken = jwtUtil.generateAccessToken(user);

        // When
        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken, user);

        // Then
        assertNotNull(newAccessToken);
        assertNotEquals(originalAccessToken, newAccessToken, "新Token应与原Token不同");
        assertEquals("testUser", jwtUtil.getUsernameFromToken(newAccessToken));
    }

    @Test
    @DisplayName("TC-009: 篡改后的Token应验证失败")
    void validateToken_WithTamperedToken_ShouldReturnFalse() {
        // Given
        User user = createTestUser(1L, "testUser", "USER");
        String token = jwtUtil.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        // Then
        assertFalse(jwtUtil.validateToken(tamperedToken), "篡改Token应验证失败");
    }
}
