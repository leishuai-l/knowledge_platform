package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.entity.Category;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.CategoryRepository;
import com.zhixiang.knowledge_platform.repository.CopyrightReportRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DownloadRecordRepository;
import com.zhixiang.knowledge_platform.repository.PointsRecordRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import com.zhixiang.knowledge_platform.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentAccessControllerTest {

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
    private CopyrightReportRepository copyrightReportRepository;

    @Autowired
    private DownloadRecordRepository downloadRecordRepository;

    @Autowired
    private PointsRecordRepository pointsRecordRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        copyrightReportRepository.deleteAll();
        downloadRecordRepository.deleteAll();
        pointsRecordRepository.deleteAll();
        documentRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void guestShouldNotAccessPendingDocumentDetailPreviewOrDownload() throws Exception {
        User uploader = createUser("pending_owner", "pending_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "待审核文档", DocumentStatus.PENDING, "txt");
        writeDocumentFile(document, "pending content");

        ResponseEntity<Map<String, Object>> detailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId()),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> detailBody = Objects.requireNonNull(detailResponse.getBody());

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(detailBody.get("code"))).isEqualTo("1");
        assertThat(detailBody).containsEntry("message", "文档不存在或无权访问");

        ResponseEntity<Map<String, Object>> guestPreviewResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-content"),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(guestPreviewResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, Object>> guestDownloadResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/download"),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(guestDownloadResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploaderShouldAccessPendingDocumentDetailPreviewAndDownload() throws Exception {
        User uploader = createUser("uploader_access", "uploader_access@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "上传者待审文档", DocumentStatus.PENDING, "txt");
        writeDocumentFile(document, "owner can read");

        ResponseEntity<Map> detailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId()),
            HttpMethod.GET,
            authorizedRequest(uploader),
            Map.class
        );
        Map<String, Object> detailBody = Objects.requireNonNull(detailResponse.getBody());

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailBody).containsEntry("code", 0);

        ResponseEntity<String> previewResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-content"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            String.class
        );
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).isEqualTo("owner can read");

        ResponseEntity<Resource> downloadResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/download"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            Resource.class
        );
        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(Objects.requireNonNull(downloadResponse.getBody()).getContentAsByteArray())
            .isEqualTo("owner can read".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminShouldAccessPendingDocumentDetailPreviewAndDownload() throws Exception {
        User uploader = createUser("pending_uploader", "pending_uploader@example.com", UserRole.USER, UserStatus.ACTIVE);
        User admin = createUser("admin_access", "admin_access@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "管理员待审文档", DocumentStatus.PENDING, "txt");
        writeDocumentFile(document, "admin can read");

        ResponseEntity<Map> detailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId()),
            HttpMethod.GET,
            authorizedRequest(admin),
            Map.class
        );
        Map<String, Object> detailBody = Objects.requireNonNull(detailResponse.getBody());

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailBody).containsEntry("code", 0);

        ResponseEntity<String> previewResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-content"),
            HttpMethod.GET,
            authorizedRequest(admin),
            String.class
        );
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).isEqualTo("admin can read");

        ResponseEntity<Resource> downloadResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/download"),
            HttpMethod.GET,
            authorizedRequest(admin),
            Resource.class
        );
        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloadResponse.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(Objects.requireNonNull(downloadResponse.getBody()).getContentAsByteArray())
            .isEqualTo("admin can read".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvedDocumentShouldRequireAuthenticatedDownloadButAllowPublicDetail() throws Exception {
        User uploader = createUser("approved_uploader", "approved_uploader@example.com", UserRole.USER, UserStatus.ACTIVE);
        User downloader = createUser("approved_user", "approved_user@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "公开文档", DocumentStatus.APPROVED, "txt");
        writeDocumentFile(document, "approved content");

        ResponseEntity<Map> guestDetailResponse = restTemplate.getForEntity(url("/api/documents/" + document.getId()), Map.class);
        Map<String, Object> guestDetailBody = Objects.requireNonNull(guestDetailResponse.getBody());
        assertThat(guestDetailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(guestDetailBody).containsEntry("code", 0);

        ResponseEntity<Map> guestDownloadResponse = restTemplate.getForEntity(url("/api/documents/" + document.getId() + "/download"), Map.class);
        assertThat(guestDownloadResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> previewResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-content"),
            HttpMethod.GET,
            authorizedRequest(downloader),
            String.class
        );
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).isEqualTo("approved content");

        ResponseEntity<Resource> downloadResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/download"),
            HttpMethod.GET,
            authorizedRequest(downloader),
            Resource.class
        );
        assertThat(downloadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void previewMetadataShouldRespectPendingDocumentPermissions() throws Exception {
        User uploader = createUser("preview_owner", "preview_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User otherUser = createUser("preview_other", "preview_other@example.com", UserRole.USER, UserStatus.ACTIVE);
        User admin = createUser("preview_admin", "preview_admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "待审预览文档", DocumentStatus.PENDING, "txt");
        writeDocumentFile(document, "preview info content");

        ResponseEntity<Map<String, Object>> ownerPreviewInfoResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-info"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> ownerPreviewInfoBody = Objects.requireNonNull(ownerPreviewInfoResponse.getBody());
        Map<String, Object> ownerPreviewInfo = (Map<String, Object>) ownerPreviewInfoBody.get("data");
        assertThat(ownerPreviewInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownerPreviewInfoBody).containsEntry("code", 0);
        assertThat(ownerPreviewInfo).containsEntry("previewable", true);
        assertThat(ownerPreviewInfo).containsEntry("previewType", "TEXT");
        assertThat(ownerPreviewInfo).containsEntry("content", "preview info content");

        ResponseEntity<Map<String, Object>> ownerPreviewableResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/previewable"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> ownerPreviewableBody = Objects.requireNonNull(ownerPreviewableResponse.getBody());
        assertThat(ownerPreviewableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownerPreviewableBody).containsEntry("code", 0).containsEntry("data", true);

        ResponseEntity<Map<String, Object>> otherPreviewInfoResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-info"),
            HttpMethod.GET,
            authorizedRequest(otherUser),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> otherPreviewInfoBody = Objects.requireNonNull(otherPreviewInfoResponse.getBody());
        assertThat(otherPreviewInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(otherPreviewInfoBody.get("code"))).isEqualTo("1");
        assertThat(otherPreviewInfoBody).containsEntry("message", "无权限访问该文档");

        ResponseEntity<Map<String, Object>> adminPreviewableResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/previewable"),
            HttpMethod.GET,
            authorizedRequest(admin),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> adminPreviewableBody = Objects.requireNonNull(adminPreviewableResponse.getBody());
        assertThat(adminPreviewableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminPreviewableBody).containsEntry("code", 0).containsEntry("data", true);
    }

    @Test
    void thumbnailShouldBeControlledBySameAccessRules() throws Exception {
        User uploader = createUser("thumb_owner", "thumb_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User otherUser = createUser("thumb_other", "thumb_other@example.com", UserRole.USER, UserStatus.ACTIVE);
        User admin = createUser("thumb_admin", "thumb_admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "缩略图文档", DocumentStatus.PENDING, "png");
        writeImageFile(document);

        ResponseEntity<Resource> ownerThumbnailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/thumbnail"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            Resource.class
        );
        assertThat(ownerThumbnailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownerThumbnailResponse.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(Objects.requireNonNull(ownerThumbnailResponse.getBody()).contentLength()).isGreaterThan(0L);

        ResponseEntity<String> otherThumbnailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/thumbnail"),
            HttpMethod.GET,
            authorizedRequest(otherUser),
            String.class
        );
        assertThat(otherThumbnailResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Resource> adminThumbnailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/thumbnail"),
            HttpMethod.GET,
            authorizedRequest(admin),
            Resource.class
        );
        assertThat(adminThumbnailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(adminThumbnailResponse.getBody()).contentLength()).isGreaterThan(0L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void guestShouldNotAccessPreviewMetadataEndpoints() throws Exception {
        User uploader = createUser("guest_preview_owner", "guest_preview_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "游客预览文档", DocumentStatus.APPROVED, "txt");
        writeDocumentFile(document, "approved preview content");

        ResponseEntity<Map<String, Object>> guestPreviewInfoResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-info"),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(guestPreviewInfoResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, Object>> guestPreviewableResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/previewable"),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(guestPreviewableResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, Object>> guestThumbnailResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/thumbnail"),
            HttpMethod.GET,
            null,
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(guestThumbnailResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvedDocumentShouldAllowAuthenticatedPreviewMetadata() throws Exception {
        User uploader = createUser("approved_meta_owner", "approved_meta_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User viewer = createUser("approved_meta_user", "approved_meta_user@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "公开预览文档", DocumentStatus.APPROVED, "txt");
        writeDocumentFile(document, "preview metadata content");

        ResponseEntity<Map<String, Object>> previewInfoResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-info"),
            HttpMethod.GET,
            authorizedRequest(viewer),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> previewInfoBody = Objects.requireNonNull(previewInfoResponse.getBody());
        Map<String, Object> previewInfo = (Map<String, Object>) previewInfoBody.get("data");
        assertThat(previewInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewInfoBody).containsEntry("code", 0);
        assertThat(previewInfo).containsEntry("previewable", true);
        assertThat(previewInfo).containsEntry("previewType", "TEXT");
        assertThat(previewInfo).containsEntry("content", "preview metadata content");

        ResponseEntity<Map<String, Object>> previewableResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/previewable"),
            HttpMethod.GET,
            authorizedRequest(viewer),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> previewableBody = Objects.requireNonNull(previewableResponse.getBody());
        assertThat(previewableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewableBody).containsEntry("code", 0).containsEntry("data", true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void unsupportedFormatShouldReturnNotPreviewableMetadata() throws Exception {
        User uploader = createUser("zip_owner", "zip_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "压缩包文档", DocumentStatus.APPROVED, "zip");
        writeBinaryFile(document, new byte[] {0x50, 0x4B, 0x03, 0x04});

        ResponseEntity<Map<String, Object>> previewInfoResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-info"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> previewInfoBody = Objects.requireNonNull(previewInfoResponse.getBody());
        Map<String, Object> previewInfo = (Map<String, Object>) previewInfoBody.get("data");
        assertThat(previewInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewInfoBody).containsEntry("code", 0);
        assertThat(previewInfo).containsEntry("previewable", false);
        assertThat(previewInfo).containsEntry("previewType", "NOT_SUPPORTED");

        ResponseEntity<Map<String, Object>> previewableResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/previewable"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        Map<String, Object> previewableBody = Objects.requireNonNull(previewableResponse.getBody());
        assertThat(previewableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewableBody).containsEntry("code", 0).containsEntry("data", false);

        ResponseEntity<Map<String, Object>> previewContentResponse = restTemplate.exchange(
            url("/api/documents/" + document.getId() + "/preview-content"),
            HttpMethod.GET,
            authorizedRequest(uploader),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        assertThat(previewContentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

    private Document createDocument(Long uploaderId, Long categoryId, String title, DocumentStatus status, String extension) {
        Document document = new Document();
        document.setTitle(title);
        document.setDescription(title + " description");
        document.setFileName(title + "." + extension);
        document.setFilePath("documents/" + title + "." + extension);
        document.setFileSize(1024L);
        document.setFileType("txt".equals(extension) ? "text/plain" : "application/pdf");
        document.setFileExtension(extension);
        document.setCategoryId(categoryId);
        document.setUploaderId(uploaderId);
        document.setStatus(status);
        document.setDownloadCount(0);
        document.setViewCount(0);
        document.setRatingAverage(BigDecimal.ZERO);
        document.setRatingCount(0);
        document.setDownloadPoints(0);
        if (status == DocumentStatus.APPROVED) {
            document.setApprovedAt(LocalDateTime.now());
            document.setApprovedBy(uploaderId);
        }
        return documentRepository.save(document);
    }

    private void writeDocumentFile(Document document, String content) throws Exception {
        Path filePath = fileUploadService.getFilePath(document.getFilePath());
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        document.setFileSize(Files.size(filePath));
        documentRepository.save(document);
    }

    private void writeImageFile(Document document) throws Exception {
        byte[] pngBytes = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+y1Z0AAAAASUVORK5CYII="
        );
        Path filePath = fileUploadService.getFilePath(document.getFilePath());
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, pngBytes);
        document.setFileSize(Files.size(filePath));
        document.setFileType("image/png");
        documentRepository.save(document);
    }

    private void writeBinaryFile(Document document, byte[] content) throws Exception {
        Path filePath = fileUploadService.getFilePath(document.getFilePath());
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
        document.setFileSize(Files.size(filePath));
        documentRepository.save(document);
    }
}
