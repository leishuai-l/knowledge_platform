package com.zhixiang.knowledge_platform.enums;

/**
 * 举报状态枚举
 */
public enum ReportStatus {
    PENDING("待处理"),
    INVESTIGATING("调查中"),
    CONFIRMED("举报属实"),
    REJECTED("举报不实"),
    CLOSED("已关闭");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
