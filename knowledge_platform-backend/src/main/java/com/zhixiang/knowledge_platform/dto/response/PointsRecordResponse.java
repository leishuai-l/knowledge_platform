package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.PointsRecord;
import com.zhixiang.knowledge_platform.enums.PointsSource;
import com.zhixiang.knowledge_platform.enums.PointsType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分记录响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "积分记录响应")
public class PointsRecordResponse {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "积分类型", example = "EARN")
    private PointsType type;

    @Schema(description = "积分数量", example = "10")
    private Integer points;

    @Schema(description = "积分来源", example = "UPLOAD")
    private PointsSource source;

    @Schema(description = "来源描述", example = "上传资料")
    private String sourceDescription;

    @Schema(description = "关联ID", example = "15")
    private Long referenceId;

    @Schema(description = "描述", example = "上传文档《Java编程基础教程》")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 从PointsRecord实体转换为PointsRecordResponse
     */
    public static PointsRecordResponse fromEntity(PointsRecord record) {
        PointsRecordResponse response = new PointsRecordResponse();
        response.setId(record.getId());
        response.setType(record.getType());
        response.setPoints(record.getPoints());
        response.setSource(record.getSource());
        response.setSourceDescription(record.getSource().getDescription());
        response.setReferenceId(record.getReferenceId());
        response.setDescription(record.getDescription());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}