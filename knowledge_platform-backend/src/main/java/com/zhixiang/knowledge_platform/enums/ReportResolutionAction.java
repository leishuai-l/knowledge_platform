package com.zhixiang.knowledge_platform.enums;

/**
 * 举报确认后的文档处置动作
 */
public enum ReportResolutionAction {
    DELETE_DOCUMENT("删除文档"),
    TAKE_DOWN_DOCUMENT("下架文档"),
    NO_DOCUMENT_CHANGE("仅记录举报，不修改文档");

    private final String description;

    ReportResolutionAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
