package com.zhixiang.knowledge_platform.enums;

/**
 * 通知类型枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum NotificationType {
    DOCUMENT_APPROVED("文档审核通过"),
    DOCUMENT_REJECTED("文档审核被拒"),
    DOCUMENT_COMMENTED("文档收到新评论"),
    DOCUMENT_RATED("文档收到新评分"),
    POINTS_EARNED("积分获得"),
    POINTS_SPENT("积分消费"),
    SYSTEM_ANNOUNCEMENT("系统公告");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}