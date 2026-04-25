package com.zhixiang.knowledge_platform.service.ai;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.service.FileUploadService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final DocumentRepository documentRepository;
    private final FileUploadService fileUploadService;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.model:quentinz/bge-large-zh-v1.5}")
    private String embeddingModelName;

    @Value("${zhixiang.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${zhixiang.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${zhixiang.vectorstore.qdrant.collection-name:knowledge_platform}")
    private String collectionName;

    private VectorStore vectorStore;
    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing RAG vector store with Qdrant...");
            log.info("Ollama embedding: {} at {}", embeddingModelName, ollamaBaseUrl);

            // 1. Create Ollama Embedding model
            OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl);
            OllamaOptions ollamaOptions = OllamaOptions.builder()
                    .model(embeddingModelName)
                    .build();

            embeddingModel = OllamaEmbeddingModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(ollamaOptions)
                    .build();

            // 2. Test connection
            float[] testEmbedding = embeddingModel.embed("测试");
            log.info("Ollama embedding model connected successfully, dimension: {}", testEmbedding.length);

            // 3. Create Qdrant Vector Store
            QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                    qdrantHost, qdrantPort, false
            ).build();
            QdrantClient qdrantClient = new QdrantClient(grpcClient);
            this.vectorStore = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(true)
                    .build();

            log.info("RAG vector store initialized successfully with Qdrant");
            log.info("Qdrant collection '{}' is ready at {}:{}", collectionName, qdrantHost, qdrantPort);

        } catch (Exception e) {
            log.error("Failed to initialize RAG vector store: {}", e.getMessage(), e);
            log.warn("RAG vector search is disabled. Please ensure Ollama and Qdrant are running.");
            vectorStore = null;
            embeddingModel = null;
        }
    }

    /**
     * Re-ingest a single approved document by its ID (for admin use)
     */
    public void reingestDocument(Long documentId) {
        if (vectorStore == null) {
            log.warn("Vector store is not available, skipping re-ingestion");
            return;
        }
        documentRepository.findById(documentId).ifPresent(doc -> {
            if (doc.isApproved()) {
                ingestDocument(doc);
                log.info("Re-ingested document ID: {} ({})", documentId, doc.getFileName());
            } else {
                log.warn("Document ID {} is not approved, skipping re-ingestion", documentId);
            }
        });
    }

    /**
     * Ingest all approved documents into vector store
     */
    public void ingestAllDocuments() {
        if (vectorStore == null) {
            log.warn("Vector store is not available, skipping document ingestion");
            return;
        }
        log.info("Starting full document ingestion for RAG...");
        List<Document> docs = documentRepository.findAllByStatus(DocumentStatus.APPROVED);

        int successCount = 0;
        for (Document doc : docs) {
            try {
                ingestDocument(doc);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to ingest document: {} (ID: {})", doc.getFileName(), doc.getId(), e);
            }
        }

        log.info("RAG ingestion complete: {}/{} documents ingested", successCount, docs.size());
    }

    /**
     * Ingest a single approved document
     */
    public void ingestDocument(Document doc) {
        if (vectorStore == null) {
            log.debug("Vector store is not available, skipping document ingestion");
            return;
        }
        try {
            // Use FileUploadService to get the correct path
            Path filePath = fileUploadService.getFilePath(doc.getFilePath());
            File file = filePath.toFile();

            if (!file.exists()) {
                log.warn("File not found for document: {} (Path: {})", doc.getFileName(), filePath);
                return;
            }

            // 1. Read document content using Tika
            FileSystemResource resource = new FileSystemResource(file);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<org.springframework.ai.document.Document> originalDocs = reader.get();

            if (originalDocs.isEmpty()) {
                log.warn("No content extracted from document: {}", doc.getFileName());
                return;
            }

            // 2. Split content into chunks (优化: 400字符块/50重叠，符合论文500字符描述)
            // 参数: minChunkSizeChars=400, minChunkLengthToEmbed=50, maxNumChunks=2000, combineLength=50, keepSeparator=true
            TokenTextSplitter splitter = new TokenTextSplitter(400, 50, 2000, 50, true);
            List<org.springframework.ai.document.Document> splitDocs = splitter.apply(originalDocs);

            // 3. Add metadata
            for (org.springframework.ai.document.Document splitDoc : splitDocs) {
                splitDoc.getMetadata().put("docId", doc.getId().toString());
                splitDoc.getMetadata().put("title", doc.getTitle());
                splitDoc.getMetadata().put("fileName", doc.getFileName());
            }

            // 4. Add to vector store (add chunks one by one)
            int successCount = 0;
            for (int i = 0; i < splitDocs.size(); i++) {
                org.springframework.ai.document.Document chunk = splitDocs.get(i);
                try {
                    // Truncate content to avoid Ollama context limit (150 chars for 512 context window)
                    String chunkText = chunk.getContent();
                    if (chunkText != null && chunkText.length() > 150) {
                        chunkText = chunkText.substring(0, 150);
                    }
                    // Create new doc with truncated content
                    org.springframework.ai.document.Document truncatedChunk =
                        new org.springframework.ai.document.Document(chunk.getId(), chunkText, chunk.getMetadata());
                    vectorStore.add(List.of(truncatedChunk));
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to add chunk {}/{} for document {}: {}", i+1, splitDocs.size(), doc.getFileName(), e.getMessage());
                }
            }
            log.info("Ingested document: {} ({}/{} chunks)", doc.getFileName(), successCount, splitDocs.size());

        } catch (Exception e) {
            throw new RuntimeException("Error ingesting document: " + doc.getFileName(), e);
        }
    }

    /**
     * Add a single AI document to the vector store
     */
    public void addDocument(org.springframework.ai.document.Document document) {
        if (vectorStore == null) {
            log.warn("Vector store is not available, skipping document addition: {}",
                    document.getMetadata().get("title"));
            return;
        }
        try {
            vectorStore.add(List.of(document));
            log.debug("Added document to vector store: docId={}, title={}",
                    document.getMetadata().get("docId"),
                    document.getMetadata().get("title"));
        } catch (Exception e) {
            log.error("Failed to add document to vector store: docId={}, title={}",
                    document.getMetadata().get("docId"),
                    document.getMetadata().get("title"), e);
        }
    }

    /**
     * Similarity search across all documents
     */
    public List<org.springframework.ai.document.Document> similaritySearch(String query) {
        if (vectorStore == null) {
            log.debug("Vector store is not available, returning empty results");
            return List.of();
        }
        try {
            return vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(query)
                            .topK(5)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error during similarity search: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Similarity search filtered by specific document ID (using Qdrant native metadata filtering)
     */
    public List<org.springframework.ai.document.Document> similaritySearch(String query, Long docId) {
        if (vectorStore == null) {
            log.debug("Vector store is not available, returning empty results");
            return List.of();
        }

        if (docId == null) {
            return similaritySearch(query);
        }

        try {
            // Use Qdrant native metadata filtering - no memory filtering needed
            Filter.Expression filterExpr = new FilterExpressionBuilder()
                    .eq("docId", String.valueOf(docId))
                    .build();

            List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(query)
                            .topK(6)
                            .filterExpression(filterExpr)
                            .build()
            );

            if (results.isEmpty()) {
                log.debug("RAG filtered search: docId={}, query='{}', no matches found",
                        docId, query);
            } else {
                log.debug("RAG filtered search: docId={}, query='{}', matched={}",
                        docId, query, results.size());
            }
            return results;
        } catch (Exception e) {
            log.error("Error during filtered similarity search: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Check if vector store is available
     */
    public boolean isAvailable() {
        return vectorStore != null;
    }
}
