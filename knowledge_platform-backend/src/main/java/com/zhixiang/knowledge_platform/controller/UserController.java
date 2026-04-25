package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.UserInfoResponse;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.CommentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DownloadRecordRepository;
import com.zhixiang.knowledge_platform.repository.RatingRepository;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.EmailVerificationService;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import com.zhixiang.knowledge_platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户个人信息管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final FileUploadService fileUploadService;
    private final EmailVerificationService emailVerificationService;
    private final DocumentRepository documentRepository;
    private final DownloadRecordRepository downloadRecordRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserInfoResponse userInfo = UserInfoResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-verification")
    @Operation(summary = "发送邮箱验证码")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @Parameter(description = "新邮箱") @RequestParam String newEmail,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        boolean sent = emailVerificationService.sendEmailChangeVerificationCode(newEmail, userId);

        if (sent) {
            return ResponseEntity.ok(ApiResponse.success(null, "验证码已发送到您的新邮箱"));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(1004, "发送验证码失败，请稍后重试"));
    }

    /**
     * 验证邮箱并更新
     */
    @PutMapping("/verify-email")
    @Operation(summary = "验证邮箱并更新")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> verifyAndUpdateEmail(
            @Parameter(description = "新邮箱") @RequestParam String newEmail,
            @Parameter(description = "验证码") @RequestParam String verificationCode,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);

        if (!emailVerificationService.verifyEmailChangeCode(newEmail, userId, verificationCode)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(1005, "验证码错误、已过期或已失效"));
        }

        User updatedUser = userService.updateUser(userId, newEmail);
        UserInfoResponse userInfo = UserInfoResponse.fromEntity(updatedUser);

        return ResponseEntity.ok(ApiResponse.success(userInfo, "邮箱更新成功"));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "旧密码") @RequestParam String oldPassword,
            @Parameter(description = "新密码") @RequestParam String newPassword,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        boolean success = userService.changePassword(userId, oldPassword, newPassword);

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(1001, "旧密码不正确"));
        }
    }

    /**
     * 上传用户头像
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户头像")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
            FileUploadService.FileUploadResult uploadResult = fileUploadService.uploadAvatar(file, userId);

            // 更新用户头像URL
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String avatarUrl = uploadResult.getRelativePath();
            userService.updateUserAvatar(userId, avatarUrl);

            Map<String, String> result = Map.of(
                "avatarUrl", avatarUrl,
                "message", "头像上传成功"
            );

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(1002, "头像上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计信息")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatistics(HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        long totalUploads = documentRepository.countByUploaderIdAndStatus(userId, DocumentStatus.APPROVED);
        long totalDownloads = downloadRecordRepository.countByUserId(userId);
        long totalComments = commentRepository.countByUserIdAndIsDeletedFalse(userId);
        long totalRatings = ratingRepository.countRatingsByUserId(userId);

        Map<String, Object> statistics = Map.of(
            "totalPoints", user.getPoints(),
            "totalUploads", totalUploads,
            "totalDownloads", totalDownloads,
            "totalComments", totalComments,
            "totalRatings", totalRatings,
            "memberSince", user.getCreatedAt()
        );

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取指定用户的公开信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取指定用户的公开信息")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserPublicInfo(
            @Parameter(description = "用户ID") @PathVariable Long id) {

        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 只返回公开信息，隐藏敏感信息
        UserInfoResponse userInfo = UserInfoResponse.fromEntity(user);
        // 清除敏感信息
        userInfo.setEmail(null);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 搜索用户（仅管理员）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserInfoResponse>>> searchUsers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<User> userPage = userService.searchUsers(keyword, pageable);
        java.util.List<UserInfoResponse> userList = userPage.getContent().stream()
                .map(UserInfoResponse::fromEntity)
                .toList();

        PageResponse<UserInfoResponse> result = PageResponse.fromPage(userPage, userList);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除用户账户（软删除）
     */
    @DeleteMapping("/profile")
    @Operation(summary = "删除用户账户")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Parameter(description = "确认密码") @RequestParam String password,
            HttpServletRequest request) {

        Long userId = JwtAuthenticationFilter.getCurrentUserId(request);
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!userService.validatePassword(user, password)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(1001, "密码不正确"));
        }

        userService.softDeleteUser(userId);
        log.warn("用户已执行账户删除: userId={}, username={}", userId, user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(null, "账户已注销"));
    }
}