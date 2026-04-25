package com.zhixiang.knowledge_platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置
 * 配置静态资源映射，支持Swagger UI自定义样式
 * 
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${zhixiang.file.upload-path}")
    private String uploadBasePath;

    /**
     * 配置消息转换器，解决 Long 类型精度丢失问题
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        // 序列化Long类型为String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        
        // 解决懒加载序列化问题
        // objectMapper.registerModule(new com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule());
        
        // 注册 Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        objectMapper.registerModule(simpleModule);
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(0, jackson2HttpMessageConverter);
    }

    /**
     * 配置静态资源处理器
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置头像文件访问路径
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + uploadBasePath + "/avatars/")
                .setCachePeriod(3600); // 设置缓存时间1小时

        // 配置Swagger UI自定义CSS资源映射
        registry.addResourceHandler("/swagger-ui-custom.css")
                .addResourceLocations("classpath:/static/");

        // 配置其他静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 确保Swagger UI资源正确映射
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
    }
}