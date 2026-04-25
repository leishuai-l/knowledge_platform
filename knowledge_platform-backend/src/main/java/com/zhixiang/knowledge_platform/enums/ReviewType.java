package com.zhixiang.knowledge_platform.enums;

/**
 * 审核类型枚举
 */
public enum ReviewType {
    INITIAL("初审"),
    FINAL("复审");

    private final String description;

    ReviewType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
