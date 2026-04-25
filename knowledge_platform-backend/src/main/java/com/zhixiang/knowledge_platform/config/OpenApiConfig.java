package com.zhixiang.knowledge_platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档配置
 * 配置Swagger UI和API文档的详细信息
 *
 * @author leishuai
 * @version 0.0.1-SNAPSHOT
 * @since 2025-09-10
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers())
                .components(getComponents())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    /**
     * API基础信息
     */
    private Info getApiInfo() {
        return new Info()
                .title("API 接口文档")
                .description(getApiDescription())
                .version("v1.0");
    }

    /**
     * API详细描述
     */
    private String getApiDescription() {
        return "API 接口文档 - 知享平台后端服务接口";
    }

    /**
     * 联系信息
     */
    private Contact getContactInfo() {
        return new Contact()
                .name("ZhiXiang Development Team")
                .email("dev@zhixiang.com")
                .url("https://github.com/zhixiang-team/knowledge-platform");
    }

    /**
     * 许可证信息
     */
    private License getLicenseInfo() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * 服务器配置
     */
    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort + contextPath)
                        .description("本地开发")
        );
    }

    /**
     * 安全组件配置
     */
    private Components getComponents() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                );
    }
}