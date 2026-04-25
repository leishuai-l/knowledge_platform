package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.dto.request.LoginRequest;
import com.zhixiang.knowledge_platform.dto.request.RefreshTokenRequest;
import com.zhixiang.knowledge_platform.dto.request.RegisterRequest;
import com.zhixiang.knowledge_platform.dto.request.ForgotPasswordRequest;
import com.zhixiang.knowledge_platform.dto.request.ResetPasswordRequest;
import com.zhixiang.knowledge_platform.dto.response.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.JwtResponse;
import com.zhixiang.knowledge_platform.dto.response.UserInfoResponse;
import com.zhixiang.knowledge_platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author leishuai
 * @version 0.0.1-SNAPSHOT
 * @since 2025-09-10
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "登录成功"));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<ApiResponse<JwtResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        try {
            JwtResponse response = authService.register(registerRequest);
            return ResponseEntity.ok(ApiResponse.success(response, "注册成功"));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {

        JwtResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "令牌刷新成功"));
    }

    @GetMapping("/validate")
    @Operation(summary = "验证令牌")
    public ResponseEntity<ApiResponse<UserInfoResponse>> validateToken(
            HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        UserInfoResponse response = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(response, "令牌有效"));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {

        String token = extractTokenFromRequest(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
    }

    @GetMapping("/check-username")
    @Operation(summary = "检查用户名")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(
            @Parameter(description = "用户名") @RequestParam String username) {

        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "用户名可用" : "用户名已被使用"));
    }

    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @Parameter(description = "邮箱地址") @RequestParam String email) {

        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "邮箱可用" : "邮箱已被使用"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "发送重置验证码")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        try {
            authService.sendPasswordResetCode(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null, "验证码已发送到您的邮箱"));
        } catch (IllegalArgumentException e) {
            log.warn("Forgot password failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Forgot password error", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("发送验证码失败，请稍后重试"));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        try {
            authService.resetPassword(
                    request.getEmail(),
                    request.getVerificationCode(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(ApiResponse.success(null, "密码重置成功"));
        } catch (IllegalArgumentException e) {
            log.warn("Reset password failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Reset password error", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("密码重置失败，请稍后重试"));
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}