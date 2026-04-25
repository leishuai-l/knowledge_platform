-- ========================================
-- 知享校园知识库共享平台 - 数据库表结构
-- 版本: 2.0.0
-- 说明: 包含所有核心业务表的建表语句
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一部分：核心业务表
-- ========================================

-- 1. 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `avatar` VARCHAR(255) DEFAULT '/avatars/default.png' COMMENT '头像路径',
    `bio` VARCHAR(500) COMMENT '个人简介',
    `role` ENUM('USER', 'ADMIN') DEFAULT 'USER' COMMENT '用户角色',
    `points` INT DEFAULT 100 COMMENT '当前积分',
    `total_points` INT DEFAULT 100 COMMENT '累计获得积分',
    `status` ENUM('ACTIVE', 'DISABLED', 'LOCKED', 'DELETED') DEFAULT 'ACTIVE' COMMENT '账户状态',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `failed_login_attempts` INT DEFAULT 0 COMMENT '失败登录次数',
    `locked_until` DATETIME COMMENT '锁定到期时间',
    `version` BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`),
    INDEX `idx_points` (`points` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 文档分类表（树形结构）
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
    `level` INT DEFAULT 1 COMMENT '分类层级（1为一级分类）',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `description` TEXT COMMENT '分类描述',
    `is_active` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`parent_id`) REFERENCES `categories`(`id`) ON DELETE CASCADE,
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_level` (`level`),
    INDEX `idx_active` (`is_active`),
    INDEX `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分类表';

-- 3. 文档标签表
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `color` VARCHAR(7) DEFAULT '#409EFF' COMMENT '标签颜色',
    `description` VARCHAR(200) COMMENT '标签描述',
    `usage_count` INT DEFAULT 0 COMMENT '使用次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_name` (`name`),
    INDEX `idx_usage_count` (`usage_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签表';

-- 4. 文档表
DROP TABLE IF EXISTS `documents`;
CREATE TABLE `documents` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
    `description` TEXT COMMENT '文档描述',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    `file_extension` VARCHAR(10) NOT NULL COMMENT '文件扩展名',
    `md5` VARCHAR(32) COMMENT '文件MD5值',
    `ai_summary` TEXT COMMENT 'AI文档摘要',
    `ai_analysis_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'AI分析状态',
    `version` BIGINT DEFAULT 0 COMMENT '乐观锁版本号',

    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',

    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'DELETED') DEFAULT 'PENDING' COMMENT '审核状态',
    `rejection_reason` TEXT COMMENT '拒绝原因',

    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `rating_average` DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分',
    `rating_count` INT DEFAULT 0 COMMENT '评分人数',

    `download_points` INT DEFAULT 0 COMMENT '下载所需积分',

    `approved_at` DATETIME COMMENT '审核通过时间',
    `approved_by` BIGINT COMMENT '审核人ID',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`),
    FOREIGN KEY (`uploader_id`) REFERENCES `users`(`id`),
    FOREIGN KEY (`approved_by`) REFERENCES `users`(`id`),

    INDEX `idx_category` (`category_id`),
    INDEX `idx_uploader` (`uploader_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at` DESC),
    INDEX `idx_download_count` (`download_count` DESC),
    INDEX `idx_rating` (`rating_average` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 5. 文档-标签关联表
DROP TABLE IF EXISTS `document_tags`;
CREATE TABLE `document_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `tags`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_document_tag` (`document_id`, `tag_id`),
    INDEX `idx_document` (`document_id`),
    INDEX `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档标签关联表';

-- ========================================
-- 第二部分：文档互动表
-- ========================================

