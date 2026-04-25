package com.zhixiang.knowledge_platform.enums;

/**
 * 积分来源枚举
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public enum PointsSource {
    REGISTER("用户注册"),
    UPLOAD("上传资料"),
    APPROVED("资料审核通过"),
    DOWNLOAD_REWARD("资料被下载"),
    RATING_REWARD("获得好评"),
    ADMIN_ADJUST("管理员调整"),
    DOWNLOAD_COST("下载消费");

    private final String description;

    PointsSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}