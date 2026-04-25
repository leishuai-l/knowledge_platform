package com.zhixiang.knowledge_platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
@Schema(description = "用户登录请求")
public class UserLoginRequest {

    @NotBlank(message = "用户名或邮箱不能为空")
    @Schema(description = "用户名或邮箱", example = "zhangsan")
    private String usernameOrEmail;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "Password123")
    private String password;

    @Schema(description = "记住登录状态", example = "true")
    private Boolean rememberMe = false;
}