-- 6. 评分表
DROP TABLE IF EXISTS `ratings`;
CREATE TABLE `ratings` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT NOT NULL COMMENT '评分（1-5星）',
    `comment` VARCHAR(500) COMMENT '评分评论',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_document_user` (`document_id`, `user_id`),
    INDEX `idx_document` (`document_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_rating` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- 7. 评论表
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（回复）',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_id`) REFERENCES `comments`(`id`) ON DELETE CASCADE,

    INDEX `idx_document` (`document_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 8. 下载记录表
DROP TABLE IF EXISTS `download_records`;
CREATE TABLE `download_records` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '下载记录ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `points_cost` INT NOT NULL COMMENT '消费积分',
    `ip_address` VARCHAR(45) COMMENT 'IP地址',
    `user_agent` TEXT COMMENT '用户代理',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `download_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    INDEX `idx_document` (`document_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_created_at` (`created_at` DESC),
    INDEX `idx_user_document` (`user_id`, `document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载记录表';

-- 9. 积分记录表
DROP TABLE IF EXISTS `points_records`;
CREATE TABLE `points_records` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` ENUM('EARN', 'SPEND') NOT NULL COMMENT '积分类型',
    `points` INT NOT NULL COMMENT '积分数量',
    `source` ENUM('REGISTER', 'UPLOAD', 'APPROVED', 'DOWNLOAD_REWARD', 'RATING_REWARD', 'ADMIN_ADJUST', 'DOWNLOAD_COST') NOT NULL COMMENT '积分来源',
    `reference_id` BIGINT COMMENT '关联ID',
    `description` VARCHAR(255) COMMENT '描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    INDEX `idx_user` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_source` (`source`),
    INDEX `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- 10. 通知表
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` ENUM('DOCUMENT_APPROVED', 'DOCUMENT_REJECTED', 'DOCUMENT_COMMENTED', 'DOCUMENT_RATED', 'POINTS_EARNED', 'POINTS_SPENT', 'SYSTEM_ANNOUNCEMENT') NOT NULL COMMENT '通知类型',
    `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `reference_id` BIGINT COMMENT '关联ID',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `read_at` DATETIME COMMENT '阅读时间',
    `is_deleted` BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    `deleted_at` DATETIME COMMENT '删除时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    INDEX `idx_user` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_read` (`is_read`),
    INDEX `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ========================================
-- 第三部分：用户系统表
-- ========================================

-- 11. 邮箱验证码表
DROP TABLE IF EXISTS `email_verifications`;
CREATE TABLE `email_verifications` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '验证ID',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` ENUM('REGISTRATION', 'PASSWORD_RESET', 'EMAIL_CHANGE') NOT NULL COMMENT '验证类型',
    `user_id` BIGINT COMMENT '关联用户ID',
    `verified` BOOLEAN DEFAULT FALSE COMMENT '是否已验证',
    `verified_at` DATETIME COMMENT '验证时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `attempts` INT DEFAULT 0 COMMENT '尝试次数',
    `max_attempts` INT DEFAULT 5 COMMENT '最大尝试次数',

    INDEX `idx_email` (`email`),
    INDEX `idx_code` (`code`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

-- 12. 用户关注表
DROP TABLE IF EXISTS `user_follows`;
CREATE TABLE `user_follows` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
    `followed_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',

    FOREIGN KEY (`follower_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`followed_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_follower_followed` (`follower_id`, `followed_id`),
    INDEX `idx_follower` (`follower_id`),
    INDEX `idx_followed` (`followed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- ========================================
-- 第四部分：论坛模块表
-- ========================================

-- 13. 论坛分类表
DROP TABLE IF EXISTS `forum_categories`;
CREATE TABLE `forum_categories` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '论坛分类ID',
    `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
    `description` TEXT COMMENT '分类描述',
    `icon` VARCHAR(50) COMMENT '分类图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `status` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_sort_order` (`sort_order`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛分类表';

-- 14. 论坛标签表
DROP TABLE IF EXISTS `forum_tags`;
CREATE TABLE `forum_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '论坛标签ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `topic_count` INT DEFAULT 0 COMMENT '使用该标签的话题数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX `idx_topic_count` (`topic_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛标签表';

-- 15. 论坛帖子表
DROP TABLE IF EXISTS `forum_topics`;
CREATE TABLE `forum_topics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` LONGTEXT NOT NULL COMMENT '帖子内容',
    `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
    `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `reply_count` INT DEFAULT 0 COMMENT '回复次数',
    `like_count` INT DEFAULT 0 COMMENT '点赞次数',
    `collection_count` INT DEFAULT 0 COMMENT '收藏次数',
    `is_pinned` BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    `is_essence` BOOLEAN DEFAULT FALSE COMMENT '是否精华',
    `status` INT DEFAULT 0 COMMENT '状态：0正常 1锁定 2隐藏 3草稿',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `forum_categories`(`id`) ON DELETE CASCADE,

    INDEX `idx_user` (`user_id`),
    INDEX `idx_category` (`category_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at` DESC),
    INDEX `idx_view_count` (`view_count` DESC),
    INDEX `idx_reply_count` (`reply_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

-- 16. 论坛帖子-标签关联表
DROP TABLE IF EXISTS `forum_topic_tags`;
CREATE TABLE `forum_topic_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    `topic_id` BIGINT NOT NULL COMMENT '帖子ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (`topic_id`) REFERENCES `forum_topics`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `forum_tags`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_topic_tag` (`topic_id`, `tag_id`),
    INDEX `idx_topic` (`topic_id`),
    INDEX `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子标签关联表';

-- 17. 论坛回复表
DROP TABLE IF EXISTS `forum_replies`;
CREATE TABLE `forum_replies` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回复ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `topic_id` BIGINT NOT NULL COMMENT '所属帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '回复用户ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父回复ID',
    `like_count` INT DEFAULT 0 COMMENT '点赞次数',
    `status` INT DEFAULT 0 COMMENT '状态：0正常 1隐藏',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (`topic_id`) REFERENCES `forum_topics`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_id`) REFERENCES `forum_replies`(`id`) ON DELETE CASCADE,

    INDEX `idx_topic` (`topic_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛回复表';

-- 18. 论坛帖子点赞表
DROP TABLE IF EXISTS `forum_topic_likes`;
CREATE TABLE `forum_topic_likes` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    `topic_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    FOREIGN KEY (`topic_id`) REFERENCES `forum_topics`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_topic_user` (`topic_id`, `user_id`),
    INDEX `idx_topic` (`topic_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子点赞表';

-- 19. 论坛帖子收藏表
DROP TABLE IF EXISTS `forum_topic_collections`;
CREATE TABLE `forum_topic_collections` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    `topic_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    FOREIGN KEY (`topic_id`) REFERENCES `forum_topics`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_topic_user` (`topic_id`, `user_id`),
    INDEX `idx_topic` (`topic_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子收藏表';

-- 20. 论坛回复点赞表
DROP TABLE IF EXISTS `forum_reply_likes`;
CREATE TABLE `forum_reply_likes` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    `reply_id` BIGINT NOT NULL COMMENT '回复ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    FOREIGN KEY (`reply_id`) REFERENCES `forum_replies`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,

    UNIQUE KEY `uk_reply_user` (`reply_id`, `user_id`),
    INDEX `idx_reply` (`reply_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛回复点赞表';

-- ========================================
-- 第五部分：AI聊天模块表
-- ========================================

-- 21. AI对话表
DROP TABLE IF EXISTS `chat_conversation`;
CREATE TABLE `chat_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) COMMENT '对话标题',
    `messages` LONGTEXT NOT NULL COMMENT '对话内容JSON数组',
    `message_count` INT DEFAULT 0 COMMENT '消息轮数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_user` (`user_id`),
    INDEX `idx_updated_at` (`updated_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话表';

-- ========================================
-- 第廿一部分：AI聊天记录迁移（从旧表到新表）
-- ========================================
-- 将旧表 chat_session 和 chat_history 的数据合并到 chat_conversation
-- 迁移后删除旧表

-- 22. AI聊天会话迁移脚本（临时，用于数据迁移）
-- 注意：在生产环境执行前，请确保已备份数据
-- 此脚本将 chat_session 和 chat_history 合并为 chat_conversation

-- 如果需要保留历史数据，可以创建新表后插入数据：
-- INSERT INTO chat_conversation (user_id, title, messages, message_count, created_at, updated_at)
-- SELECT
--     cs.user_id,
--     cs.title,
--     CONCAT('[', GROUP_CONCAT(
--         CONCAT('{"role":"', ch.role, '","content":"', REPLACE(ch.content, '"', '\\"'), '"}')
--         ORDER BY ch.message_order SEPARATOR ','
--     ), ']') as messages,
--     cs.message_count,
--     cs.created_at,
--     cs.updated_at
-- FROM chat_session cs
-- JOIN chat_history ch ON cs.id = ch.session_id
-- GROUP BY cs.id;

-- DROP TABLE IF EXISTS chat_history;
-- DROP TABLE IF EXISTS chat_session;

-- ========================================
-- 第六部分：审核治理模块表
-- ========================================

-- 23. 文档审核记录表
DROP TABLE IF EXISTS `document_reviews`;
CREATE TABLE `document_reviews` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审核记录ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `review_type` ENUM('INITIAL', 'FINAL') NOT NULL COMMENT '审核类型',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL COMMENT '审核状态',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `review_comment` TEXT COMMENT '审核意见',

    `format_check_passed` BOOLEAN COMMENT '格式检查是否通过',
    `content_compliance_passed` BOOLEAN COMMENT '内容合规是否通过',
    `similarity_score` DOUBLE COMMENT '相似度分数',
    `similar_document_id` BIGINT COMMENT '相似文档ID',

    `academic_score` INT COMMENT '学术性评分',
    `originality_score` INT COMMENT '原创性评分',
    `practicality_score` INT COMMENT '实用性评分',
    `copyright_compliance` BOOLEAN COMMENT '版权合规性',

    `rejection_reason` TEXT COMMENT '拒绝原因',
    `suggestions` TEXT COMMENT '修改建议',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `reviewed_at` DATETIME COMMENT '审核时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewer_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,

    INDEX `idx_document` (`document_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档审核记录表';

-- 24. 文档申诉表
DROP TABLE IF EXISTS `document_appeals`;
CREATE TABLE `document_appeals` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申诉记录ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `review_id` BIGINT NOT NULL COMMENT '关联审核记录ID',
    `user_id` BIGINT NOT NULL COMMENT '申诉用户ID',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL COMMENT '申诉状态',
    `appeal_reason` TEXT NOT NULL COMMENT '申诉理由',
    `evidence` TEXT COMMENT '证据材料',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handler_comment` TEXT COMMENT '处理意见',
    `final_decision` TEXT COMMENT '最终处理决定',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `handled_at` DATETIME COMMENT '处理时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`review_id`) REFERENCES `document_reviews`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`handler_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,

    INDEX `idx_document` (`document_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档申诉表';

-- 25. 版权举报表
DROP TABLE IF EXISTS `copyright_reports`;
CREATE TABLE `copyright_reports` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报记录ID',
    `document_id` BIGINT NOT NULL COMMENT '被举报文档ID',
    `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
    `report_type` ENUM('PLAGIARISM', 'COPYRIGHT_INFRINGEMENT', 'ILLEGAL_CONTENT', 'OTHER') NOT NULL COMMENT '举报类型',
    `status` ENUM('PENDING', 'INVESTIGATING', 'CONFIRMED', 'REJECTED') NOT NULL COMMENT '处理状态',
    `description` TEXT NOT NULL COMMENT '举报描述',
    `evidence_urls` TEXT COMMENT '证据链接',
    `contact_info` VARCHAR(100) COMMENT '联系方式',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handler_comment` TEXT COMMENT '处理意见',
    `action_taken` TEXT COMMENT '采取的措施',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `handled_at` DATETIME COMMENT '处理时间',

    FOREIGN KEY (`document_id`) REFERENCES `documents`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reporter_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`handler_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,

    INDEX `idx_document` (`document_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版权举报表';

-- ========================================
-- 恢复外键检查
-- ========================================
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 表结构创建完成
-- ========================================
SELECT 'Schema creation completed successfully!' AS message;
