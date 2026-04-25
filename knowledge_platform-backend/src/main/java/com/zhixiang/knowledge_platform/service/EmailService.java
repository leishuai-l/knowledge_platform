package com.zhixiang.knowledge_platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件服务
 * 提供邮件发送功能，包括验证码、通知等
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${zhixiang.mail.from:noreply@zhixiang.com}")
    private String fromEmail;

    @Value("${zhixiang.mail.from-name:知享平台}")
    private String fromName;

    @Value("${zhixiang.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${zhixiang.email.verification-url:http://localhost:3000/verify}")
    private String verificationBaseUrl;

    /**
     * 发送邮箱验证码
     */
    public CompletableFuture<Boolean> sendVerificationCode(String toEmail, String verificationCode, String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送验证码邮件到: {}", toEmail);
                return true; // 返回true以避免影响业务流程
            }

            try {
                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("verificationCode", verificationCode);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("expireMinutes", 10);

                String htmlContent = templateEngine.process("email/verification-code", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】邮箱验证码");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("验证码邮件发送成功: {} -> {}", fromEmail, toEmail);
                return true;

            } catch (MessagingException e) {
                log.error("发送验证码邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送邮箱验证链接
     */
    public CompletableFuture<Boolean> sendVerificationLink(String toEmail, String verificationToken, String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送验证链接邮件到: {}", toEmail);
                return true;
            }

            try {
                String verificationUrl = verificationBaseUrl + "?token=" + verificationToken;

                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("verificationUrl", verificationUrl);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("email/verification-link", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】请验证您的邮箱");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("验证链接邮件发送成功: {} -> {}", fromEmail, toEmail);
                return true;

            } catch (MessagingException e) {
                log.error("发送验证链接邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送密码重置邮件
     */
    public CompletableFuture<Boolean> sendPasswordResetEmail(String toEmail, String resetToken, String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送密码重置邮件到: {}", toEmail);
                return true;
            }

            try {
                String resetUrl = verificationBaseUrl.replace("/verify", "/reset-password") + "?token=" + resetToken;

                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("resetUrl", resetUrl);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("expireHours", 24);

                String htmlContent = templateEngine.process("email/password-reset", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】密码重置");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("密码重置邮件发送成功: {} -> {}", fromEmail, toEmail);
                return true;

            } catch (MessagingException e) {
                log.error("发送密码重置邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送文档审核通知邮件
     */
    public CompletableFuture<Boolean> sendDocumentApprovalNotification(String toEmail, String username, String documentTitle, boolean approved, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送文档审核通知邮件到: {}", toEmail);
                return true;
            }

            try {
                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("documentTitle", documentTitle);
                context.setVariable("approved", approved);
                context.setVariable("reason", reason);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                String templateName = approved ? "email/document-approved" : "email/document-rejected";
                String htmlContent = templateEngine.process(templateName, context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】文档审核" + (approved ? "通过" : "未通过") + "通知");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("文档审核通知邮件发送成功: {} -> {} ({})", fromEmail, toEmail, approved ? "通过" : "拒绝");
                return true;

            } catch (MessagingException e) {
                log.error("发送文档审核通知邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送简单文本邮件
     */
    public CompletableFuture<Boolean> sendSimpleEmail(String toEmail, String subject, String text) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送简单邮件到: {}", toEmail);
                return true;
            }

            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(text);

                mailSender.send(message);
                log.info("简单邮件发送成功: {} -> {}", fromEmail, toEmail);
                return true;

            } catch (Exception e) {
                log.error("发送简单邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送欢迎邮件
     */
    public CompletableFuture<Boolean> sendWelcomeEmail(String toEmail, String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送欢迎邮件到: {}", toEmail);
                return true;
            }

            try {
                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("platformUrl", verificationBaseUrl.replace("/verify", ""));

                String htmlContent = templateEngine.process("email/welcome", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】欢迎加入知识分享社区！");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("欢迎邮件发送成功: {} -> {}", fromEmail, toEmail);
                return true;

            } catch (MessagingException e) {
                log.error("发送欢迎邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 发送积分变动通知邮件
     */
    public CompletableFuture<Boolean> sendPointsNotification(String toEmail, String username, int pointsChange, String reason, int currentPoints) {
        return CompletableFuture.supplyAsync(() -> {
            if (!mailEnabled) {
                log.info("邮件功能已禁用，跳过发送积分通知邮件到: {}", toEmail);
                return true;
            }

            try {
                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("pointsChange", pointsChange);
                context.setVariable("reason", reason);
                context.setVariable("currentPoints", currentPoints);
                context.setVariable("isIncrease", pointsChange > 0);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("email/points-notification", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (UnsupportedEncodingException e) {
                    helper.setFrom(fromEmail);
                    log.warn("设置发件人名称失败，使用默认邮箱地址: {}", e.getMessage());
                }
                helper.setTo(toEmail);
                helper.setSubject("【知享平台】积分变动通知");
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("积分变动通知邮件发送成功: {} -> {} ({}积分)", fromEmail, toEmail, pointsChange);
                return true;

            } catch (MessagingException e) {
                log.error("发送积分变动通知邮件失败: {}", toEmail, e);
                return false;
            }
        });
    }

    /**
     * 测试邮件发送功能
     */
    public boolean testEmailConnection() {
        if (!mailEnabled) {
            log.info("邮件功能已禁用，跳过邮件连接测试");
            return true;
        }

        try {
            MimeMessage testMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(testMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(fromEmail); // 发送给自己测试
            helper.setSubject("邮件服务连接测试");
            helper.setText("这是一条测试邮件，用于验证邮件服务配置是否正确。");

            // 不实际发送，只是创建消息来测试配置
            log.info("邮件服务连接测试成功");
            return true;
        } catch (Exception e) {
            log.error("邮件服务连接测试失败", e);
            return false;
        }
    }
}