package com.zhixiang.knowledge_platform.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 认证控制器集成测试
 * 测试JWT双Token认证流程的端到端行为
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JWT双Token认证集成测试")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== TC-031 ~ TC-035: JWT认证流程集成测试 ==========

    @Test
    @DisplayName("TC-031: 使用正确凭据登录应返回双Token")
    void login_WithValidCredentials_ShouldReturnDualTokens() throws Exception {
        // Given
        String loginRequest = "{" +
                "\"username\": \"testuser\"," +
                "\"password\": \"testpassword123\"" +
                "}";

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        // 验证返回的Token结构
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String accessToken = jsonNode.get("data").get("token").asText();
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertNotEquals(accessToken, refreshToken, "Access Token和Refresh Token应不同");
        assertTrue(accessToken.split("\\.").length == 3, "Access Token应为JWT格式");
        assertTrue(refreshToken.split("\\.").length == 3, "Refresh Token应为JWT格式");
    }

    @Test
    @DisplayName("TC-032: 使用错误密码登录应返回401")
    void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given
        String loginRequest = "{" +
                "\"username\": \"testuser\"," +
                "\"password\": \"wrongpassword\"" +
                "}";

        // When & Then - 可能返回401或400
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 400, "应返回401或400");
                });
    }

    @Test
    @DisplayName("TC-033: 使用有效Refresh Token应刷新Access Token")
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() throws Exception {
        // Given - 先登录获取双Token
        String loginRequest = "{" +
                "\"username\": \"testuser\"," +
                "\"password\": \"testpassword123\"" +
                "}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        // When - 使用Refresh Token刷新
        String refreshRequest = "{\"refreshToken\": \"" + refreshToken + "\"}";

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        // Then
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String newAccessToken = refreshJson.get("data").get("token").asText();

        String originalAccessToken = jsonNode.get("data").get("token").asText();
        assertNotEquals(originalAccessToken, newAccessToken, "刷新后的Token应不同");
    }

    @Test
    @DisplayName("TC-034: 访问受保护资源应携带有效Token")
    void accessProtectedResource_WithValidToken_ShouldSucceed() throws Exception {
        // Given - 登录获取Token
        String loginRequest = "{" +
                "\"username\": \"testuser\"," +
                "\"password\": \"testpassword123\"" +
                "}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        String accessToken = jsonNode.get("data").get("token").asText();

        // When & Then - 使用Token访问受保护资源
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 成功200，或用户不存在返回404，但不应返回401/403
                    assertTrue(status == 200 || status == 404,
                            "应返回200或404，但不应返回401/403");
                });
    }

    @Test
    @DisplayName("TC-035: 访问受保护资源无Token应返回401")
    void accessProtectedResource_WithoutToken_ShouldReturn401() throws Exception {
        // When & Then - 不携带Token访问
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403,
                            "应返回401或403");
                });
    }
}
