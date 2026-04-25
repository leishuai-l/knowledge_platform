package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标签数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 根据名称查找标签
     */
    Optional<Tag> findByName(String name);

    /**
     * 检查标签名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据名称模糊查询标签
     */
    List<Tag> findByNameContainingIgnoreCase(String name);

    /**
     * 分页模糊查询标签
     */
    Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 查询热门标签（按使用次数降序）
     */
    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC")
    List<Tag> findPopularTags(Pageable pageable);

    /**
     * 查询使用次数大于指定值的标签
     */
    List<Tag> findByUsageCountGreaterThan(Integer usageCount);

    /**
     * 查询使用次数在指定范围内的标签
     */
    List<Tag> findByUsageCountBetween(Integer minUsage, Integer maxUsage);

    /**
     * 按使用次数降序查询所有标签
     */
    List<Tag> findAllByOrderByUsageCountDesc();

    /**
     * 按创建时间降序查询标签
     */
    List<Tag> findAllByOrderByCreatedAtDesc();

    /**
     * 查询标签云数据（热门标签）
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.usageCount DESC")
    List<Tag> findTagCloudData(Pageable pageable);

    /**
     * 根据ID列表查询标签
     */
    List<Tag> findByIdIn(List<Long> ids);

    /**
     * 根据名称列表查询标签
     */
    List<Tag> findByNameIn(List<String> names);

    /**
     * 查询未使用的标签 (使用JPQL避免关联查询问题)
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount = :usageCount ORDER BY t.createdAt DESC")
    List<Tag> findByUsageCount(@Param("usageCount") Integer usageCount);

    /**
     * 统计标签总数
     */
    @Query("SELECT COUNT(t) FROM Tag t")
    Long countAllTags();

    /**
     * 统计有使用的标签数
     */
    @Query("SELECT COUNT(t) FROM Tag t WHERE t.usageCount > 0")
    Long countUsedTags();

    /**
     * 查询最近创建的标签
     */
    @Query("SELECT t FROM Tag t ORDER BY t.createdAt DESC")
    List<Tag> findRecentTags(Pageable pageable);

    /**
     * 搜索标签（支持模糊匹配）
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Tag> searchTags(@Param("keyword") String keyword);

    /**
     * 分页搜索标签
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.usageCount DESC")
    Page<Tag> searchTags(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计使用次数大于指定值的标签数量
     */
    Long countByUsageCountGreaterThan(Integer usageCount);

    /**
     * 查询使用次数在指定范围内的标签（按使用次数降序）
     */
    List<Tag> findByUsageCountBetweenOrderByUsageCountDesc(Integer minUsage, Integer maxUsage);

    /**
     * 查询推荐标签（基于使用频率）
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.usageCount DESC, t.createdAt DESC")
    List<Tag> findRecommendedTags(Pageable pageable);
}