package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.entity.EmailVerification;
import com.zhixiang.knowledge_platform.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 邮箱验证控制器
 * 处理邮箱验证相关的API请求
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "邮箱验证")
@Validated
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send-registration-code")
    @Operation(summary = "发送注册验证码")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> sendRegistrationCode(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "用户名") @RequestParam @NotBlank(message = "用户名不能为空") String username,
            HttpServletRequest request) {

        log.info("收到注册验证码请求: email={}, username={}, ip={}", email, username, getClientIp(request));

        return emailVerificationService.sendRegistrationVerificationCode(email, username)
            .thenApply(sent -> {
                if (sent) {
                    return ResponseEntity.ok(ApiResponse.<Void>success());
                } else {
                    return ResponseEntity.ok(ApiResponse.<Void>error("验证码发送失败，请稍后重试"));
                }
            })
            .exceptionally(ex -> {
                log.error("发送注册验证码异常: email={}", email, ex);
                return ResponseEntity.ok(ApiResponse.<Void>error("验证码发送失败: " + ex.getMessage()));
            });
    }

    @PostMapping("/send-reset-code")
    @Operation(summary = "发送重置验证码")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> sendPasswordResetCode(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            HttpServletRequest request) {

        log.info("收到密码重置验证码请求: email={}, ip={}", email, getClientIp(request));

        return emailVerificationService.sendPasswordResetVerificationCode(email)
            .thenApply(sent -> {
                if (sent) {
                    return ResponseEntity.ok(ApiResponse.<Void>success());
                } else {
                    return ResponseEntity.ok(ApiResponse.<Void>error("验证码发送失败，请稍后重试"));
                }
            })
            .exceptionally(ex -> {
                log.error("发送密码重置验证码异常: email={}", email, ex);
                return ResponseEntity.ok(ApiResponse.<Void>error("验证码发送失败: " + ex.getMessage()));
            });
    }

    @PostMapping("/verify-registration-code")
    @Operation(summary = "验证注册验证码")
    public ResponseEntity<ApiResponse<Void>> verifyRegistrationCode(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "验证码") @RequestParam @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字") String code,
            HttpServletRequest request) {

        log.info("收到注册验证码验证请求: email={}, code={}, ip={}", email, code, getClientIp(request));

        try {
            boolean verified = emailVerificationService.verifyCode(email, code, EmailVerification.VerificationType.REGISTRATION);

            if (verified) {
                log.info("注册验证码验证成功: email={}", email);
                return ResponseEntity.ok(ApiResponse.success());
            } else {
                log.warn("注册验证码验证失败: email={}, code={}", email, code);
                return ResponseEntity.ok(ApiResponse.error("验证码错误或已过期"));
            }
        } catch (Exception e) {
            log.error("验证注册验证码异常: email={}, code={}", email, code, e);
            return ResponseEntity.ok(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-code")
    @Operation(summary = "验证重置验证码")
    public ResponseEntity<ApiResponse<Void>> verifyPasswordResetCode(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "验证码") @RequestParam @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字") String code,
            HttpServletRequest request) {

        log.info("收到密码重置验证码验证请求: email={}, code={}, ip={}", email, code, getClientIp(request));

        try {
            boolean verified = emailVerificationService.verifyCode(email, code, EmailVerification.VerificationType.PASSWORD_RESET);

            if (verified) {
                log.info("密码重置验证码验证成功: email={}", email);
                return ResponseEntity.ok(ApiResponse.success());
            } else {
                log.warn("密码重置验证码验证失败: email={}, code={}", email, code);
                return ResponseEntity.ok(ApiResponse.error("验证码错误或已过期"));
            }
        } catch (Exception e) {
            log.error("验证密码重置验证码异常: email={}, code={}", email, code, e);
            return ResponseEntity.ok(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    @GetMapping("/verification-status")
    @Operation(summary = "获取验证状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVerificationStatus(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "验证类型") @RequestParam String type) {

        try {
            EmailVerification.VerificationType verificationType = EmailVerification.VerificationType.valueOf(type.toUpperCase());
            EmailVerificationService.EmailVerificationStatus status = emailVerificationService.getVerificationStatus(email, verificationType);

            Map<String, Object> result = Map.of(
                "email", email,
                "type", type,
                "status", status.name(),
                "description", status.getDescription(),
                "verified", status == EmailVerificationService.EmailVerificationStatus.VERIFIED
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IllegalArgumentException e) {
            log.warn("无效的验证类型: {}", type);
            return ResponseEntity.ok(ApiResponse.error("无效的验证类型"));
        } catch (Exception e) {
            log.error("获取验证状态异常: email={}, type={}", email, type, e);
            return ResponseEntity.ok(ApiResponse.error("获取验证状态失败: " + e.getMessage()));
        }
    }

    @GetMapping("/is-verified")
    @Operation(summary = "检查是否已验证")
    public ResponseEntity<ApiResponse<Boolean>> isEmailVerified(
            @Parameter(description = "邮箱地址") @RequestParam @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "验证类型") @RequestParam String type) {

        try {
            EmailVerification.VerificationType verificationType = EmailVerification.VerificationType.valueOf(type.toUpperCase());
            boolean verified = emailVerificationService.isEmailVerified(email, verificationType);

            return ResponseEntity.ok(ApiResponse.success(verified));

        } catch (IllegalArgumentException e) {
            log.warn("无效的验证类型: {}", type);
            return ResponseEntity.ok(ApiResponse.error("无效的验证类型"));
        } catch (Exception e) {
            log.error("检查邮箱验证状态异常: email={}, type={}", email, type, e);
            return ResponseEntity.ok(ApiResponse.error("检查验证状态失败: " + e.getMessage()));
        }
    }

    @GetMapping("/verification-types")
    @Operation(summary = "获取验证类型")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVerificationTypes() {
        Map<String, String> types = Map.of(
            "REGISTRATION", EmailVerification.VerificationType.REGISTRATION.getDescription(),
            "PASSWORD_RESET", EmailVerification.VerificationType.PASSWORD_RESET.getDescription(),
            "EMAIL_CHANGE", EmailVerification.VerificationType.EMAIL_CHANGE.getDescription()
        );

        return ResponseEntity.ok(ApiResponse.success(types));
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}