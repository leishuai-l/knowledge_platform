package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.RatingCreateRequest;
import com.zhixiang.knowledge_platform.dto.response.RatingInfoResponse;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.Rating;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 评分管理服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private final RatingRepository ratingRepository;
    private final DocumentRepository documentRepository;
    private final PointsService pointsService;
    private final DownloadRecordService downloadRecordService;

    /**
     * 创建或更新评分
     */
    @Transactional
    public RatingInfoResponse createOrUpdateRating(Long documentId, Long userId, RatingCreateRequest request) {
        log.info("创建或更新评分，文档ID: {}, 用户ID: {}, 评分: {}", documentId, userId, request.getScore());

        // 验证评分范围
        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }

        // 检查文档是否存在且已审核
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (!document.isApproved()) {
            throw new RuntimeException("只能对已审核的文档进行评分");
        }

        // 检查用户是否已下载过该文档
        boolean hasDownloaded = downloadRecordService.hasUserDownloadedDocument(userId, documentId);
        if (!hasDownloaded) {
            throw new RuntimeException("只有下载过文档的用户才能进行评分");
        }

        // 检查是否已经评分过
        Rating rating = ratingRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElse(null);

        boolean isNewRating = (rating == null);

        if (rating == null) {
            // 新评分
            rating = new Rating();
            rating.setDocumentId(documentId);
            rating.setUserId(userId);
            rating.setCreatedAt(LocalDateTime.now());
        }

        rating.setScore(request.getScore());
        rating.setComment(request.getComment());
        rating.setUpdatedAt(LocalDateTime.now());

        Rating savedRating = ratingRepository.save(rating);

        // 更新文档评分统计
        updateDocumentRatingStats(documentId);

        // 如果是新的高评分（4分以上），奖励上传者积分
        if (isNewRating && request.getScore() >= 4) {
            try {
                pointsService.rewardRatingPoints(document.getUploaderId(), documentId);
            } catch (Exception e) {
                log.warn("高评分积分奖励失败，上传者ID: {}, 文档ID: {}", document.getUploaderId(), documentId, e);
            }
        }

        log.info("评分{}成功，ID: {}", isNewRating ? "创建" : "更新", savedRating.getId());
        return RatingInfoResponse.fromEntitySimple(savedRating);
    }

    /**
     * 获取文档的评分列表
     */
    public List<Rating> getDocumentRatings(Long documentId) {
        return ratingRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * 获取用户对文档的评分
     */
    public Rating getUserRatingForDocument(Long documentId, Long userId) {
        return ratingRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElse(null);
    }

    /**
     * 获取用户的所有评分记录
     */
    public List<Rating> getUserRatings(Long userId) {
        // 使用JOIN FETCH查询避免懒加载问题
        return ratingRepository.findByUserIdWithDocumentOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除评分
     */
    @Transactional
    public void deleteRating(Long ratingId, Long userId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("评分不存在"));

        // 检查权限：只有评分者本人可以删除
        if (!rating.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此评分");
        }

        ratingRepository.delete(rating);

        // 更新文档评分统计
        updateDocumentRatingStats(rating.getDocumentId());

        log.info("评分删除成功，ID: {}, 用户ID: {}", ratingId, userId);
    }

    /**
     * 管理员删除评分
     */
    @Transactional
    public void adminDeleteRating(Long ratingId, String reason) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("评分不存在"));

        ratingRepository.delete(rating);

        // 更新文档评分统计
        updateDocumentRatingStats(rating.getDocumentId());

        log.info("管理员删除评分，ID: {}, 原因: {}", ratingId, reason);
    }

    /**
     * 更新文档评分统计信息
     */
    @Transactional
    public void updateDocumentRatingStats(Long documentId) {
        List<Rating> ratings = ratingRepository.findByDocumentId(documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (ratings.isEmpty()) {
            document.setRatingCount(0);
            document.setRatingAverage(java.math.BigDecimal.ZERO);
        } else {
            document.setRatingCount(ratings.size());

            OptionalDouble average = ratings.stream()
                    .mapToInt(Rating::getScore)
                    .average();

            document.setRatingAverage(java.math.BigDecimal.valueOf(average.orElse(0.0)));
        }

        documentRepository.save(document);
        log.debug("文档评分统计更新完成，文档ID: {}, 评分数: {}, 平均分: {}",
                documentId, document.getRatingCount(), document.getRatingAverage());
    }

    /**
     * 获取文档平均评分
     */
    public Double getDocumentAverageRating(Long documentId) {
        List<Rating> ratings = ratingRepository.findByDocumentId(documentId);

        if (ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 获取文档评分统计
     */
    public Object getDocumentRatingStatistics(Long documentId) {
        List<Rating> ratings = ratingRepository.findByDocumentId(documentId);

        if (ratings.isEmpty()) {
            return java.util.Map.of(
                "total", 0,
                "average", 0.0,
                "distribution", new int[]{0, 0, 0, 0, 0}
            );
        }

        // 统计各分值的分布
        int[] distribution = new int[5]; // 1-5分
        for (Rating rating : ratings) {
            distribution[rating.getScore() - 1]++;
        }

        double average = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);

        return java.util.Map.of(
            "total", ratings.size(),
            "average", Math.round(average * 10.0) / 10.0, // 保留一位小数
            "distribution", distribution
        );
    }

    /**
     * 检查用户是否已评分过文档
     */
    public boolean hasUserRatedDocument(Long documentId, Long userId) {
        return ratingRepository.findByDocumentIdAndUserId(documentId, userId).isPresent();
    }

    /**
     * 获取用户评分统计
     */
    public Object getUserRatingStatistics(Long userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);

        if (ratings.isEmpty()) {
            return java.util.Map.of(
                "totalRatings", 0,
                "averageGiven", 0.0,
                "mostCommonScore", 0
            );
        }

        double averageGiven = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);

        // 统计最常给的分数
        java.util.Map<Integer, Long> scoreCount = ratings.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Rating::getScore,
                    java.util.stream.Collectors.counting()
                ));

        Integer mostCommonScore = scoreCount.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(0);

        return java.util.Map.of(
            "totalRatings", ratings.size(),
            "averageGiven", Math.round(averageGiven * 10.0) / 10.0,
            "mostCommonScore", mostCommonScore
        );
    }

    /**
     * 获取最新评分列表
     */
    public List<Rating> getRecentRatings(int limit) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, limit);
        return ratingRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    }

    /**
     * 获取高分文档列表
     */
    public List<Object[]> getTopRatedDocuments(int limit) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, limit);
        return ratingRepository.findTopRatedDocuments(pageable);
    }
}