package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.common.ApiResponse;
import com.zhixiang.knowledge_platform.dto.response.CategoryInfoResponse;
import com.zhixiang.knowledge_platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器
 *
 * @author leishuai
 * @version 0.0.1-SNAPSHOT
 * @since 2025-09-10
 */
@Tag(name = "分类管理", description = "分类的增删改查、树形结构管理等功能")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类树形结构", description = "获取完整的分类树形结构，包含所有层级")
    @GetMapping("/tree")
    public ApiResponse<List<CategoryInfoResponse>> getCategoryTree(
            @Parameter(description = "是否包含禁用的分类，仅管理员可用")
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        try {
            List<CategoryInfoResponse> categoryTree;
            if (includeDisabled) {
                // 需要管理员权限才能查看禁用的分类
                categoryTree = categoryService.getCategoryTreeForAdmin();
            } else {
                categoryTree = categoryService.getCategoryTree();
            }
            return ApiResponse.success(categoryTree);
        } catch (Exception e) {
            log.error("获取分类树形结构失败", e);
            return ApiResponse.error("获取分类树形结构失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有分类树形结构", description = "获取包含禁用分类的完整树形结构，无权限限制")
    @GetMapping("/all-tree")
    public ApiResponse<List<CategoryInfoResponse>> getAllCategoryTree() {
        try {
            List<CategoryInfoResponse> categoryTree = categoryService.getAllCategoryTree();
            return ApiResponse.success(categoryTree);
        } catch (Exception e) {
            log.error("获取所有分类树形结构失败", e);
            return ApiResponse.error("获取分类树形结构失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取顶级分类", description = "获取所有一级分类")
    @GetMapping("/top-level")
    public ApiResponse<List<CategoryInfoResponse>> getTopLevelCategories() {
        try {
            List<CategoryInfoResponse> categories = categoryService.getTopLevelCategories();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取顶级分类失败", e);
            return ApiResponse.error("获取顶级分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取子分类", description = "获取指定父分类下的所有子分类")
    @GetMapping("/{parentId}/children")
    public ApiResponse<List<CategoryInfoResponse>> getSubCategories(@PathVariable Long parentId) {
        try {
            List<CategoryInfoResponse> categories = categoryService.getSubCategories(parentId);
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取子分类失败, 父分类ID: {}", parentId, e);
            return ApiResponse.error("获取子分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取分类详情", description = "获取指定ID的分类详细信息")
    @GetMapping("/{id}")
    public ApiResponse<CategoryInfoResponse> getCategoryById(@PathVariable Long id) {
        try {
            CategoryInfoResponse category = categoryService.getCategoryById(id);
            return ApiResponse.success(category);
        } catch (Exception e) {
            log.error("获取分类详情失败, ID: {}", id, e);
            return ApiResponse.error("获取分类详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据名称查找分类", description = "根据分类名称查找分类")
    @GetMapping("/name/{name}")
    public ApiResponse<CategoryInfoResponse> getCategoryByName(@PathVariable String name) {
        try {
            CategoryInfoResponse category = categoryService.getCategoryByName(name);
            return ApiResponse.success(category);
        } catch (Exception e) {
            log.error("根据名称查找分类失败, 名称: {}", name, e);
            return ApiResponse.error("查找分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据层级获取分类", description = "获取指定层级的所有分类")
    @GetMapping("/level/{level}")
    public ApiResponse<List<CategoryInfoResponse>> getCategoriesByLevel(@PathVariable Integer level) {
        try {
            if (level < 1 || level > 3) {
                return ApiResponse.error("层级必须在1-3之间");
            }
            List<CategoryInfoResponse> categories = categoryService.getCategoriesByLevel(level);
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("根据层级获取分类失败, 层级: {}", level, e);
            return ApiResponse.error("获取分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "搜索分类", description = "根据关键词搜索分类名称")
    @GetMapping("/search")
    public ApiResponse<List<CategoryInfoResponse>> searchCategories(
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword) {
        try {
            List<CategoryInfoResponse> categories = categoryService.searchCategories(keyword);
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("搜索分类失败, 关键词: {}", keyword, e);
            return ApiResponse.error("搜索分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取有文档的分类", description = "获取包含已审核文档的所有分类")
    @GetMapping("/with-documents")
    public ApiResponse<List<CategoryInfoResponse>> getCategoriesWithDocuments() {
        try {
            List<CategoryInfoResponse> categories = categoryService.getCategoriesWithDocuments();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取有文档的分类失败", e);
            return ApiResponse.error("获取分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取分类路径", description = "获取从根分类到指定分类的完整路径")
    @GetMapping("/{id}/path")
    public ApiResponse<String> getCategoryPath(@PathVariable Long id) {
        try {
            String path = categoryService.getCategoryPath(id);
            return ApiResponse.<String>success(path);
        } catch (Exception e) {
            log.error("获取分类路径失败, ID: {}", id, e);
            return ApiResponse.error("获取分类路径失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建分类", description = "创建新的分类")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryInfoResponse> createCategory(
            @Parameter(description = "分类名称", required = true)
            @RequestParam String name,
            @Parameter(description = "分类描述")
            @RequestParam(required = false) String description,
            @Parameter(description = "父分类ID（为空则创建顶级分类）")
            @RequestParam(required = false) Long parentId) {
        try {
            CategoryInfoResponse category = categoryService.createCategory(name, description, parentId);
            return ApiResponse.success(category, "分类创建成功");
        } catch (Exception e) {
            log.error("创建分类失败, 名称: {}, 父分类ID: {}", name, parentId, e);
            return ApiResponse.error("创建分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新分类信息", description = "更新分类的名称、描述和状态")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryInfoResponse> updateCategory(
            @PathVariable Long id,
            @Parameter(description = "分类名称")
            @RequestParam(required = false) String name,
            @Parameter(description = "分类描述")
            @RequestParam(required = false) String description,
            @Parameter(description = "分类状态")
            @RequestParam(required = false) Boolean isActive) {
        try {
            CategoryInfoResponse category = categoryService.updateCategory(id, name, description, isActive);
            return ApiResponse.success(category, "分类信息更新成功");
        } catch (Exception e) {
            log.error("更新分类信息失败, ID: {}", id, e);
            return ApiResponse.error("更新分类信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除分类", description = "删除指定分类（软删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ApiResponse.successMessage("分类删除成功");
        } catch (Exception e) {
            log.error("删除分类失败, ID: {}", id, e);
            return ApiResponse.error("删除分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新分类排序", description = "更新分类在同级中的排序位置")
    @PutMapping("/{id}/sort-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateCategorySortOrder(
            @PathVariable Long id,
            @Parameter(description = "新的排序值", required = true)
            @RequestParam Integer sortOrder) {
        try {
            categoryService.updateCategorySortOrder(id, sortOrder);
            return ApiResponse.successMessage("分类排序更新成功");
        } catch (Exception e) {
            log.error("更新分类排序失败, ID: {}, 排序值: {}", id, sortOrder, e);
            return ApiResponse.error("更新分类排序失败: " + e.getMessage());
        }
    }

    @Operation(summary = "移动分类", description = "将分类移动到新的父级下")
    @PutMapping("/{id}/move")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> moveCategory(
            @PathVariable Long id,
            @Parameter(description = "新的父分类ID（为空则移动到顶级）")
            @RequestParam(required = false) Long newParentId) {
        try {
            categoryService.moveCategory(id, newParentId);
            return ApiResponse.successMessage("分类移动成功");
        } catch (Exception e) {
            log.error("移动分类失败, ID: {}, 新父分类ID: {}", id, newParentId, e);
            return ApiResponse.error("移动分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量启用/禁用分类", description = "批量操作分类的启用状态")
    @PutMapping("/batch/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleCategoriesActive(
            @Parameter(description = "分类ID列表", required = true)
            @RequestParam List<Long> categoryIds,
            @Parameter(description = "是否启用", required = true)
            @RequestParam boolean active) {
        try {
            categoryService.toggleCategoriesActive(categoryIds, active);
            return ApiResponse.successMessage(active ? "分类批量启用成功" : "分类批量禁用成功");
        } catch (Exception e) {
            log.error("批量{}分类失败, IDs: {}", active ? "启用" : "禁用", categoryIds, e);
            return ApiResponse.error("批量操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检查分类是否存在", description = "检查指定ID的分类是否存在且启用")
    @GetMapping("/{id}/exists")
    public ApiResponse<Boolean> categoryExists(@PathVariable Long id) {
        try {
            boolean exists = categoryService.categoryExists(id);
            return ApiResponse.success(exists);
        } catch (Exception e) {
            log.error("检查分类存在性失败, ID: {}", id, e);
            return ApiResponse.error("检查失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取所有分类", description = "管理员获取所有分类，包括已禁用的")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CategoryInfoResponse>> getAllCategoriesForAdmin() {
        try {
            List<CategoryInfoResponse> categories = categoryService.getAllCategoriesForAdmin();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("管理员获取所有分类失败", e);
            return ApiResponse.error("获取分类失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：获取分类树形结构", description = "管理员获取完整的分类树形结构，包含已禁用的分类")
    @GetMapping("/admin/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CategoryInfoResponse>> getCategoryTreeForAdmin() {
        try {
            List<CategoryInfoResponse> categoryTree = categoryService.getCategoryTreeForAdmin();
            return ApiResponse.success(categoryTree);
        } catch (Exception e) {
            log.error("管理员获取分类树形结构失败", e);
            return ApiResponse.error("获取分类树形结构失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取分类统计信息", description = "获取每个分类下的文档数量统计")
    @GetMapping("/statistics")
    public ApiResponse<List<Object[]>> getCategoryStatistics() {
        try {
            List<Object[]> statistics = categoryService.getCategoryStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取分类统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }
}