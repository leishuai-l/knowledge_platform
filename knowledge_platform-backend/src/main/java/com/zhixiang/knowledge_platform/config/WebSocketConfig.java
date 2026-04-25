package com.zhixiang.knowledge_platform.config;

import com.zhixiang.knowledge_platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket配置类
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单代理，消息前缀为 /topic 的消息会被代理到所有连接的客户端
        config.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        // 用户专用消息前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，允许跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 获取Authorization头
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            // 验证token
                            if (jwtUtil.validateAccessToken(token)) {
                                String username = jwtUtil.getUsernameFromToken(token);
                                Long userId = jwtUtil.getUserIdFromToken(token);
                                String role = jwtUtil.getRoleFromToken(token);

                                if (username != null && role != null) {
                                    // 创建认证对象
                                    List<SimpleGrantedAuthority> authorities = List.of(
                                        new SimpleGrantedAuthority("ROLE_" + role)
                                    );
    
                                    Authentication auth = new UsernamePasswordAuthenticationToken(
                                        username, null, authorities
                                    );
    
                                    // 设置用户信息到accessor中，供后续使用
                                    accessor.setUser(auth);
                                    if (userId != null) {
                                        accessor.setHeader("userId", userId.toString());
                                    }
    
                                    log.info("WebSocket用户认证成功: username={}, userId={}, role={}",
                                        username, userId, role);
                                }
                            } else {
                                log.warn("WebSocket认证失败：无效的token");
                            }
                        } catch (Exception e) {
                            log.error("WebSocket认证错误：{}", e.getMessage());
                        }
                    } else {
                        log.info("WebSocket连接无认证信息，作为匿名用户处理");
                    }
                }

                return message;
            }
        });
    }
}