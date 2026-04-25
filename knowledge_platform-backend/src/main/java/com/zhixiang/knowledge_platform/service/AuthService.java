package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.LoginRequest;
import com.zhixiang.knowledge_platform.dto.request.RegisterRequest;
import com.zhixiang.knowledge_platform.dto.response.JwtResponse;
import com.zhixiang.knowledge_platform.dto.response.UserInfoResponse;
import com.zhixiang.knowledge_platform.entity.EmailVerification;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    @Value("${zhixiang.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${zhixiang.security.account-lock-minutes:30}")
    private int accountLockMinutes;

    public AuthService(AuthenticationManager authenticationManager,
                      UserService userService,
                      JwtUtil jwtUtil,
                      EmailVerificationService emailVerificationService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * 用户登录
     */
    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        // 先检查用户是否存在
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        // 检查账户是否被锁定
        if (user.isAccountLocked()) {
            log.warn("Login attempt on locked account: {}", user.getUsername());
            throw new BadCredentialsException("账户已被锁定，请稍后再试");
        }

        try {
            // 验证用户凭据
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 登录成功，重置失败次数
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedLoginAttempts();
                userService.updateUser(user);
            }

            // 生成JWT令牌
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            // 更新最后登录时间
            userService.updateLastLogin(user.getId());

            // 构造响应
            JwtResponse response = new JwtResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(24 * 60 * 60); // 24小时，单位秒
            response.setUser(UserInfoResponse.fromEntity(user));

            log.info("User {} logged in successfully", user.getUsername());
            return response;

        } catch (AuthenticationException e) {
            // 登录失败，增加失败次数
            handleLoginFailure(user);

            log.warn("Login failed for user: {} (attempt {})",
                    loginRequest.getUsername(), user.getFailedLoginAttempts());
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    /**
     * 处理登录失败
     */
    @Transactional
    public void handleLoginFailure(User user) {
        user.incrementFailedLoginAttempts();

        if (user.shouldLockAccount(maxLoginAttempts)) {
            user.lockAccount(accountLockMinutes);
            log.warn("Account locked for user: {} after {} failed attempts",
                    user.getUsername(), user.getFailedLoginAttempts());
        }

        userService.updateUser(user);
    }

    /**
     * 用户注册
     */
    @Transactional
    public JwtResponse register(RegisterRequest registerRequest) {
        // 验证邮箱验证码
        boolean isVerified = emailVerificationService.verifyCode(
                registerRequest.getEmail(),
                registerRequest.getVerificationCode(),
                EmailVerification.VerificationType.REGISTRATION
        );

        if (!isVerified) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        // 创建用户
        User user = userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
        );

        // 生成JWT令牌
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // 构造响应
        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(24 * 60 * 60); // 24小时，单位秒
        response.setUser(UserInfoResponse.fromEntity(user));

        log.info("User {} registered successfully", user.getUsername());
        return response;
    }

    /**
     * 刷新令牌
     */
    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BadCredentialsException("刷新令牌无效");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));

        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(user);

        // 构造响应
        JwtResponse response = new JwtResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken); // 保持原刷新令牌
        response.setTokenType("Bearer");
        response.setExpiresIn(24 * 60 * 60); // 24小时，单位秒
        response.setUser(UserInfoResponse.fromEntity(user));

        log.debug("Token refreshed for user: {}", username);
        return response;
    }

    /**
     * 验证令牌
     */
    public UserInfoResponse validateToken(String token) {
        if (!jwtUtil.validateAccessToken(token)) {
            throw new BadCredentialsException("令牌无效");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));

        return UserInfoResponse.fromEntity(user);
    }

    /**
     * 登出（客户端处理，服务端暂时不需要特殊逻辑）
     */
    public void logout(String token) {
        // JWT是无状态的，登出主要由客户端处理（删除本地存储的token）
        // 如果需要服务端黑名单功能，可以在这里添加token到黑名单
        String username = jwtUtil.getUsernameFromToken(token);
        log.info("User {} logged out", username);
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        return !userService.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用
     */
    public boolean isEmailAvailable(String email) {
        return !userService.existsByEmail(email);
    }

    /**
     * 发送密码重置验证码
     */
    @Transactional
    public void sendPasswordResetCode(String email) {
        // 检查用户是否存在
        if (!userService.existsByEmail(email)) {
            throw new IllegalArgumentException("该邮箱未注册");
        }

        // 发送密码重置验证码
        emailVerificationService.sendPasswordResetVerificationCode(email)
                .whenComplete((sent, throwable) -> {
                    if (throwable != null) {
                        log.error("发送密码重置验证码异步处理失败", throwable);
                    }
                });

        log.info("Password reset verification code sent to: {}", email);
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(String email, String verificationCode, String newPassword) {
        // 验证验证码
        boolean isVerified = emailVerificationService.verifyCode(
                email,
                verificationCode,
                EmailVerification.VerificationType.PASSWORD_RESET
        );

        if (!isVerified) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        // 重置密码
        userService.resetPasswordWithNewPassword(email, newPassword);

        log.info("Password reset successfully for email: {}", email);
    }
}