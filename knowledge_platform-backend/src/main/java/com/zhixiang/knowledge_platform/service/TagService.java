package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.dto.response.TagInfoResponse;
import com.zhixiang.knowledge_platform.entity.Tag;
import com.zhixiang.knowledge_platform.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签管理服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 获取所有标签
     */
    public List<TagInfoResponse> getAllTags() {
        List<Tag> tags = tagRepository.findAll(Sort.by(Sort.Direction.DESC, "usageCount"));
        return tags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 分页获取标签列表
     */
    public PageResponse<TagInfoResponse> getTags(int page, int size, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Tag> tagPage = tagRepository.findAll(pageable);

        List<TagInfoResponse> tagList = tagPage.getContent().stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.fromPage(tagPage, tagList);
    }

    /**
     * 获取热门标签
     */
    public List<TagInfoResponse> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> popularTags = tagRepository.findPopularTags(pageable);

        return popularTags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据名称搜索标签
     */
    public List<TagInfoResponse> searchTagsByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return getAllTags();
        }

        List<Tag> tags = tagRepository.findByNameContainingIgnoreCase(keyword.trim());
        return tags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取标签
     */
    public TagInfoResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在，ID: " + id));
        return TagInfoResponse.fromEntity(tag);
    }

    /**
     * 根据名称获取标签
     */
    public TagInfoResponse getTagByName(String name) {
        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("标签不存在，名称: " + name));
        return TagInfoResponse.fromEntity(tag);
    }

    /**
     * 创建标签
     */
    @Transactional
    public TagInfoResponse createTag(String name, String color, String description) {
        log.info("创建标签，名称: {}", name);

        // 验证标签名称
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("标签名称不能为空");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() > 20) {
            throw new IllegalArgumentException("标签名称不能超过20个字符");
        }

        // 检查标签是否已存在
        if (tagRepository.findByName(trimmedName).isPresent()) {
            throw new RuntimeException("标签已存在: " + trimmedName);
        }

        Tag tag = new Tag();
        tag.setName(trimmedName);
        tag.setColor(StringUtils.hasText(color) ? color : "#409EFF"); // 默认蓝色
        tag.setDescription(description);
        tag.setUsageCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());

        Tag savedTag = tagRepository.save(tag);
        log.info("标签创建成功，ID: {}", savedTag.getId());

        return TagInfoResponse.fromEntity(savedTag);
    }

    /**
     * 更新标签信息
     */
    @Transactional
    public TagInfoResponse updateTag(Long id, String name, String color, String description) {
        log.info("更新标签，ID: {}", id);

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 更新名称
        if (StringUtils.hasText(name)) {
            String trimmedName = name.trim();
            if (trimmedName.length() > 20) {
                throw new IllegalArgumentException("标签名称不能超过20个字符");
            }

            if (!trimmedName.equals(tag.getName())) {
                // 检查新名称是否已存在
                if (tagRepository.findByName(trimmedName).isPresent()) {
                    throw new RuntimeException("标签名称已存在: " + trimmedName);
                }
                tag.setName(trimmedName);
            }
        }

        // 更新颜色
        if (StringUtils.hasText(color)) {
            tag.setColor(color);
        }

        // 更新描述
        if (description != null) {
            tag.setDescription(description);
        }

        tag.setUpdatedAt(LocalDateTime.now());

        Tag updatedTag = tagRepository.save(tag);
        log.info("标签更新成功，ID: {}", id);

        return TagInfoResponse.fromEntity(updatedTag);
    }

    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Long id) {
        log.info("删除标签，ID: {}", id);

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 强制删除标签，不检查使用次数（管理员可以删除任何标签）
        tagRepository.delete(tag);
        log.info("标签删除成功，ID: {}", id);
    }

    /**
     * 增加标签使用次数
     */
    @Transactional
    public void incrementTagUsage(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag != null) {
            tag.setUsageCount(tag.getUsageCount() + 1);
            tag.setUpdatedAt(LocalDateTime.now());
            tagRepository.save(tag);
        }
    }

    /**
     * 减少标签使用次数
     */
    @Transactional
    public void decrementTagUsage(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag != null && tag.getUsageCount() > 0) {
            tag.setUsageCount(tag.getUsageCount() - 1);
            tag.setUpdatedAt(LocalDateTime.now());
            tagRepository.save(tag);
        }
    }

    /**
     * 初始化默认标签
     */
    @Transactional
    public void initializeDefaultTags() {
        log.info("初始化默认标签");

        // 默认标签数据
        String[][] defaultTags = {
            {"课件", "#409EFF", "课程教学材料和讲义"},
            {"作业", "#67C23A", "课程作业和练习题"},
            {"考试资料", "#E6A23C", "考试题目和复习资料"},
            {"参考书", "#F56C6C", "教学参考书籍和资料"},
            {"实验报告", "#909399", "实验过程和结果报告"},
            {"期末复习", "#409EFF", "期末考试复习材料"},
            {"课程设计", "#67C23A", "课程设计项目文档"},
            {"毕业设计", "#E6A23C", "毕业论文和设计资料"},
            {"学习笔记", "#9C27B0", "个人学习心得和笔记"},
            {"题库", "#FF5722", "练习题和模拟题集"}
        };

        for (String[] tagData : defaultTags) {
            // 检查标签是否已存在
            if (tagRepository.findByName(tagData[0]).isEmpty()) {
                Tag tag = new Tag();
                tag.setName(tagData[0]);
                tag.setColor(tagData[1]);
                tag.setDescription(tagData[2]);
                tag.setUsageCount(0);
                tag.setCreatedAt(LocalDateTime.now());
                tag.setUpdatedAt(LocalDateTime.now());
                tagRepository.save(tag);
            }
        }

        log.info("默认标签初始化完成，处理了{}个标签", defaultTags.length);
    }

    /**
     * 批量创建标签
     */
    @Transactional
    public List<TagInfoResponse> createTags(List<String> tagNames) {
        log.info("批量创建标签，数量: {}", tagNames.size());

        List<Tag> tags = tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct() // 去重
                .filter(name -> name.length() <= 20)
                .map(name -> {
                    // 如果标签已存在，则跳过
                    if (tagRepository.findByName(name).isPresent()) {
                        return null;
                    }

                    Tag tag = new Tag();
                    tag.setName(name);
                    tag.setColor("#409EFF");
                    tag.setUsageCount(0);
                    tag.setCreatedAt(LocalDateTime.now());
                    tag.setUpdatedAt(LocalDateTime.now());
                    return tag;
                })
                .filter(tag -> tag != null)
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            return List.of();
        }

        List<Tag> savedTags = tagRepository.saveAll(tags);
        log.info("批量标签创建成功，创建数量: {}", savedTags.size());

        return savedTags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取或创建标签
     */
    @Transactional
    public Tag getOrCreateTag(String name) {
        String trimmedName = name.trim();

        return tagRepository.findByName(trimmedName)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(trimmedName);
                    newTag.setColor("#409EFF");
                    newTag.setUsageCount(0);
                    newTag.setCreatedAt(LocalDateTime.now());
                    newTag.setUpdatedAt(LocalDateTime.now());
                    return tagRepository.save(newTag);
                });
    }

    /**
     * 获取未使用的标签
     */
    public List<TagInfoResponse> getUnusedTags() {
        log.info("开始查询未使用的标签");
        try {
            List<Tag> unusedTags = tagRepository.findByUsageCount(0);
            log.info("成功查询到 {} 个未使用的标签", unusedTags.size());

            return unusedTags.stream()
                    .map(TagInfoResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询未使用标签时发生错误", e);
            throw new RuntimeException("查询未使用标签失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理未使用的标签
     */
    @Transactional
    public int cleanUnusedTags() {
        log.info("清理未使用的标签");

        List<Tag> unusedTags = tagRepository.findByUsageCount(0);
        if (!unusedTags.isEmpty()) {
            tagRepository.deleteAll(unusedTags);
            log.info("清理完成，删除标签数量: {}", unusedTags.size());
            return unusedTags.size();
        }

        return 0;
    }

    /**
     * 获取标签统计信息
     */
    public Object getTagStatistics() {
        Long totalTags = tagRepository.count();
        Long usedTags = tagRepository.countByUsageCountGreaterThan(0);
        Long unusedTags = totalTags - usedTags;

        // 获取最热门的标签
        List<Tag> topTags = tagRepository.findPopularTags(PageRequest.of(0, 5));

        return java.util.Map.of(
            "totalTags", totalTags,
            "usedTags", usedTags,
            "unusedTags", unusedTags,
            "topTags", topTags.stream()
                    .map(TagInfoResponse::fromEntity)
                    .collect(Collectors.toList())
        );
    }

    /**
     * 根据使用次数范围查询标签
     */
    public List<TagInfoResponse> getTagsByUsageRange(int minUsage, int maxUsage) {
        List<Tag> tags = tagRepository.findByUsageCountBetweenOrderByUsageCountDesc(minUsage, maxUsage);
        return tags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐标签（基于使用频率）
     */
    public List<TagInfoResponse> getRecommendedTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> recommendedTags = tagRepository.findRecommendedTags(pageable);

        return recommendedTags.stream()
                .map(TagInfoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 创建排序对象
     */
    private Sort createSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "usageCount");
        }

        return switch (sortBy.toLowerCase()) {
            case "usage" -> Sort.by(Sort.Direction.DESC, "usageCount");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "created" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "usageCount");
        };
    }

    /**
     * 验证标签名称
     */
    public boolean isValidTagName(String name) {
        return StringUtils.hasText(name) &&
               name.trim().length() > 0 &&
               name.trim().length() <= 20;
    }

    /**
     * 标准化标签名称
     */
    public String normalizeTagName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim().toLowerCase().replaceAll("\\s+", "-");
    }

    /**
     * 获取标签总数
     */
    public Long getTotalTagCount() {
        return tagRepository.count();
    }
}