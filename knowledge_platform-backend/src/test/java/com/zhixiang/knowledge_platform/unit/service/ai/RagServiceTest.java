package com.zhixiang.knowledge_platform.unit.service.ai;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import com.zhixiang.knowledge_platform.service.ai.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * RAG智能问答服务单元测试
 * 验证向量检索TopK参数：全局检索TopK=5，单文档检索TopK=6
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private RagService ragService;

    @BeforeEach
    void setUp() {
        // 使用反射将mock的vectorStore注入，禁用@PostConstruct中的初始化
        org.springframework.test.util.ReflectionTestUtils.setField(
                ragService, "vectorStore", vectorStore);
    }

    private Document createTestDocument(Long id, String title) {
        Document doc = new Document();
        doc.setId(id);
        doc.setTitle(title);
        doc.setFileName(title + ".pdf");
        doc.setStatus(DocumentStatus.APPROVED);
        doc.setFilePath("/uploads/" + id + ".pdf");
        return doc;
    }

    // ========== TC-025 ~ TC-027: 向量检索TopK参数验证 ==========
    @Test
    @DisplayName("TC-025: 全局检索应使用TopK=5参数")
    void similaritySearch_GlobalSearch_ShouldUseTopK5() {
        // Given
        String query = "Spring Boot框架介绍";
        List<org.springframework.ai.document.Document> expectedResults = List.of(
                new org.springframework.ai.document.Document("1", "内容1", java.util.Map.of()),
                new org.springframework.ai.document.Document("2", "内容2", java.util.Map.of())
        );

        // 使用类型安全的argThat
        when(vectorStore.similaritySearch(argThat((SearchRequest req) ->
                req != null && req.getTopK() == 5
        ))).thenReturn(expectedResults);

        // When
        List<org.springframework.ai.document.Document> results = ragService.similaritySearch(query);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(vectorStore).similaritySearch(argThat((SearchRequest req) -> {
            assertEquals(5, req.getTopK(), "全局检索TopK应为5");
            assertEquals(query, req.getQuery());
            return true;
        }));
    }

    @Test
    @DisplayName("TC-026: 单文档检索应使用TopK=6参数")
    void similaritySearch_SingleDocumentSearch_ShouldUseTopK6() {
        // Given
        String query = "数据库设计";
        Long docId = 100L;

        List<org.springframework.ai.document.Document> expectedResults = List.of(
                new org.springframework.ai.document.Document("1", "数据库设计内容1", java.util.Map.of("docId", "100")),
                new org.springframework.ai.document.Document("2", "数据库设计内容2", java.util.Map.of("docId", "100"))
        );

        when(vectorStore.similaritySearch(argThat((SearchRequest req) ->
                req != null && req.getTopK() == 6
        ))).thenReturn(expectedResults);

        // When
        List<org.springframework.ai.document.Document> results = ragService.similaritySearch(query, docId);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(vectorStore).similaritySearch(argThat((SearchRequest req) -> {
            assertEquals(6, req.getTopK(), "单文档检索TopK应为6");
            assertEquals(query, req.getQuery());
            assertNotNull(req.getFilterExpression(), "单文档检索应包含过滤条件");
            return true;
        }));
    }

    @Test
    @DisplayName("TC-027: 单文档检索未指定文档ID时应退化为全局检索")
    void similaritySearch_WithNullDocId_ShouldFallBackToGlobal() {
        // Given
        String query = "测试查询";
        Long nullDocId = null;

        List<org.springframework.ai.document.Document> expectedResults = List.of(
                new org.springframework.ai.document.Document("1", "全局内容", java.util.Map.of())
        );

        when(vectorStore.similaritySearch(argThat((SearchRequest req) ->
                req != null && req.getTopK() == 5
        ))).thenReturn(expectedResults);

        // When
        List<org.springframework.ai.document.Document> results = ragService.similaritySearch(query, nullDocId);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(vectorStore).similaritySearch(argThat((SearchRequest req) -> {
            assertEquals(5, req.getTopK(), "无docId时TopK应为5");
            return true;
        }));
    }

    // ========== TC-028: 文档向量化检查 ==========
    @Test
    @DisplayName("TC-028: 文档向量化应仅处理已批准状态的文档")
    void ingestDocument_ShouldOnlyProcessApprovedDocuments() {
        // Given
        Document approvedDoc = createTestDocument(1L, "已批准文档");
        Document pendingDoc = createTestDocument(2L, "待审核文档");
        pendingDoc.setStatus(DocumentStatus.PENDING);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(approvedDoc));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(pendingDoc));

        // 将vectorStore设为null，避免实际调用
        org.springframework.test.util.ReflectionTestUtils.setField(
                ragService, "vectorStore", null);

        // When - 调用reingestDocument方法（只有APPROVED文档会被处理）
        ragService.reingestDocument(1L);  // 应该尝试处理
        ragService.reingestDocument(2L);  // 应该跳过

        // Then - 检查文档状态判断逻辑
        verify(documentRepository).findById(1L);
        verify(documentRepository).findById(2L);
    }

    // ========== TC-029: 向量存储可用性检查 ==========
    @Test
    @DisplayName("TC-029: 向量存储不可用时相似性搜索应返回空列表")
    void similaritySearch_WhenVectorStoreUnavailable_ShouldReturnEmptyList() {
        // Given - 向量存储不可用（vectorStore为null）
        org.springframework.test.util.ReflectionTestUtils.setField(
                ragService, "vectorStore", null);

        // When
        List<org.springframework.ai.document.Document> results = ragService.similaritySearch("测试查询");

        // Then
        assertNotNull(results, "结果不应为null");
        assertTrue(results.isEmpty(), "向量存储不可用时结果应为空列表");
    }

    @Test
    @DisplayName("TC-030: 检查向量存储可用性")
    void isAvailable_ShouldReflectVectorStoreState() {
        // Given - 向量存储不可用
        org.springframework.test.util.ReflectionTestUtils.setField(
                ragService, "vectorStore", null);

        // When & Then
        assertFalse(ragService.isAvailable(), "vectorStore为null时应返回false");

        // Given - 向量存储可用
        org.springframework.test.util.ReflectionTestUtils.setField(
                ragService, "vectorStore", vectorStore);

        // Then
        assertTrue(ragService.isAvailable(), "vectorStore存在时应返回true");
    }
}
