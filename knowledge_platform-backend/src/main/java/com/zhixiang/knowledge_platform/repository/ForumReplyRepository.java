package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    Page<ForumReply> findByTopicId(Long topicId, Pageable pageable);
    long countByTopicId(Long topicId);
    Page<ForumReply> findByContentContaining(String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE ForumReply r SET r.likeCount = r.likeCount + 1 WHERE r.id = :replyId")
    void incrementLikeCount(@Param("replyId") Long replyId);

    @Modifying
    @Query("UPDATE ForumReply r SET r.likeCount = CASE WHEN r.likeCount > 0 THEN r.likeCount - 1 ELSE 0 END WHERE r.id = :replyId")
    void decrementLikeCount(@Param("replyId") Long replyId);
}
