package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${zhixiang.points.initial:100}")
    private Integer initialPoints;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security UserDetailsService接口实现
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 检查用户状态
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("用户状态异常: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .credentialsExpired(false)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 根据ID查找用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 创建新用户
     */
    @Transactional
    public User createUser(String username, String email, String password) {
        // 检查用户名和邮箱是否已存在
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPoints(initialPoints);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * 验证用户密码
     */
    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * 更新用户最后登录时间
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * 更新用户积分
     */
    @Transactional
    public void updateUserPoints(Long userId, Integer points) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPoints(points);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * 增加用户积分
     */
    @Transactional
    public void addUserPoints(Long userId, Integer points) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPoints(user.getPoints() + points);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * 扣除用户积分
     */
    @Transactional
    public boolean deductUserPoints(Long userId, Integer points) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPoints() >= points) {
                user.setPoints(user.getPoints() - points);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 检查邮箱是否被其他用户使用
        if (email != null && !email.equals(user.getEmail())) {
            if (existsByEmail(email)) {
                throw new IllegalArgumentException("邮箱已被使用");
            }
            user.setEmail(email);
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 修改用户密码
     */
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return true;
    }

    /**
     * 获取活跃用户列表
     */
    public List<User> getActiveUsers() {
        return userRepository.findByStatus(UserStatus.ACTIVE);
    }

    /**
     * 锁定用户
     */
    @Transactional
    public void lockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.LOCKED);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.warn("Locked user: {}", user.getUsername());
        });
    }

    /**
     * 解锁用户
     */
    @Transactional
    public void unlockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.ACTIVE);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Unlocked user: {}", user.getUsername());
        });
    }

    /**
     * 软删除用户
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(UserStatus.DELETED);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.warn("Soft deleted user: {}", user.getUsername());
        });
    }

    /**
     * 搜索用户（分页）
     */
    public org.springframework.data.domain.Page<User> searchUsers(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findByStatus(UserStatus.ACTIVE, pageable);
        }
        return userRepository.searchUsers(keyword.trim(), UserStatus.ACTIVE, pageable);
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateUserAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("更新用户头像，用户ID: {}, 头像路径: {}", userId, avatarUrl);
    }

    /**
     * 重置密码（管理员功能）
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 生成6位随机密码
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset for user: {}", user.getUsername());
        return newPassword;
    }

    /**
     * 重置密码为指定密码（忘记密码功能）
     */
    @Transactional
    public void resetPasswordWithNewPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset with new password for user: {}", user.getUsername());
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }

    /**
     * 获取用户总数
     */
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * 获取活跃用户总数
     */
    public long getActiveUserCount() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    /**
     * 批量更新用户状态
     */
    @Transactional
    public void batchUpdateUserStatus(List<Long> userIds, UserStatus status) {
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> {
            user.setStatus(status);
            user.setUpdatedAt(LocalDateTime.now());
        });
        userRepository.saveAll(users);
        log.info("Batch updated {} users to status: {}", users.size(), status);
    }

    /**
     * 获取用户角色为管理员的用户列表
     */
    public List<User> getAdminUsers() {
        return userRepository.findByRole(UserRole.ADMIN);
    }

    /**
     * 提升用户为管理员
     */
    @Transactional
    public void promoteToAdmin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRole(UserRole.ADMIN);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Promoted user {} to ADMIN", user.getUsername());
        });
    }

    /**
     * 降级管理员为普通用户
     */
    @Transactional
    public void demoteFromAdmin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRole(UserRole.USER);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Demoted user {} from ADMIN", user.getUsername());
        });
    }

    /**
     * 更新用户实体（用于直接保存User对象）
     */
    @Transactional
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 获取用户总数
     */
    public Long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * 获取最近活跃用户数（24小时内登录）
     */
    public Long getRecentActiveUserCount() {
        // 获取最近24小时内登录的用户数
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return userRepository.countByLastLoginTimeAfter(since);
    }
}