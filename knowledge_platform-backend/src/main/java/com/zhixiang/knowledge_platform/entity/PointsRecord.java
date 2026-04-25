package com.zhixiang.knowledge_platform.entity;

import com.zhixiang.knowledge_platform.enums.PointsSource;
import com.zhixiang.knowledge_platform.enums.PointsType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 积分记录实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "points_records")
@Data
@EqualsAndHashCode(callSuper = false)
public class PointsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PointsType type;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private PointsSource source;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 是否为获得积分
     */
    public boolean isEarn() {
        return PointsType.EARN.equals(this.type);
    }

    /**
     * 是否为消费积分
     */
    public boolean isSpend() {
        return PointsType.SPEND.equals(this.type);
    }

    /**
     * 创建积分记录
     */
    public static PointsRecord createRecord(Long userId, Integer points, PointsType type,
                                          PointsSource source, Long referenceId, String description) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setPoints(points);
        record.setSource(source);
        record.setReferenceId(referenceId);
        record.setDescription(description);
        return record;
    }
}