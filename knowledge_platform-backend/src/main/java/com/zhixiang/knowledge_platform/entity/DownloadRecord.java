package com.zhixiang.knowledge_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 下载记录实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "download_records")
@Data
@EqualsAndHashCode(callSuper = false)
public class DownloadRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "points_cost", nullable = false)
    private Integer pointsCost;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "download_time", nullable = false)
    private LocalDateTime downloadTime;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 是否为免费下载
     */
    public boolean isFreeDownload() {
        return this.pointsCost == 0;
    }

    /**
     * 创建下载记录
     */
    public static DownloadRecord create(Long documentId, Long userId, Integer pointsCost,
                                      String ipAddress, String userAgent) {
        DownloadRecord record = new DownloadRecord();
        record.setDocumentId(documentId);
        record.setUserId(userId);
        record.setPointsCost(pointsCost);
        record.setIpAddress(ipAddress);
        record.setUserAgent(userAgent);
        record.setDownloadTime(LocalDateTime.now());
        return record;
    }
}