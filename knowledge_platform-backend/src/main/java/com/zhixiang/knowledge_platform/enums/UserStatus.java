package com.zhixiang.knowledge_platform.enums;

/**
 * 用户状态枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum UserStatus {
    ACTIVE("正常"),
    DISABLED("禁用"),
    LOCKED("锁定"),
    DELETED("已删除");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}