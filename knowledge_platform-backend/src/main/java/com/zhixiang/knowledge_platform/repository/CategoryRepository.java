package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问接口
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 查询所有顶级分类（一级分类）
     */
    List<Category> findByParentIdIsNullAndIsActiveOrderBySortOrder(Boolean isActive);

    /**
     * 查询指定父分类下的子分类
     */
    List<Category> findByParentIdAndIsActiveOrderBySortOrder(Long parentId, Boolean isActive);

    /**
     * 查询指定层级的分类
     */
    List<Category> findByLevelAndIsActiveOrderBySortOrder(Integer level, Boolean isActive);

    /**
     * 根据名称查找分类
     */
    Optional<Category> findByNameAndIsActive(String name, Boolean isActive);

    /**
     * 检查分类名称在同一父级下是否存在
     */
    boolean existsByNameAndParentId(String name, Long parentId);

    /**
     * 查询所有启用的分类
     */
    List<Category> findByIsActiveTrueOrderByLevelAscSortOrderAsc();

    /**
     * 查询分类树结构
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.level ASC, c.sortOrder ASC")
    List<Category> findCategoryTree();

    /**
     * 查询指定分类的所有子分类（递归）
     */
    @Query(value = "WITH RECURSIVE category_tree AS (" +
                   "SELECT id, name, parent_id, level FROM categories WHERE id = :categoryId " +
                   "UNION ALL " +
                   "SELECT c.id, c.name, c.parent_id, c.level FROM categories c " +
                   "INNER JOIN category_tree ct ON c.parent_id = ct.id" +
                   ") SELECT * FROM category_tree WHERE id != :categoryId",
           nativeQuery = true)
    List<Category> findAllSubCategories(@Param("categoryId") Long categoryId);

    /**
     * 查询分类路径（从根到指定分类）
     */
    @Query(value = "WITH RECURSIVE category_path AS (" +
                   "SELECT id, name, parent_id, level, CAST(name AS CHAR(1000)) as path " +
                   "FROM categories WHERE parent_id IS NULL " +
                   "UNION ALL " +
                   "SELECT c.id, c.name, c.parent_id, c.level, CONCAT(cp.path, ' > ', c.name) " +
                   "FROM categories c INNER JOIN category_path cp ON c.parent_id = cp.id" +
                   ") SELECT * FROM category_path WHERE id = :categoryId",
           nativeQuery = true)
    List<Object[]> findCategoryPath(@Param("categoryId") Long categoryId);

    /**
     * 统计每个分类下的文档数量
     */
    @Query("SELECT c.id, c.name, COUNT(d.id) FROM Category c " +
           "LEFT JOIN Document d ON c.id = d.categoryId AND d.status = 'APPROVED' " +
           "WHERE c.isActive = true GROUP BY c.id, c.name ORDER BY c.sortOrder")
    List<Object[]> countDocumentsByCategory();

    /**
     * 查询有文档的分类
     */
    @Query("SELECT DISTINCT c FROM Category c " +
           "INNER JOIN Document d ON c.id = d.categoryId " +
           "WHERE c.isActive = true AND d.status = 'APPROVED' " +
           "ORDER BY c.level ASC, c.sortOrder ASC")
    List<Category> findCategoriesWithDocuments();

    /**
     * 根据名称模糊查询分类
     */
    List<Category> findByNameContainingIgnoreCaseAndIsActiveOrderBySortOrder(String name, Boolean isActive);

    /**
     * 获取最大排序值
     */
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.parentId = :parentId")
    Integer findMaxSortOrderByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有分类（包括禁用的）按层级和排序（排除已删除的）
     */
    List<Category> findAllByIsDeletedFalseOrderByLevelAscSortOrderAsc();

    /**
     * 获取指定分类及其所有子分类的ID列表
     */
    @Query(value = "WITH RECURSIVE category_tree AS (" +
                   "SELECT id FROM categories WHERE id = :categoryId " +
                   "UNION ALL " +
                   "SELECT c.id FROM categories c " +
                   "INNER JOIN category_tree ct ON c.parent_id = ct.id" +
                   ") SELECT id FROM category_tree",
           nativeQuery = true)
    List<Long> findCategoryAndDescendantIds(@Param("categoryId") Long categoryId);

    /**
     * 查询所有分类（包括禁用的）按层级和排序
     */
    List<Category> findAllByOrderByLevelAscSortOrderAsc();
}