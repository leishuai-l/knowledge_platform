package com.zhixiang.knowledge_platform.exception;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出此异常
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResourceNotFoundException user(Long userId) {
        return new ResourceNotFoundException("用户不存在，ID: " + userId);
    }

    public static ResourceNotFoundException document(Long documentId) {
        return new ResourceNotFoundException("文档不存在，ID: " + documentId);
    }

    public static ResourceNotFoundException category(Long categoryId) {
        return new ResourceNotFoundException("分类不存在，ID: " + categoryId);
    }

    public static ResourceNotFoundException tag(Long tagId) {
        return new ResourceNotFoundException("标签不存在，ID: " + tagId);
    }

    public static ResourceNotFoundException comment(Long commentId) {
        return new ResourceNotFoundException("评论不存在，ID: " + commentId);
    }

    public static ResourceNotFoundException rating(Long ratingId) {
        return new ResourceNotFoundException("评分不存在，ID: " + ratingId);
    }
}