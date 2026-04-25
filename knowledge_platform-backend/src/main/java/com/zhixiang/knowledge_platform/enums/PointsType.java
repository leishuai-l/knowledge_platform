package com.zhixiang.knowledge_platform.enums;

/**
 * 积分类型枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum PointsType {
    // 获得积分
    EARN("获得积分"),

    // 消费积分
    SPEND("消费积分");

    private final String description;

    PointsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}