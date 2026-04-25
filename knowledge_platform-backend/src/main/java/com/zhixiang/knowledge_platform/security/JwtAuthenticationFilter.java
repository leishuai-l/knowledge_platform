package com.zhixiang.knowledge_platform.security;

import com.zhixiang.knowledge_platform.service.UserService;
import com.zhixiang.knowledge_platform.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 拦截请求并验证JWT令牌
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            // log.info("Processing request: {} {}, Token present: {}", request.getMethod(), request.getRequestURI(), jwt != null);

            if (StringUtils.hasText(jwt)) {
                if (jwtUtil.validateAccessToken(jwt)) {
                    String username = jwtUtil.getUsernameFromToken(jwt);
                    // log.info("Token valid for user: {}", username);

                    UserDetails userDetails = userService.loadUserByUsername(username);
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 记录当前用户信息到请求属性中，方便Controller使用
                        request.setAttribute("currentUserId", jwtUtil.getUserIdFromToken(jwt));
                        request.setAttribute("currentUsername", username);
                        request.setAttribute("currentUserRole", jwtUtil.getRoleFromToken(jwt));
                    }
                } else {
                    log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                }
            } else {
                // log.debug("No JWT token found for request: {}", request.getRequestURI());
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT令牌
     * 仅从Authorization Header获取，不支持URL参数，以防止Token泄露到日志
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 判断是否应该跳过此过滤器
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        
        // 跳过异步分发请求
        if (jakarta.servlet.DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return true;
        }

        // 跳过公开接口
        if (path.startsWith("/api/auth/") ||
            path.startsWith("/api/test/") ||
            path.startsWith("/swagger-ui/") ||
            path.startsWith("/api-docs/") ||
            path.equals("/swagger-ui.html") ||
            path.startsWith("/actuator/") ||
            path.startsWith("/static/") ||
            path.startsWith("/public/") ||
            path.equals("/favicon.ico") ||
            path.equals("/")) {
            return true;
        }

        // 标签接口：只有GET请求是公开的，POST/PUT/DELETE需要认证
        if (path.startsWith("/api/tags")) {
            return "GET".equals(method);
        }

        // 分类接口：只有GET请求是公开的，POST/PUT/DELETE需要认证
        if (path.startsWith("/api/categories")) {
            return "GET".equals(method);
        }

        // 文档下载是公开的
        if (path.startsWith("/api/documents/download")) {
            return true;
        }

        // 文档基本信息查看（游客可访问）
        if (path.equals("/api/documents") ||
            path.equals("/api/documents/search") ||
            path.equals("/api/documents/stats") ||
            path.equals("/api/documents/latest") ||
            path.equals("/api/documents/popular") ||
            path.equals("/api/documents/approved") ||
            path.equals("/api/documents/top-rated") ||
            path.startsWith("/api/documents/file-types/")) {
            return "GET".equals(method);
        }

        // 单个文档详情和预览信息端点：经过JWT过滤器以便正确解析已登录用户身份
        // 游客访问仍被允许，因为SecurityConfig中配置为permitAll()
        // 注意：不再跳过这些端点，以确保管理员能正确预览未审核文档
        if (path.matches("/api/documents/\\d+/previewable")) {
            return "GET".equals(method);
        }

        // 按分类获取文档（游客可访问）
        if (path.startsWith("/api/documents/category/")) {
            return "GET".equals(method);
        }

        // 积分排行榜是公开的
        if (path.startsWith("/api/points/leaderboard")) {
            return true;
        }

        // 论坛相关接口：只有GET请求是公开的，POST/PUT/DELETE需要认证
        if (path.startsWith("/api/forum/categories") || 
            path.startsWith("/api/forum/topics") || 
            path.startsWith("/api/forum/tags") || 
            path.startsWith("/api/forum/hot-topics")) {
            return "GET".equals(method);
        }

        return false;
    }

    /**
     * 从请求中获取当前用户ID
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 从请求中获取当前用户ID (允许为null)
     */
    public static Long getCurrentUserIdOrNull(HttpServletRequest request) {
        return getCurrentUserId(request);
    }

    /**
     * 从请求中获取当前用户名
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String) {
            return (String) username;
        }
        return null;
    }

    /**
     * 从请求中获取当前用户角色
     */
    public static String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        if (role instanceof String) {
            return (String) role;
        }
        return null;
    }
}