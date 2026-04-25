package com.zhixiang.knowledge_platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求DTO
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Data
public class UserUpdateRequest {

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;

    @Size(max = 100, message = "所在学校长度不能超过100个字符")
    private String school;

    @Size(max = 100, message = "专业长度不能超过100个字符")
    private String major;
}