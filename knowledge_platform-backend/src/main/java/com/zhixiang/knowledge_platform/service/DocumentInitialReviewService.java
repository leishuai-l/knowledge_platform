package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.DocumentReview;
import com.zhixiang.knowledge_platform.enums.DocumentStatus;
import com.zhixiang.knowledge_platform.enums.ReviewStatus;
import com.zhixiang.knowledge_platform.enums.ReviewType;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档初审服务
 * 实现自动化初审：格式检查、内容合规性检测、文本相似度分析
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentInitialReviewService {

    private final DocumentReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final FileUploadService fileUploadService;
    private final PointsService pointsService;
    private final FileTypeRegistryService fileTypeRegistryService;

    // 敏感词列表（实际应该从数据库或配置文件加载）
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "反动", "邪教"
    );

    // 文件大小限制（100MB）
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    // 相似度阈值（超过80%认为高度相似）
    private static final double SIMILARITY_THRESHOLD = 80.0;

    /**
     * 执行文档初审
     */
    @Transactional
    public DocumentReview performInitialReview(Document document) {
        log.info("开始对文档 {} 进行初审", document.getId());

        DocumentReview review = new DocumentReview();
        review.setDocumentId(document.getId());
        review.setReviewType(ReviewType.INITIAL);
        review.setStatus(ReviewStatus.PENDING);

        try {
            // 1. 格式检查
            boolean formatCheckPassed = checkFormat(document);
            review.setFormatCheckPassed(formatCheckPassed);

            // 2. 内容合规性检测
            boolean contentCompliancePassed = checkContentCompliance(document);
            review.setContentCompliancePassed(contentCompliancePassed);

            // 3. 文本相似度分析
            SimilarityResult similarityResult = checkSimilarity(document);
            review.setSimilarityScore(similarityResult.getScore());
            review.setSimilarDocumentId(similarityResult.getSimilarDocumentId());

            // 4. 生成审核结果和建议
            generateReviewResult(review);

            review.setReviewedAt(LocalDateTime.now());
            reviewRepository.save(review);

            // 5. 更新文档状态
            updateDocumentStatus(document, review);

            log.info("文档 {} 初审完成，结果: {}", document.getId(), review.getStatus());
            return review;

        } catch (Exception e) {
            log.error("文档 {} 初审失败", document.getId(), e);
            review.setStatus(ReviewStatus.REJECTED);
            review.setRejectionReason("系统错误：文档初审暂时不可用，请稍后联系管理员处理");
            review.setSuggestions("文档已保存，请不要重复上传相同文件，可前往我的上传查看状态或稍后联系管理员。");
            review.setReviewComment("初审执行异常，系统已自动标记为拒绝。请管理员介入处理。");
            review.setReviewedAt(LocalDateTime.now());
            reviewRepository.save(review);
            updateDocumentStatus(document, review);
            return review;
        }
    }

    /**
     * 格式检查
     */
    private boolean checkFormat(Document document) {
        List<String> issues = new ArrayList<>();

        // 1. 检查文件大小
        if (document.getFileSize() > MAX_FILE_SIZE) {
            issues.add("文件大小超过限制（最大100MB）");
        }

        // 2. 检查文件扩展名
        String extension = document.getFileExtension().toLowerCase();
        if (!fileTypeRegistryService.isInitialReviewSupported(extension)) {
            issues.add("平台暂不支持该文件类型进入审核流程：" + extension);
        }

        // 3. 检查文件是否存在且可读
        try {
            Path filePath = fileUploadService.getFilePath(document.getFilePath());
            if (!Files.exists(filePath)) {
                issues.add("文件不存在");
            } else if (!Files.isReadable(filePath)) {
                issues.add("文件不可读");
            }
        } catch (Exception e) {
            issues.add("文件访问错误：" + e.getMessage());
        }

        // 4. 检查标题和描述
        if (document.getTitle() == null || document.getTitle().trim().isEmpty()) {
            issues.add("标题不能为空");
        } else if (document.getTitle().length() < 5) {
            issues.add("标题过短（至少5个字符）");
        } else if (document.getTitle().length() > 200) {
            issues.add("标题过长（最多200个字符）");
        }

        if (document.getDescription() != null && document.getDescription().length() > 2000) {
            issues.add("描述过长（最多2000个字符）");
        }

        return issues.isEmpty();
    }

    /**
     * 内容合规性检测
     */
    private boolean checkContentCompliance(Document document) {
        List<String> issues = new ArrayList<>();

        // 1. 检查标题中的敏感词
        String title = document.getTitle().toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (title.contains(word)) {
                issues.add("标题包含敏感词：" + word);
            }
        }

        // 2. 检查描述中的敏感词
        if (document.getDescription() != null) {
            String description = document.getDescription().toLowerCase();
            for (String word : SENSITIVE_WORDS) {
                if (description.contains(word)) {
                    issues.add("描述包含敏感词：" + word);
                }
            }
        }

        // 3. 检查是否包含联系方式（防止广告）
        String content = (document.getTitle() + " " +
                         (document.getDescription() != null ? document.getDescription() : ""));

        // 检查手机号
        Pattern phonePattern = Pattern.compile("1[3-9]\\d{9}");
        if (phonePattern.matcher(content).find()) {
            issues.add("内容包含联系方式");
        }

        // 检查QQ号
        Pattern qqPattern = Pattern.compile("[QqＱｑ]{2}[:：]?\\s*\\d{5,}");
        if (qqPattern.matcher(content).find()) {
            issues.add("内容包含QQ号");
        }

        // 检查微信号
        Pattern wechatPattern = Pattern.compile("[微Ｗｗ][信Ｘｘ][:：]?\\s*[a-zA-Z0-9_-]{6,}");
        if (wechatPattern.matcher(content).find()) {
            issues.add("内容包含微信号");
        }

        return issues.isEmpty();
    }

    /**
     * 文本相似度检测
     * 简化版实现，实际应该使用更复杂的算法（如余弦相似度、编辑距离等）
     */
    private SimilarityResult checkSimilarity(Document document) {
        try {
            // 获取所有已审核通过的文档
            List<Document> approvedDocuments = documentRepository
                    .findAllByStatus(DocumentStatus.APPROVED);

            double maxSimilarity = 0.0;
            Long similarDocumentId = null;

            // 计算与每个文档的相似度
            for (Document existingDoc : approvedDocuments) {
                if (existingDoc.getId().equals(document.getId())) {
                    continue;
                }

                double similarity = calculateSimilarity(
                        document.getTitle(),
                        existingDoc.getTitle()
                );

                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    similarDocumentId = existingDoc.getId();
                }
            }

            return new SimilarityResult(maxSimilarity, similarDocumentId);

        } catch (Exception e) {
            log.error("相似度检测失败", e);
            return new SimilarityResult(0.0, null);
        }
    }

    /**
     * 计算两个文本的相似度（简化版）
     * 使用 Jaccard 相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        // 转换为小写并分词
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");

        // 计算交集和并集
        List<String> set1 = Arrays.asList(words1);
        List<String> set2 = Arrays.asList(words2);

        long intersection = set1.stream()
                .filter(set2::contains)
                .count();

        long union = set1.size() + set2.size() - intersection;

        if (union == 0) {
            return 0.0;
        }

        return (double) intersection / union * 100.0;
    }

    /**
     * 生成审核结果和建议
     */
    private void generateReviewResult(DocumentReview review) {
        StringBuilder comment = new StringBuilder();
        StringBuilder suggestions = new StringBuilder();
        boolean passed = true;

        // 格式检查结果
        if (!Boolean.TRUE.equals(review.getFormatCheckPassed())) {
            passed = false;
            comment.append("格式检查未通过。");
            suggestions.append("请检查文件格式、大小和标题描述是否符合要求。");
        }

        // 内容合规性检查结果
        if (!Boolean.TRUE.equals(review.getContentCompliancePassed())) {
            passed = false;
            comment.append("内容合规性检查未通过。");
            suggestions.append("请移除敏感词汇和联系方式。");
        }

        // 相似度检查结果
        if (review.getSimilarityScore() != null &&
            review.getSimilarityScore() > SIMILARITY_THRESHOLD) {
            passed = false;
            comment.append(String.format("检测到与文档 #%d 高度相似（相似度: %.2f%%）。",
                    review.getSimilarDocumentId(), review.getSimilarityScore()));
            suggestions.append("请确保文档具有原创性，避免重复上传。");
        }

        if (passed) {
            review.setStatus(ReviewStatus.APPROVED);
            review.setReviewComment("初审通过，等待复审。");
        } else {
            review.setStatus(ReviewStatus.REJECTED);
            review.setRejectionReason(comment.toString());
            review.setSuggestions(suggestions.toString());
        }
    }

    /**
     * 更新文档状态
     */
    private void updateDocumentStatus(Document document, DocumentReview review) {
        if (review.isPassed()) {
            // 初审通过，等待复审
            document.setStatus(DocumentStatus.PENDING);
            document.setAiAnalysisStatus("INITIAL_APPROVED");
            rewardUploadPoints(document);
        } else {
            // 初审未通过
            document.setStatus(DocumentStatus.REJECTED);
            document.setRejectionReason(review.getRejectionReason());
            document.setAiAnalysisStatus("INITIAL_REJECTED");
        }
        documentRepository.save(document);
    }

    private void rewardUploadPoints(Document document) {
        try {
            pointsService.rewardUploadPoints(document.getUploaderId(), document.getId());
        } catch (Exception e) {
            log.warn("上传积分奖励失败，用户ID: {}, 文档ID: {}", document.getUploaderId(), document.getId(), e);
        }
    }

    /**
     * 相似度检测结果
     */    private static class SimilarityResult {
        private final double score;
        private final Long similarDocumentId;

        public SimilarityResult(double score, Long similarDocumentId) {
            this.score = score;
            this.similarDocumentId = similarDocumentId;
        }

        public double getScore() {
            return score;
        }

        public Long getSimilarDocumentId() {
            return similarDocumentId;
        }
    }
}
