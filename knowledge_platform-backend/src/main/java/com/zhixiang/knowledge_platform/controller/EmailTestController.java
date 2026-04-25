package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件测试控制器
 * 用于测试邮件发送功能
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "邮件测试", description = "邮件功能测试接口")
@SecurityRequirement(name = "bearerAuth")
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * 测试邮件连接
     */
    @GetMapping("/email-connection")
    @Operation(summary = "测试邮件服务连接")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> testEmailConnection() {
        try {
            boolean result = emailService.testEmailConnection();

            if (result) {
                return ResponseEntity.ok(ApiResponse.success("邮件服务连接正常"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("邮件服务连接失败"));
            }
        } catch (Exception e) {
            log.error("测试邮件连接失败", e);
            return ResponseEntity.ok(ApiResponse.error("测试失败: " + e.getMessage()));
        }
    }

    /**
     * 发送测试邮件
     */
    @PostMapping("/send-test-email")
    @Operation(summary = "发送测试邮件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> sendTestEmail(
            @Parameter(description = "收件人邮箱") @RequestParam String toEmail) {

        try {
            log.info("发送测试邮件到: {}", toEmail);

            CompletableFuture<Boolean> future = emailService.sendSimpleEmail(
                toEmail,
                "【知享平台】邮件服务测试",
                "这是一封来自知享平台的测试邮件。如果您收到此邮件，说明邮件服务配置成功！"
            );

            Boolean result = future.get(); // 等待异步结果

            if (result) {
                return ResponseEntity.ok(ApiResponse.success("测试邮件发送成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("测试邮件发送失败"));
            }
        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            return ResponseEntity.ok(ApiResponse.error("发送失败: " + e.getMessage()));
        }
    }

    /**
     * 发送测试验证码邮件
     */
    @PostMapping("/send-verification-code")
    @Operation(summary = "发送测试验证码邮件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> sendTestVerificationCode(
            @Parameter(description = "收件人邮箱") @RequestParam String toEmail,
            @Parameter(description = "用户名") @RequestParam(defaultValue = "测试用户") String username) {

        try {
            log.info("发送测试验证码邮件到: {}", toEmail);

            // 生成测试验证码
            String testCode = String.format("%06d", (int)(Math.random() * 1000000));

            CompletableFuture<Boolean> future = emailService.sendVerificationCode(
                toEmail,
                testCode,
                username
            );

            Boolean result = future.get(); // 等待异步结果

            if (result) {
                return ResponseEntity.ok(ApiResponse.success("验证码邮件发送成功，测试验证码: " + testCode));
            } else {
                return ResponseEntity.ok(ApiResponse.error("验证码邮件发送失败"));
            }
        } catch (Exception e) {
            log.error("发送测试验证码邮件失败", e);
            return ResponseEntity.ok(ApiResponse.error("发送失败: " + e.getMessage()));
        }
    }
}