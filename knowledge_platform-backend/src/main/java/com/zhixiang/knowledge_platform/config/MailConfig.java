package com.zhixiang.knowledge_platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 邮件配置类
 * 提供邮件服务的条件性配置
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@Slf4j
public class MailConfig {

    /**
     * 当邮件功能未启用时，提供一个空的JavaMailSender实现
     */
    @Bean
    @ConditionalOnProperty(name = "zhixiang.mail.enabled", havingValue = "false", matchIfMissing = true)
    public JavaMailSender disabledMailSender() {
        log.info("邮件功能已禁用，使用空实现");
        return new JavaMailSenderImpl() {
            @Override
            public void send(org.springframework.mail.SimpleMailMessage simpleMessage) {
                log.debug("邮件功能已禁用，跳过发送简单邮件: {}", simpleMessage.getSubject());
            }

            @Override
            public void send(org.springframework.mail.SimpleMailMessage... simpleMessages) {
                log.debug("邮件功能已禁用，跳过发送 {} 封简单邮件", simpleMessages.length);
            }

            @Override
            public void send(jakarta.mail.internet.MimeMessage mimeMessage) {
                try {
                    log.debug("邮件功能已禁用，跳过发送MIME邮件: {}", mimeMessage.getSubject());
                } catch (Exception e) {
                    log.debug("邮件功能已禁用，跳过发送MIME邮件");
                }
            }

            @Override
            public void send(jakarta.mail.internet.MimeMessage... mimeMessages) {
                log.debug("邮件功能已禁用，跳过发送 {} 封MIME邮件", mimeMessages.length);
            }

            @Override
            public jakarta.mail.internet.MimeMessage createMimeMessage() {
                return super.createMimeMessage();
            }
        };
    }
}