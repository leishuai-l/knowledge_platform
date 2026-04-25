package com.zhixiang.knowledge_platform.config;

import com.zhixiang.knowledge_platform.security.JwtAuthenticationEntryPoint;
import com.zhixiang.knowledge_platform.security.JwtAuthenticationFilter;
import com.zhixiang.knowledge_platform.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy UserService userService,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         @Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userService = userService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * DAO认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(passwordEncoder());
        authProvider.setUserDetailsService(userService);
        return authProvider;
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 使用具体的域名列表，而不是通配符
        // 生产环境应该配置具体的前端域名
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
            // 生产环境添加: "https://yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin()) // 允许同源 iframe 嵌入
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 允许异步分发（用于SSE流式响应）
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                
                // 静态资源 - 放在最前面，优先级最高
                .requestMatchers("/", "/static/**", "/public/**", "/favicon.ico").permitAll()
                .requestMatchers("/avatars/**").permitAll()

                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/email/**").permitAll()
                .requestMatchers("/api/test/**").authenticated()

                // 标签管理（临时开放用于测试）
                .requestMatchers("/api/tags/unused").permitAll()

                // Swagger文档
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Actuator健康检查
                .requestMatchers("/actuator/**").permitAll()

                // 文档受控访问接口
                .requestMatchers("/api/documents/download/**").authenticated()
                .requestMatchers("/api/documents/*/download").authenticated()
                .requestMatchers("/api/documents/*/content").authenticated()
                .requestMatchers(
                    "/api/documents/*/thumbnail",
                    "/api/documents/*/preview-content",
                    "/api/documents/*/preview-archive"
                ).authenticated()

                // 文档基本信息查看（游客可访问）
                .requestMatchers("/api/documents", "/api/documents/*").permitAll()
                .requestMatchers("/api/documents/*/preview-info").permitAll()
                .requestMatchers("/api/documents/*/previewable").permitAll()
                .requestMatchers("/api/documents/stats", "/api/documents/latest", "/api/documents/popular").permitAll()
                .requestMatchers("/api/documents/file-types/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()

                // 标签查看（游客可访问）
                .requestMatchers("/api/tags", "/api/tags/page", "/api/tags/popular", "/api/tags/search", "/api/tags/*/").permitAll()
                .requestMatchers("/api/tags/statistics", "/api/tags/usage-range", "/api/tags/recommended").permitAll()
                .requestMatchers("/api/tags/validate", "/api/tags/normalize").permitAll()

                // 积分排行榜等公开数据
                .requestMatchers("/api/points/leaderboard", "/api/points/recent").permitAll()

                // 评论和评分查看（游客可访问）
                .requestMatchers("/api/comments/documents/*", "/api/comments/recent", "/api/comments/popular").permitAll()
                .requestMatchers("/api/ratings/documents/*", "/api/ratings/recent", "/api/ratings/top-documents").permitAll()

                // WebSocket端点（允许访问，包括SockJS所有端点）
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws/info").permitAll()
                .requestMatchers("/ws/websocket").permitAll()
                .requestMatchers("/ws/iframe*.html").permitAll()

                // 社区论坛（游客可访问读接口）
                .requestMatchers(
                    "/api/forum/categories",
                    "/api/forum/topics",
                    "/api/forum/topics/**", // Allow detail and replies with path variables
                    "/api/forum/tags",
                    "/api/forum/hot-topics"
                ).permitAll()

                // AI 智能服务（需要认证）
                .requestMatchers("/api/ai/**").authenticated()

                // 用户相关接口（需要认证）
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/points/**").authenticated()

                // 文档上传和管理（需要认证）
                .requestMatchers("/api/documents/upload").authenticated()
                .requestMatchers("/api/documents/*/edit").authenticated()
                .requestMatchers("/api/documents/*/delete").authenticated()

                // 评论和评分创建/管理（需要认证）
                .requestMatchers("/api/comments/**", "/api/ratings/**").authenticated()

                // 文件上传和预览需要认证
                .requestMatchers("/api/files/**").authenticated()

                // 申诉接口（需要认证）
                .requestMatchers("/api/appeals").authenticated()
                .requestMatchers("/api/appeals/my").authenticated()

                // 知识产权保护接口（需要认证）
                .requestMatchers("/api/copyright/reports").authenticated()
                .requestMatchers("/api/copyright/reports/my").authenticated()

                // 管理员接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 其他所有请求需要认证
                .anyRequest().authenticated()
            );

        // 添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}