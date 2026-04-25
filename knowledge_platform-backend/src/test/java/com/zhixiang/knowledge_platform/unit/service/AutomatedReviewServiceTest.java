package com.zhixiang.knowledge_platform.unit.service;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.service.AutomatedReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 文档自动化初审服务单元测试
 * 验证三级过滤机制：格式过滤 → 内容过滤 → MD5查重
 */
@ExtendWith(MockitoExtension.class)
class AutomatedReviewServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private AutomatedReviewService automatedReviewService;

    // ========== TC-016: 格式白名单过滤测试 ==========
    @Test
    @DisplayName("TC-016: PDF文件格式应通过格式检查")
    void checkFormat_WithPdfFile_ShouldPass() {
        // Given
        MultipartFile pdfFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(pdfFile);

        // Then
        assertTrue((Boolean) result.get("formatValid"), "PDF格式应通过验证");
    }

    @Test
    @DisplayName("TC-017: Word文档格式应通过格式检查")
    void checkFormat_WithWordFile_ShouldPass() {
        // Given
        MultipartFile wordFile = new MockMultipartFile(
                "file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{0x50, 0x4B, 0x03, 0x04}
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(wordFile);

        // Then
        assertTrue((Boolean) result.get("formatValid"), "Word格式应通过验证");
    }

    @Test
    @DisplayName("TC-018: 可执行文件应被格式过滤拦截")
    void checkFormat_WithExeFile_ShouldFail() {
        // Given
        MultipartFile exeFile = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[]{0x4D, 0x5A}
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(exeFile);

        // Then
        assertFalse((Boolean) result.get("passed"), "可执行文件应被拒绝");
        assertFalse((Boolean) result.get("formatValid"), "格式验证应失败");
        assertTrue(result.get("message").toString().contains("Invalid"), "应返回无效格式提示");
    }

    // ========== TC-019 ~ TC-021: 敏感词检测测试 ==========
    @Test
    @DisplayName("TC-019: 文件名包含'代考'关键词应被拒绝")
    void checkContent_WithIllegalKeyword_ShouldFail() {
        // Given
        MultipartFile illegalFile = new MockMultipartFile(
                "file", "期末代考答案.pdf", "application/pdf", "content".getBytes()
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(illegalFile);

        // Then
        assertFalse((Boolean) result.get("contentCompliant"), "敏感词文件应被拒绝");
        assertFalse((Boolean) result.get("passed"), "应未通过审核");
    }

    @Test
    @DisplayName("TC-020: 文件名包含'色情'关键词应被拒绝")
    void checkContent_WithPornKeyword_ShouldFail() {
        // Given
        MultipartFile illegalFile = new MockMultipartFile(
                "file", "色情资料.pdf", "application/pdf", "content".getBytes()
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(illegalFile);

        // Then
        assertFalse((Boolean) result.get("contentCompliant"), "敏感词文件应被拒绝");
    }

    @Test
    @DisplayName("TC-021: 正常文件名应通过内容检查")
    void checkContent_WithNormalFilename_ShouldPass() {
        // Given
        MultipartFile normalFile = new MockMultipartFile(
                "file", "Java编程思想.pdf", "application/pdf", "content".getBytes()
        );

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(normalFile);

        // Then
        assertTrue((Boolean) result.get("contentCompliant"), "正常文件应通过内容检查");
    }

    // ========== TC-022 ~ TC-024: MD5查重测试 ==========
    @Test
    @DisplayName("TC-022: 全新文件MD5查重应通过")
    void checkMd5_WithNewFile_ShouldPass() {
        // Given
        MockMultipartFile newFile = new MockMultipartFile(
                "file", "全新文件.pdf", "application/pdf", "全新内容".getBytes()
        );
        when(documentRepository.findByMd5(anyString())).thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(newFile);

        // Then
        Double similarityScore = (Double) result.get("similarityScore");
        assertEquals(0.0, similarityScore, "新文件相似度应为0");
        assertTrue((Boolean) result.get("passed"), "新文件应通过审核");
    }

    @Test
    @DisplayName("TC-023: 已存在文件（非删除状态）MD5重复应被拒绝")
    void checkMd5_WithDuplicateFile_ShouldFail() {
        // Given
        MockMultipartFile duplicateFile = new MockMultipartFile(
                "file", "重复文件.pdf", "application/pdf", "相同内容".getBytes()
        );

        Document existingDoc = new Document();
        existingDoc.setId(1L);
        existingDoc.setStatus(DocumentStatus.APPROVED); // 非删除状态

        when(documentRepository.findByMd5(anyString())).thenReturn(List.of(existingDoc));

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(duplicateFile);

        // Then
        Double similarityScore = (Double) result.get("similarityScore");
        assertEquals(1.0, similarityScore, "重复文件相似度应为1.0");
        assertFalse((Boolean) result.get("passed"), "重复文件应被拒绝");
        assertTrue(result.get("message").toString().contains("too similar"), "应返回重复提示");
    }

    @Test
    @DisplayName("TC-024: 已删除文件的MD5重复应允许上传")
    void checkMd5_WithDeletedFileDuplicate_ShouldPass() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "曾被删除的文件.pdf", "application/pdf", "相同内容".getBytes()
        );

        Document deletedDoc = new Document();
        deletedDoc.setId(1L);
        deletedDoc.setStatus(DocumentStatus.DELETED); // 已删除状态

        when(documentRepository.findByMd5(anyString())).thenReturn(List.of(deletedDoc));

        // When
        Map<String, Object> result = automatedReviewService.performPreliminaryReview(file);

        // Then
        Double similarityScore = (Double) result.get("similarityScore");
        assertEquals(0.0, similarityScore, "已删除文件不算重复，相似度应为0");
        assertTrue((Boolean) result.get("passed"), "已删除文件的MD5应允许重新上传");
    }
}
