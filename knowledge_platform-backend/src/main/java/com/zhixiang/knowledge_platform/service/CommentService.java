package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.CommentCreateRequest;
import com.zhixiang.knowledge_platform.dto.response.CommentInfoResponse;
import com.zhixiang.knowledge_platform.dto.response.PageResponse;
import com.zhixiang.knowledge_platform.entity.Comment;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.repository.CommentRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论管理服务类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DownloadRecordService downloadRecordService;

    /**
     * 创建评论
     */
    @Transactional
    public CommentInfoResponse createComment(Long documentId, Long userId, CommentCreateRequest request) {
        log.info("创建评论，文档ID: {}, 用户ID: {}, 内容长度: {}",
                documentId, userId, request.getContent().length());

        // 验证评论内容
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        if (request.getContent().length() > 1000) {
            throw new IllegalArgumentException("评论内容不能超过1000个字符");
        }

        // 检查文档是否存在且已审核
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (!document.isApproved()) {
            throw new RuntimeException("只能对已审核的文档进行评论");
        }

        // 检查用户是否已下载过该文档
        boolean hasDownloaded = downloadRecordService.hasUserDownloadedDocument(userId, documentId);
        if (!hasDownloaded) {
            throw new RuntimeException("只有下载过文档的用户才能进行评论");
        }

        // 验证父评论（如果是回复）
        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));

            // 检查父评论是否属于同一文档
            if (!parentComment.getDocumentId().equals(documentId)) {
                throw new RuntimeException("父评论不属于当前文档");
            }

            // 限制回复层级（最多2级：评论->回复）
            if (parentComment.getParentId() != null) {
                throw new RuntimeException("不支持多层级回复");
            }
        }

        Comment comment = new Comment();
        comment.setDocumentId(documentId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        comment.setParentId(request.getParentId());
        comment.setIsDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        log.info("评论创建成功，ID: {}", savedComment.getId());

        return convertToCommentInfoResponse(savedComment);
    }

    /**
     * 获取文档的评论列表（树状结构）
     */
    public PageResponse<CommentInfoResponse> getDocumentComments(Long documentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 只获取顶级评论（没有父评论的）
        Page<Comment> commentPage = commentRepository.findByDocumentIdAndParentIdIsNullAndIsDeletedFalse(
                documentId, pageable);

        List<CommentInfoResponse> commentList = commentPage.getContent().stream()
                .map(comment -> {
                    CommentInfoResponse response = convertToCommentInfoResponse(comment);
                    // 加载回复
                    loadReplies(response);
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.fromPage(commentPage, commentList);
    }

    /**
     * 获取评论的回复列表
     */
    public List<CommentInfoResponse> getCommentReplies(Long parentId) {
        List<Comment> replies = commentRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId);
        return replies.stream()
                .map(this::convertToCommentInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的评论列表
     */
    public PageResponse<CommentInfoResponse> getUserComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findByUserIdAndIsDeletedFalse(userId, pageable);

        List<CommentInfoResponse> commentList = commentPage.getContent().stream()
                .map(this::convertToCommentInfoResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(commentPage, commentList);
    }

    /**
     * 更新评论内容
     */
    @Transactional
    public CommentInfoResponse updateComment(Long commentId, Long userId, String content) {
        log.info("更新评论，ID: {}, 用户ID: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 检查权限
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限修改此评论");
        }

        // 检查评论是否已删除
        if (comment.getIsDeleted()) {
            throw new RuntimeException("无法修改已删除的评论");
        }

        // 验证内容
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        if (content.length() > 1000) {
            throw new IllegalArgumentException("评论内容不能超过1000个字符");
        }

        comment.setContent(content.trim());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        log.info("评论更新成功，ID: {}", commentId);

        return convertToCommentInfoResponse(updatedComment);
    }

    /**
     * 删除评论（软删除）
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("删除评论，ID: {}, 用户ID: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 检查权限
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此评论");
        }

        // 软删除
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        log.info("评论删除成功，ID: {}", commentId);
    }

    /**
     * 管理员删除评论
     */
    @Transactional
    public void adminDeleteComment(Long commentId, String reason) {
        log.info("管理员删除评论，ID: {}, 原因: {}", commentId, reason);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 软删除
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        log.info("管理员评论删除成功，ID: {}", commentId);
    }

    /**
     * 批量删除用户的所有评论
     */
    @Transactional
    public void deleteUserComments(Long userId) {
        log.info("批量删除用户评论，用户ID: {}", userId);

        List<Comment> userComments = commentRepository.findByUserIdAndIsDeletedFalse(userId);
        for (Comment comment : userComments) {
            comment.setIsDeleted(true);
            comment.setUpdatedAt(LocalDateTime.now());
        }

        commentRepository.saveAll(userComments);
        log.info("用户评论批量删除完成，用户ID: {}, 删除数量: {}", userId, userComments.size());
    }

    /**
     * 获取评论详情
     */
    public CommentInfoResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (comment.getIsDeleted()) {
            throw new RuntimeException("评论已被删除");
        }

        return convertToCommentInfoResponse(comment);
    }

    /**
     * 获取文档评论统计
     */
    public Map<String, Object> getDocumentCommentStatistics(Long documentId) {
        // 总评论数（不包括已删除的）
        Long totalComments = commentRepository.countByDocumentIdAndIsDeletedFalse(documentId);

        // 顶级评论数
        Long topLevelComments = commentRepository.countByDocumentIdAndParentIdIsNullAndIsDeletedFalse(documentId);

        // 回复数
        Long replies = totalComments - topLevelComments;

        return Map.of(
            "totalComments", totalComments,
            "topLevelComments", topLevelComments,
            "replies", replies
        );
    }

    /**
     * 获取用户评论统计
     */
    public Map<String, Object> getUserCommentStatistics(Long userId) {
        Long totalComments = commentRepository.countByUserIdAndIsDeletedFalse(userId);
        Long topLevelComments = commentRepository.countByUserIdAndParentIdIsNullAndIsDeletedFalse(userId);
        Long replies = totalComments - topLevelComments;

        return Map.of(
            "totalComments", totalComments,
            "topLevelComments", topLevelComments,
            "replies", replies
        );
    }

    /**
     * 搜索评论
     */
    public PageResponse<CommentInfoResponse> searchComments(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.searchComments(keyword, pageable);

        List<CommentInfoResponse> commentList = commentPage.getContent().stream()
                .map(this::convertToCommentInfoResponse)
                .collect(Collectors.toList());

        return PageResponse.fromPage(commentPage, commentList);
    }

    /**
     * 获取最新评论
     */
    public List<CommentInfoResponse> getRecentComments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Comment> recentComments = commentRepository.findRecentComments(pageable);

        return recentComments.stream()
                .map(this::convertToCommentInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门评论（回复数多的评论）
     */
    public List<CommentInfoResponse> getPopularComments(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Comment> popularComments = commentRepository.findPopularComments(pageable);

        return popularComments.stream()
                .map(this::convertToCommentInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否已评论过文档
     */
    public boolean hasUserCommentedDocument(Long documentId, Long userId) {
        return commentRepository.existsByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId);
    }

    /**
     * 加载评论的回复
     */
    private void loadReplies(CommentInfoResponse comment) {
        List<CommentInfoResponse> replies = getCommentReplies(comment.getId());
        comment.setReplies(replies);
    }

    /**
     * 转换为评论信息响应DTO
     */
    private CommentInfoResponse convertToCommentInfoResponse(Comment comment) {
        CommentInfoResponse response = CommentInfoResponse.fromEntity(comment);

        // 加载用户信息
        User user = userRepository.findById(comment.getUserId()).orElse(null);
        if (user != null) {
            response.setUserNickname(user.getUsername());
            response.setUserAvatar(user.getAvatar());
        }

        // 如果是回复，加载父评论信息
        if (comment.getParentId() != null) {
            Comment parentComment = commentRepository.findById(comment.getParentId()).orElse(null);
            if (parentComment != null && !parentComment.getIsDeleted()) {
                User parentUser = userRepository.findById(parentComment.getUserId()).orElse(null);
                if (parentUser != null) {
                    response.setParentUserNickname(parentUser.getUsername());
                }
            }
        }

        return response;
    }
}