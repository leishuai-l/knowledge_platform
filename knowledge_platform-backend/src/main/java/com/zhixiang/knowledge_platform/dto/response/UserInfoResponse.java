package com.zhixiang.knowledge_platform.dto.response;

import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "用户信息响应")
public class UserInfoResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "头像URL", example = "/avatars/default.png")
    private String avatar;

    @Schema(description = "用户角色", example = "USER")
    private UserRole role;

    @Schema(description = "当前积分", example = "150")
    private Integer points;

    @Schema(description = "累计积分", example = "200")
    private Integer totalPoints;

    @Schema(description = "账户状态", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;

    /**
     * 从User实体转换为UserInfoResponse
     */
    public static UserInfoResponse fromEntity(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        // response.setNickname(user.getUsername()); // nickname字段已删除
        response.setAvatar(user.getAvatar());
        response.setRole(user.getRole());
        response.setPoints(user.getPoints());
        response.setTotalPoints(user.getTotalPoints());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}