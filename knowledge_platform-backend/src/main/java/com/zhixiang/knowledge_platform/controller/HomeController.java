package com.zhixiang.knowledge_platform.controller;

import com.zhixiang.knowledge_platform.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根路径控制器
 */
@RestController
@Tag(name = "系统信息", description = "提供接口服务的基本信息")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "服务状态", description = "返回 API 服务的基本信息和入口地址")
    public ResponseEntity<ApiResponse<Map<String, Object>>> index() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "Knowledge Platform API");
        data.put("version", "v1");
        data.put("swaggerUi", "/swagger-ui.html");
        data.put("apiDocs", "/v3/api-docs");
        data.put("status", "running");

        return ResponseEntity.ok(ApiResponse.success(data, "接口服务运行正常"));
    }
}
