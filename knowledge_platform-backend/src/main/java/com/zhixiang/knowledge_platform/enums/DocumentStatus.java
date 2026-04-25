package com.zhixiang.knowledge_platform.enums;

/**
 * 文档状态枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum DocumentStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    DELETED("已删除");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}