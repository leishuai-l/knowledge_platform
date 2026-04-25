package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long> {
    Page<ForumTopic> findByCategoryId(Long categoryId, Pageable pageable);
    Page<ForumTopic> findByAuthorId(Long authorId, Pageable pageable);
    Page<ForumTopic> findByTagsName(String tagName, Pageable pageable);
    Page<ForumTopic> findByCategoryIdAndTagsName(Long categoryId, String tagName, Pageable pageable);
    Page<ForumTopic> findByAuthorIdAndStatus(Long authorId, Integer status, Pageable pageable);
    Page<ForumTopic> findByStatus(Integer status, Pageable pageable);
    Page<ForumTopic> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);
    Page<ForumTopic> findByTagsNameAndStatus(String tagName, Integer status, Pageable pageable);
    Page<ForumTopic> findByCategoryIdAndTagsNameAndStatus(Long categoryId, String tagName, Integer status, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("UPDATE ForumTopic t SET t.likeCount = t.likeCount + 1 WHERE t.id = :topicId")
    void incrementLikeCount(@Param("topicId") Long topicId);

    @Modifying
    @Query("UPDATE ForumTopic t SET t.likeCount = CASE WHEN t.likeCount > 0 THEN t.likeCount - 1 ELSE 0 END WHERE t.id = :topicId")
    void decrementLikeCount(@Param("topicId") Long topicId);
}
