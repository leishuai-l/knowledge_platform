package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.response.CategoryInfoResponse;
import com.zhixiang.knowledge_platform.entity.Category;
import com.zhixiang.knowledge_platform.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类管理服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 获取所有顶级分类（一级分类）
     */
    public List<CategoryInfoResponse> getTopLevelCategories() {
        List<Category> categories = categoryRepository.findByParentIdIsNullAndIsActiveOrderBySortOrder(true);
        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定父分类下的子分类
     */
    public List<CategoryInfoResponse> getSubCategories(Long parentId) {
        List<Category> categories = categoryRepository.findByParentIdAndIsActiveOrderBySortOrder(parentId, true);
        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 获取分类树形结构
     */
    public List<CategoryInfoResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findCategoryTree();

        // 构建树形结构
        Map<Long, CategoryInfoResponse> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        CategoryInfoResponse::fromEntitySimple
                ));

        List<CategoryInfoResponse> rootCategories = new ArrayList<>();

        for (CategoryInfoResponse category : categoryMap.values()) {
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                CategoryInfoResponse parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }

        return rootCategories;
    }

    /**
     * 获取所有分类的树形结构（包括禁用的）
     */
    public List<CategoryInfoResponse> getAllCategoryTree() {
        List<Category> allCategories = categoryRepository.findAllByIsDeletedFalseOrderByLevelAscSortOrderAsc();

        // 构建树形结构
        Map<Long, CategoryInfoResponse> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        CategoryInfoResponse::fromEntitySimple
                ));

        List<CategoryInfoResponse> rootCategories = new ArrayList<>();

        for (CategoryInfoResponse category : categoryMap.values()) {
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                CategoryInfoResponse parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }

        return rootCategories;
    }

    /**
     * 根据ID获取分类详情
     */
    public CategoryInfoResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在，ID: " + id));

        if (!category.getIsActive()) {
            throw new RuntimeException("分类已被禁用");
        }

        return CategoryInfoResponse.fromEntity(category);
    }

    /**
     * 根据名称查找分类
     */
    public CategoryInfoResponse getCategoryByName(String name) {
        Category category = categoryRepository.findByNameAndIsActive(name, true)
                .orElseThrow(() -> new RuntimeException("分类不存在，名称: " + name));

        return CategoryInfoResponse.fromEntity(category);
    }

    /**
     * 获取指定层级的分类
     */
    public List<CategoryInfoResponse> getCategoriesByLevel(Integer level) {
        List<Category> categories = categoryRepository.findByLevelAndIsActiveOrderBySortOrder(level, true);
        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 搜索分类（模糊匹配名称）
     */
    public List<CategoryInfoResponse> searchCategories(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getCategoryTree();
        }

        List<Category> categories = categoryRepository
                .findByNameContainingIgnoreCaseAndIsActiveOrderBySortOrder(keyword, true);

        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 获取有文档的分类
     */
    public List<CategoryInfoResponse> getCategoriesWithDocuments() {
        List<Category> categories = categoryRepository.findCategoriesWithDocuments();
        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 创建分类
     */
    @Transactional
    public CategoryInfoResponse createCategory(String name, String description, Long parentId) {
        log.info("创建分类，名称: {}, 父分类ID: {}", name, parentId);

        // 验证分类名称
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        // 检查同一父级下是否存在同名分类
        if (categoryRepository.existsByNameAndParentId(name, parentId)) {
            throw new RuntimeException("同一级别下已存在相同名称的分类");
        }

        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description);
        category.setParentId(parentId);
        category.setIsActive(true);

        // 设置层级
        if (parentId == null) {
            category.setLevel(1);
        } else {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("父分类不存在"));
            category.setLevel(parent.getLevel() + 1);

            // 限制最大层级为3级
            if (category.getLevel() > 3) {
                throw new RuntimeException("分类层级不能超过3级");
            }
        }

        // 设置排序值（在同级中最后）
        Integer maxSortOrder = categoryRepository.findMaxSortOrderByParentId(parentId);
        category.setSortOrder(maxSortOrder + 1);

        Category savedCategory = categoryRepository.save(category);
        log.info("分类创建成功，ID: {}", savedCategory.getId());

        return CategoryInfoResponse.fromEntity(savedCategory);
    }

    /**
     * 更新分类信息
     */
    @Transactional
    public CategoryInfoResponse updateCategory(Long id, String name, String description, Boolean isActive) {
        log.info("更新分类信息，ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        // 验证分类名称
        if (StringUtils.hasText(name)) {
            String trimmedName = name.trim();
            if (!trimmedName.equals(category.getName())) {
                // 检查同一父级下是否存在同名分类
                if (categoryRepository.existsByNameAndParentId(trimmedName, category.getParentId())) {
                    throw new RuntimeException("同一级别下已存在相同名称的分类");
                }
                category.setName(trimmedName);
            }
        }

        if (description != null) {
            category.setDescription(description);
        }

        // 更新状态
        if (isActive != null) {
            category.setIsActive(isActive);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("分类信息更新成功，ID: {}", id);

        return CategoryInfoResponse.fromEntity(updatedCategory);
    }

    /**
     * 删除分类（软删除）
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("删除分类，ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        // 检查是否有子分类
        List<Category> subCategories = categoryRepository.findByParentIdAndIsActiveOrderBySortOrder(id, true);
        if (!subCategories.isEmpty()) {
            throw new RuntimeException("无法删除包含子分类的分类，请先删除或移动子分类");
        }

        // 软删除（设置删除标记，保持isActive状态）
        category.setIsDeleted(true);
        categoryRepository.save(category);

        log.info("分类删除成功，ID: {}", id);
    }

    /**
     * 更新分类排序
     */
    @Transactional
    public void updateCategorySortOrder(Long id, Integer newSortOrder) {
        log.info("更新分类排序，ID: {}, 新排序值: {}", id, newSortOrder);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        if (newSortOrder < 0) {
            throw new IllegalArgumentException("排序值不能为负数");
        }

        category.setSortOrder(newSortOrder);
        categoryRepository.save(category);

        log.info("分类排序更新成功，ID: {}", id);
    }

    /**
     * 移动分类到新的父级
     */
    @Transactional
    public void moveCategory(Long categoryId, Long newParentId) {
        log.info("移动分类，分类ID: {}, 新父分类ID: {}", categoryId, newParentId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        if (!category.getIsActive()) {
            throw new RuntimeException("无法移动已禁用的分类");
        }

        // 验证新父分类
        if (newParentId != null) {
            Category newParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("新父分类不存在"));

            if (!newParent.getIsActive()) {
                throw new RuntimeException("新父分类已被禁用");
            }

            // 防止循环引用（不能移动到自己的子分类下）
            if (isDescendant(categoryId, newParentId)) {
                throw new RuntimeException("不能将分类移动到其子分类下");
            }

            // 检查层级限制
            if (newParent.getLevel() >= 3) {
                throw new RuntimeException("移动后的分类层级不能超过3级");
            }

            category.setLevel(newParent.getLevel() + 1);
        } else {
            category.setLevel(1);
        }

        // 检查新父级下是否存在同名分类
        if (categoryRepository.existsByNameAndParentId(category.getName(), newParentId)) {
            throw new RuntimeException("目标位置已存在相同名称的分类");
        }

        category.setParentId(newParentId);

        // 设置新的排序值
        Integer maxSortOrder = categoryRepository.findMaxSortOrderByParentId(newParentId);
        category.setSortOrder(maxSortOrder + 1);

        categoryRepository.save(category);
        log.info("分类移动成功，ID: {}", categoryId);
    }

    /**
     * 检查是否为后代分类
     */
    private boolean isDescendant(Long ancestorId, Long descendantId) {
        if (ancestorId.equals(descendantId)) {
            return true;
        }

        Category descendant = categoryRepository.findById(descendantId).orElse(null);
        if (descendant == null || descendant.getParentId() == null) {
            return false;
        }

        return isDescendant(ancestorId, descendant.getParentId());
    }

    /**
     * 获取分类路径
     */
    public String getCategoryPath(Long categoryId) {
        List<Object[]> pathResult = categoryRepository.findCategoryPath(categoryId);
        if (!pathResult.isEmpty()) {
            return (String) pathResult.get(0)[4]; // path字段在第5个位置
        }
        return "";
    }

    /**
     * 获取分类统计信息
     */
    public List<Object[]> getCategoryStatistics() {
        return categoryRepository.countDocumentsByCategory();
    }

    /**
     * 批量启用/禁用分类
     */
    @Transactional
    public void toggleCategoriesActive(List<Long> categoryIds, boolean active) {
        log.info("批量{}分类，IDs: {}", active ? "启用" : "禁用", categoryIds);

        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                if (!active && category.getChildren() != null && !category.getChildren().isEmpty()) {
                    throw new RuntimeException("无法禁用包含子分类的分类，分类ID: " + categoryId);
                }
                category.setIsActive(active);
                categoryRepository.save(category);
            }
        }

        log.info("批量操作完成");
    }

    /**
     * 检查分类是否存在
     */
    public boolean categoryExists(Long id) {
        return categoryRepository.findById(id)
                .map(Category::getIsActive)
                .orElse(false);
    }

    /**
     * 获取所有分类（包括禁用的）- 仅管理员
     */
    public List<CategoryInfoResponse> getAllCategoriesForAdmin() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(CategoryInfoResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    /**
     * 获取分类树形结构（包括禁用的）- 仅管理员
     */
    public List<CategoryInfoResponse> getCategoryTreeForAdmin() {
        List<Category> allCategories = categoryRepository.findAllByOrderByLevelAscSortOrderAsc();

        // 构建树形结构
        Map<Long, CategoryInfoResponse> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        CategoryInfoResponse::fromEntitySimple
                ));

        List<CategoryInfoResponse> rootCategories = new ArrayList<>();

        for (CategoryInfoResponse category : categoryMap.values()) {
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                CategoryInfoResponse parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }

        return rootCategories;
    }

    /**
     * 获取分类总数
     */
    public Long getTotalCategoryCount() {
        return categoryRepository.count();
    }
}