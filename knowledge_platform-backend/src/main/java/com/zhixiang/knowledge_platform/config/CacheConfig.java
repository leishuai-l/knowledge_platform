package com.zhixiang.knowledge_platform.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 * 使用内存缓存提升性能
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     * 使用ConcurrentMapCacheManager提供内存级缓存
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // 预定义缓存名称
        cacheManager.setCacheNames(
            java.util.Arrays.asList(
                "documents",           // 文档缓存
                "categories",          // 分类缓存
                "tags",               // 标签缓存
                "users",              // 用户缓存
                "statistics",         // 统计数据缓存
                "notifications",      // 通知缓存
                "searchResults"       // 搜索结果缓存
            )
        );

        // 允许运行时创建缓存
        cacheManager.setAllowNullValues(false);

        return cacheManager;
    }
}