package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.EmailVerification;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.exception.BusinessException;
import com.zhixiang.knowledge_platform.exception.ErrorCode;
import com.zhixiang.knowledge_platform.repository.EmailVerificationRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 邮箱验证服务
 * 处理邮箱验证码的生成、发送、验证等功能
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${zhixiang.email.verification.max-daily-requests:10}")
    private int maxDailyRequests;

    @Value("${zhixiang.email.verification.code-length:6}")
    private int codeLength;

    @Value("${zhixiang.email.verification.expire-minutes:10}")
    private int expireMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 发送注册验证码
     */
    public CompletableFuture<Boolean> sendRegistrationVerificationCode(String email, String username) {
        validateEmailRequest(email, EmailVerification.VerificationType.REGISTRATION);

        // 生成验证码
        String code = generateVerificationCode();

        // 删除旧的未验证记录
        emailVerificationRepository.deleteUnverifiedByEmailAndType(email, EmailVerification.VerificationType.REGISTRATION);

        // 创建新的验证记录
        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setCode(code);
        verification.setType(EmailVerification.VerificationType.REGISTRATION);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));

        emailVerificationRepository.save(verification);

        // 异步发送邮件
        return emailService.sendVerificationCode(email, code, username)
            .thenApply(sent -> {
                if (sent) {
                    log.info("注册验证码发送成功: {}", email);
                } else {
                    log.error("注册验证码发送失败: {}", email);
                }
                return sent;
            });
    }

    /**
     * 发送密码重置验证码
     */
    public CompletableFuture<Boolean> sendPasswordResetVerificationCode(String email) {
        // 检查用户是否存在
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册"));

        validateEmailRequest(email, EmailVerification.VerificationType.PASSWORD_RESET);

        // 生成验证码
        String code = generateVerificationCode();

        // 删除旧的未验证记录
        emailVerificationRepository.deleteUnverifiedByEmailAndType(email, EmailVerification.VerificationType.PASSWORD_RESET);

        // 创建新的验证记录
        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setCode(code);
        verification.setType(EmailVerification.VerificationType.PASSWORD_RESET);
        verification.setUserId(user.getId());
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));

        emailVerificationRepository.save(verification);

        // 异步发送邮件
        return emailService.sendVerificationCode(email, code, user.getUsername())
            .thenApply(sent -> {
                if (sent) {
                    log.info("密码重置验证码发送成功: {}", email);
                } else {
                    log.error("密码重置验证码发送失败: {}", email);
                }
                return sent;
            });
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String code, EmailVerification.VerificationType type) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
            .findFirstByEmailAndTypeOrderByCreatedAtDesc(email, type);

        if (verificationOpt.isEmpty()) {
            log.warn("验证记录不存在: email={}, type={}", email, type);
            return false;
        }

        EmailVerification verification = verificationOpt.get();

        // 检查是否已经验证过
        if (verification.getVerified()) {
            log.warn("验证码已使用: email={}, type={}", email, type);
            return false;
        }

        // 检查是否过期
        if (verification.isExpired()) {
            log.warn("验证码已过期: email={}, type={}", email, type);
            return false;
        }

        // 检查尝试次数
        if (verification.isExceededMaxAttempts()) {
            log.warn("验证码尝试次数超限: email={}, type={}", email, type);
            return false;
        }

        // 增加尝试次数
        verification.incrementAttempts();

        // 验证验证码
        if (!verification.getCode().equals(code)) {
            emailVerificationRepository.save(verification);
            log.warn("验证码不匹配: email={}, type={}, attempts={}", email, type, verification.getAttempts());
            return false;
        }

        // 验证成功
        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("验证码验证成功: email={}, type={}", email, type);
        return true;
    }

    /**
     * 检查邮箱是否已验证
     */
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email, EmailVerification.VerificationType type) {
        return emailVerificationRepository.existsByEmailAndTypeAndVerified(email, type, true);
    }

    /**
     * 获取邮箱的验证状态
     */
    @Transactional(readOnly = true)
    public EmailVerificationStatus getVerificationStatus(String email, EmailVerification.VerificationType type) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
            .findFirstByEmailAndTypeOrderByCreatedAtDesc(email, type);

        if (verificationOpt.isEmpty()) {
            return EmailVerificationStatus.NOT_REQUESTED;
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.getVerified()) {
            return EmailVerificationStatus.VERIFIED;
        }

        if (verification.isExpired()) {
            return EmailVerificationStatus.EXPIRED;
        }

        if (verification.isExceededMaxAttempts()) {
            return EmailVerificationStatus.EXCEEDED_ATTEMPTS;
        }

        return EmailVerificationStatus.PENDING;
    }

    /**
     * 验证请求合法性
     */
    private void validateEmailRequest(String email, EmailVerification.VerificationType type) {
        // 检查今日请求次数
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayRequests = emailVerificationRepository.countTodayVerificationsByEmail(email, startOfDay);

        if (todayRequests >= maxDailyRequests) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                String.format("今日验证码请求次数已达上限(%d次)，请明日再试", maxDailyRequests));
        }

        // 检查是否有未过期的验证码
        Optional<EmailVerification> existingVerification = emailVerificationRepository
            .findFirstByEmailAndTypeOrderByCreatedAtDesc(email, type);

        if (existingVerification.isPresent()) {
            EmailVerification verification = existingVerification.get();
            if (!verification.getVerified() && !verification.isExpired()) {
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), verification.getExpiresAt()).toMinutes();
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    String.format("验证码尚未过期，请%d分钟后再试", remainingMinutes + 1));
            }
        }
    }

    /**
     * 生成验证码
     */
    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 定时清理过期的验证记录
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredVerifications() {
        try {
            int deletedCount = emailVerificationRepository.deleteExpiredVerifications(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("清理过期验证记录: {} 条", deletedCount);
            }
        } catch (Exception e) {
            log.error("清理过期验证记录失败", e);
        }
    }

    /**
     * 获取用户的验证历史
     */
    @Transactional(readOnly = true)
    public List<EmailVerification> getUserVerificationHistory(Long userId) {
        return emailVerificationRepository.findRecentByUserId(userId);
    }

    /**
     * 发送邮箱更改验证码
     */
    public boolean sendEmailChangeVerificationCode(String newEmail, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "新邮箱不能与当前邮箱相同");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该邮箱已被其他用户使用");
        }

        validateEmailRequest(newEmail, EmailVerification.VerificationType.EMAIL_CHANGE);

        String code = generateVerificationCode();
        emailVerificationRepository.deleteUnverifiedByEmailAndType(newEmail, EmailVerification.VerificationType.EMAIL_CHANGE);

        EmailVerification verification = new EmailVerification();
        verification.setEmail(newEmail);
        verification.setCode(code);
        verification.setType(EmailVerification.VerificationType.EMAIL_CHANGE);
        verification.setUserId(userId);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));
        emailVerificationRepository.save(verification);

        boolean sent = emailService.sendVerificationCode(newEmail, code, user.getUsername()).join();
        if (!sent) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码发送失败，请稍后重试");
        }

        log.info("邮箱更改验证码发送成功: userId={}, newEmail={}", userId, newEmail);
        return true;
    }

    /**
     * 验证邮箱更改验证码
     */
    public boolean verifyEmailChangeCode(String newEmail, Long userId, String inputCode) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
            .findFirstByEmailAndTypeOrderByCreatedAtDesc(newEmail, EmailVerification.VerificationType.EMAIL_CHANGE);

        if (verificationOpt.isEmpty()) {
            log.warn("邮箱更改验证记录不存在: userId={}, newEmail={}", userId, newEmail);
            return false;
        }

        EmailVerification verification = verificationOpt.get();
        if (verification.getUserId() == null || !verification.getUserId().equals(userId)) {
            log.warn("邮箱更改验证记录用户不匹配: requestUserId={}, recordUserId={}, newEmail={}", userId, verification.getUserId(), newEmail);
            return false;
        }

        if (verification.getVerified()) {
            log.warn("邮箱更改验证码已使用: userId={}, newEmail={}", userId, newEmail);
            return false;
        }

        if (verification.isExpired()) {
            log.warn("邮箱更改验证码已过期: userId={}, newEmail={}", userId, newEmail);
            return false;
        }

        if (verification.isExceededMaxAttempts()) {
            log.warn("邮箱更改验证码尝试次数超限: userId={}, newEmail={}", userId, newEmail);
            return false;
        }

        verification.incrementAttempts();
        if (!verification.getCode().equals(inputCode)) {
            emailVerificationRepository.save(verification);
            log.warn("邮箱更改验证码不匹配: userId={}, newEmail={}, attempts={}", userId, newEmail, verification.getAttempts());
            return false;
        }

        if (userRepository.existsByEmail(newEmail)) {
            log.warn("邮箱更改目标邮箱已被占用: userId={}, newEmail={}", userId, newEmail);
            return false;
        }

        verification.markAsVerified();
        emailVerificationRepository.save(verification);
        log.info("邮箱更改验证码验证成功: userId={}, newEmail={}", userId, newEmail);
        return true;
    }

    /**
     * 邮箱验证状态枚举
     */
    public enum EmailVerificationStatus {
        NOT_REQUESTED("未请求"),
        PENDING("待验证"),
        VERIFIED("已验证"),
        EXPIRED("已过期"),
        EXCEEDED_ATTEMPTS("超过尝试次数");

        private final String description;

        EmailVerificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}