package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.entity.Category;
import com.zhixiang.knowledge_platform.entity.Comment;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DownloadRecord;
import com.zhixiang.knowledge_platform.entity.EmailVerification;
import com.zhixiang.knowledge_platform.entity.Rating;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.CategoryRepository;
import com.zhixiang.knowledge_platform.repository.CommentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DownloadRecordRepository;
import com.zhixiang.knowledge_platform.repository.EmailVerificationRepository;
import com.zhixiang.knowledge_platform.repository.RatingRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import com.zhixiang.knowledge_platform.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DownloadRecordRepository downloadRecordRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        commentRepository.deleteAll();
        downloadRecordRepository.deleteAll();
        documentRepository.deleteAll();
        emailVerificationRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmailVerificationShouldCreateEmailChangeRecord() {
        User user = createUser("user_send", "user_send@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/send-email-verification?newEmail=new_email@example.com"),
            HttpMethod.POST,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body)
            .containsEntry("code", 0)
            .containsEntry("message", "验证码已发送到您的新邮箱");

        EmailVerification verification = emailVerificationRepository
            .findFirstByEmailAndTypeOrderByCreatedAtDesc("new_email@example.com", EmailVerification.VerificationType.EMAIL_CHANGE)
            .orElse(null);
        assertThat(verification).isNotNull();
        assertThat(verification.getUserId()).isEqualTo(user.getId());
        assertThat(verification.getVerified()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmailVerificationShouldFailWhenEmailAlreadyExists() {
        User user = createUser("user_exists", "user_exists@example.com", UserRole.USER, UserStatus.ACTIVE);
        createUser("other_user", "taken@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/send-email-verification?newEmail=taken@example.com"),
            HttpMethod.POST,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(body.get("code"))).isEqualTo("1001");
        assertThat(body).containsEntry("message", "该邮箱已被其他用户使用");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyEmailShouldUpdateUserEmail() {
        User user = createUser("user_verify", "user_verify@example.com", UserRole.USER, UserStatus.ACTIVE);

        EmailVerification verification = new EmailVerification();
        verification.setEmail("updated_email@example.com");
        verification.setCode("123456");
        verification.setType(EmailVerification.VerificationType.EMAIL_CHANGE);
        verification.setUserId(user.getId());
        verification.setVerified(false);
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/verify-email?newEmail=updated_email@example.com&verificationCode=123456"),
            HttpMethod.PUT,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body)
            .containsEntry("code", 0)
            .containsEntry("message", "邮箱更新成功");

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo("updated_email@example.com");

        EmailVerification updatedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getVerified()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyEmailShouldFailWhenCodeIsWrong() {
        User user = createUser("user_wrong_code", "user_wrong_code@example.com", UserRole.USER, UserStatus.ACTIVE);

        EmailVerification verification = new EmailVerification();
        verification.setEmail("wrong_code_email@example.com");
        verification.setCode("123456");
        verification.setType(EmailVerification.VerificationType.EMAIL_CHANGE);
        verification.setUserId(user.getId());
        verification.setVerified(false);
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/verify-email?newEmail=wrong_code_email@example.com&verificationCode=000000"),
            HttpMethod.PUT,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(body.get("code"))).isEqualTo("1005");
        assertThat(body).containsEntry("message", "验证码错误、已过期或已失效");

        EmailVerification updatedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getVerified()).isFalse();
        assertThat(updatedVerification.getAttempts()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAccountShouldSoftDeleteUser() {
        User user = createUser("user_delete", "user_delete@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/profile?password=password123"),
            HttpMethod.DELETE,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body)
            .containsEntry("code", 0)
            .containsEntry("message", "账户已注销");

        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAccountShouldInvalidateFurtherAuthenticatedAccess() {
        User user = createUser("user_delete_token", "user_delete_token@example.com", UserRole.USER, UserStatus.ACTIVE);
        HttpEntity<Void> request = authorizedRequest(user);

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
            url("/api/users/profile?password=password123"),
            HttpMethod.DELETE,
            request,
            Map.class
        );

        Map<String, Object> deleteBody = Objects.requireNonNull((Map<String, Object>) deleteResponse.getBody());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteBody)
            .containsEntry("code", 0)
            .containsEntry("message", "账户已注销");

        ResponseEntity<Map> profileResponse = restTemplate.exchange(
            url("/api/users/profile"),
            HttpMethod.GET,
            request,
            Map.class
        );

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteAccountShouldFailWhenPasswordIsWrong() {
        User user = createUser("user_wrong_password", "user_wrong_password@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/profile?password=wrong-password"),
            HttpMethod.DELETE,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(body.get("code"))).isEqualTo("1001");
        assertThat(body).containsEntry("message", "密码不正确");

        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchUsersShouldReturnOnlyActiveUsers() {
        User admin = createUser("admin_search", "admin_search@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        createUser("active_match", "active_match@example.com", UserRole.USER, UserStatus.ACTIVE);
        createUser("deleted_match", "deleted_match@example.com", UserRole.USER, UserStatus.DELETED);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/search?keyword=match&page=0&size=10"),
            HttpMethod.GET,
            authorizedRequest(admin),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).containsEntry("code", 0);
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).containsEntry("username", "active_match");
        assertThat(String.valueOf(data.get("total"))).isEqualTo("1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchUsersShouldReturnForbiddenForNonAdmin() {
        User user = createUser("normal_user", "normal_user@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/search?keyword=test&page=0&size=10"),
            HttpMethod.GET,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(String.valueOf(body.get("code"))).isEqualTo("3002");
    }

    @Test
    @SuppressWarnings("unchecked")
    void userStatisticsShouldReturnRealCounts() {
        User user = createUser("user_stats", "user_stats@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createApprovedDocument(user.getId(), category.getId(), "统计文档");
        createDownloadRecord(user.getId(), document.getId());
        createComment(user.getId(), document.getId(), "统计评论");
        createRating(user.getId(), document.getId(), 5);

        ResponseEntity<Map> response = restTemplate.exchange(
            url("/api/users/statistics"),
            HttpMethod.GET,
            authorizedRequest(user),
            Map.class
        );

        Map<String, Object> body = Objects.requireNonNull((Map<String, Object>) response.getBody());
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).containsEntry("code", 0);
        assertThat(String.valueOf(data.get("totalPoints"))).isEqualTo("100");
        assertThat(String.valueOf(data.get("totalUploads"))).isEqualTo("1");
        assertThat(String.valueOf(data.get("totalDownloads"))).isEqualTo("1");
        assertThat(String.valueOf(data.get("totalComments"))).isEqualTo("1");
        assertThat(String.valueOf(data.get("totalRatings"))).isEqualTo("1");
        assertThat(data.get("memberSince")).isNotNull();
    }

    private HttpEntity<Void> authorizedRequest(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateAccessToken(user));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private User createUser(String username, String email, UserRole role, UserStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setStatus(status);
        user.setPoints(100);
        user.setTotalPoints(100);
        return userRepository.save(user);
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setLevel(1);
        category.setSortOrder(0);
        category.setIsActive(true);
        category.setIsDeleted(false);
        return categoryRepository.save(category);
    }

    private Document createApprovedDocument(Long uploaderId, Long categoryId, String title) {
        Document document = new Document();
        document.setTitle(title);
        document.setDescription(title + " description");
        document.setFileName(title + ".pdf");
        document.setFilePath("/tmp/" + title + ".pdf");
        document.setFileSize(1024L);
        document.setFileType("application/pdf");
        document.setFileExtension("pdf");
        document.setCategoryId(categoryId);
        document.setUploaderId(uploaderId);
        document.setStatus(DocumentStatus.APPROVED);
        document.setDownloadCount(1);
        document.setViewCount(1);
        document.setRatingAverage(BigDecimal.valueOf(5.0));
        document.setRatingCount(1);
        document.setDownloadPoints(0);
        document.setApprovedAt(LocalDateTime.now());
        document.setApprovedBy(uploaderId);
        return documentRepository.save(document);
    }

    private void createDownloadRecord(Long userId, Long documentId) {
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setDocumentId(documentId);
        downloadRecord.setPointsCost(0);
        downloadRecord.setIpAddress("127.0.0.1");
        downloadRecord.setUserAgent("JUnit");
        downloadRecord.setDownloadTime(LocalDateTime.now());
        downloadRecordRepository.save(downloadRecord);
    }

    private void createComment(Long userId, Long documentId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setDocumentId(documentId);
        comment.setContent(content);
        comment.setIsDeleted(false);
        commentRepository.save(comment);
    }

    private void createRating(Long userId, Long documentId, int score) {
        Rating rating = new Rating();
        rating.setUserId(userId);
        rating.setDocumentId(documentId);
        rating.setScore(score);
        ratingRepository.save(rating);
    }
}
