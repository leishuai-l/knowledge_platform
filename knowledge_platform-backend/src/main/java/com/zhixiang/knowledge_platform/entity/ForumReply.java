package com.zhixiang.knowledge_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 论坛回复实体类
 */
@Entity
@Table(name = "forum_replies")
@Data
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private ForumTopic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // Parent reply for nested comments (can be null if it's a top-level reply to the topic)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForumReply parent;

    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    // 0: normal, 1: hidden
    @Column(nullable = false)
    private Integer status = 0;
    
    // To easily fetch all replies under a root comment if we want a 2-level structure
    // But for now, let's keep it simple. If needed, we can add rootParentId.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
