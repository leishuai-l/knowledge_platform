package com.zhixiang.knowledge_platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库迁移工具
 * 用于修复旧数据的兼容性问题
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting database migration checks...");

        // 修复 Users 表中 version 字段为 null 的情况
        try {
            int updatedCount = jdbcTemplate.update("UPDATE users SET version = 0 WHERE version IS NULL");
            if (updatedCount > 0) {
                log.info("Fixed {} user records with null version", updatedCount);
            }
        } catch (Exception e) {
            log.warn("Failed to update users version: {}", e.getMessage());
        }

        // 修复 Documents 表中 version 字段为 null 的情况
        try {
            int updatedCount = jdbcTemplate.update("UPDATE documents SET version = 0 WHERE version IS NULL");
            if (updatedCount > 0) {
                log.info("Fixed {} document records with null version", updatedCount);
            }
        } catch (Exception e) {
            log.warn("Failed to update documents version: {}", e.getMessage());
        }

        log.info("Database migration checks completed.");
    }
}
