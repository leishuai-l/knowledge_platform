package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.entity.Category;
import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.enums.ReportType;
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
class CopyrightEvidenceAccessControllerTest {

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
    void reporterShouldViewOwnEvidence() throws Exception {
        User reporter = createUser("report_owner", "report_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User uploader = createUser("doc_owner", "doc_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "举报文档");

        String evidencePath = "reports/" + reporter.getId() + "/evidence.txt";
        writeFile(evidencePath, "owner evidence");
        createReport(document.getId(), reporter.getId(), evidencePath);

        ResponseEntity<Resource> response = restTemplate.exchange(
            url("/api/copyright/reports/evidence/view?path=" + evidencePath),
            HttpMethod.GET,
            authorizedRequest(reporter),
            Resource.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getContentAsByteArray())
            .isEqualTo("owner evidence".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void otherUserShouldNotViewForeignEvidence() throws Exception {
        User reporter = createUser("report_foreign_owner", "report_foreign_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User otherUser = createUser("other_user", "other_user@example.com", UserRole.USER, UserStatus.ACTIVE);
        User uploader = createUser("doc_owner2", "doc_owner2@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "举报文档2");

        String evidencePath = "reports/" + reporter.getId() + "/foreign.txt";
        writeFile(evidencePath, "foreign evidence");
        createReport(document.getId(), reporter.getId(), evidencePath);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/copyright/reports/evidence/view?path=" + evidencePath),
            HttpMethod.GET,
            authorizedRequest(otherUser),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminShouldViewAnyEvidence() throws Exception {
        User reporter = createUser("report_admin_owner", "report_admin_owner@example.com", UserRole.USER, UserStatus.ACTIVE);
        User admin = createUser("report_admin", "report_admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        User uploader = createUser("doc_owner3", "doc_owner3@example.com", UserRole.USER, UserStatus.ACTIVE);
        Category category = createCategory("Java");
        Document document = createDocument(uploader.getId(), category.getId(), "举报文档3");

        String evidencePath = "reports/" + reporter.getId() + "/admin-readable.txt";
        writeFile(evidencePath, "admin evidence");
        createReport(document.getId(), reporter.getId(), evidencePath);

        ResponseEntity<Resource> response = restTemplate.exchange(
            url("/api/copyright/reports/evidence/view?path=" + evidencePath),
            HttpMethod.GET,
            authorizedRequest(admin),
            Resource.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getContentAsByteArray())
            .isEqualTo("admin evidence".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidEvidencePathShouldBeRejected() {
        User user = createUser("path_user", "path_user@example.com", UserRole.USER, UserStatus.ACTIVE);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url("/api/copyright/reports/evidence/view?path=avatars/user_1.png"),
            HttpMethod.GET,
            authorizedRequest(user),
            (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

    private Document createDocument(Long uploaderId, Long categoryId, String title) {
        Document document = new Document();
        document.setTitle(title);
        document.setDescription(title + " description");
        document.setFileName(title + ".txt");
        document.setFilePath("documents/" + title + ".txt");
        document.setFileSize(12L);
        document.setFileType("text/plain");
        document.setFileExtension("txt");
        document.setCategoryId(categoryId);
        document.setUploaderId(uploaderId);
        document.setStatus(DocumentStatus.APPROVED);
        document.setDownloadCount(0);
        document.setViewCount(0);
        document.setRatingAverage(BigDecimal.ZERO);
        document.setRatingCount(0);
        document.setDownloadPoints(0);
        document.setApprovedAt(LocalDateTime.now());
        document.setApprovedBy(uploaderId);
        return documentRepository.save(document);
    }

    private CopyrightReport createReport(Long documentId, Long reporterId, String evidencePath) {
        CopyrightReport report = new CopyrightReport();
        report.setDocumentId(documentId);
        report.setReporterId(reporterId);
        report.setReportType(ReportType.OTHER);
        report.setStatus(ReportStatus.PENDING);
        report.setDescription("evidence test");
        report.setEvidenceUrls(evidencePath);
        report.setContactInfo("tester@example.com");
        return copyrightReportRepository.save(report);
    }

    private void writeFile(String relativePath, String content) throws Exception {
        Path filePath = fileUploadService.getFilePath(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }
}
