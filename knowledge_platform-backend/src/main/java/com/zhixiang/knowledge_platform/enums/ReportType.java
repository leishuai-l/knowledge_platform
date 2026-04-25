package com.zhixiang.knowledge_platform.enums;

/**
 * 举报类型枚举
 */
public enum ReportType {
    COPYRIGHT_INFRINGEMENT("版权侵权"),
    PLAGIARISM("抄袭剽窃"),
    ILLEGAL_CONTENT("违法内容"),
    INAPPROPRIATE_CONTENT("不当内容"),
    SENSITIVE_CONTENT("敏感内容"),
    FALSE_INFORMATION("虚假信息"),
    SPAM("垃圾广告"),
    OTHER("其他");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
