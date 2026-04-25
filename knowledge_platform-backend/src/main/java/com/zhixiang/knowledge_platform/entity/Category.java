package com.zhixiang.knowledge_platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类实体类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = false)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 父分类关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    @JsonIgnore  // 避免JSON序列化时的循环引用
    private Category parent;

    // 子分类关联
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // 避免JSON序列化时的懒加载异常
    private List<Category> children;

    /**
     * 是否为顶级分类
     */
    public boolean isTopLevel() {
        return this.parentId == null || this.level == 1;
    }

    /**
     * 获取完整路径名称
     */
    public String getFullPath() {
        if (isTopLevel()) {
            return this.name;
        }
        // 这里可以根据需要实现完整路径逻辑
        return this.name;
    }
}