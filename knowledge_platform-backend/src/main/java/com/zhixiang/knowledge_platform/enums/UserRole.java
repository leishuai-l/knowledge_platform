package com.zhixiang.knowledge_platform.enums;

/**
 * 用户角色枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum UserRole {
    USER("普通用户"),
    ADMIN("管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}