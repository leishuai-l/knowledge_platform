package com.zhixiang.knowledge_platform.enums;

/**
 * 申诉状态枚举
 */
public enum AppealStatus {
    PENDING("待处理"),
    APPROVED("申诉通过"),
    REJECTED("申诉驳回");

    private final String description;

    AppealStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
