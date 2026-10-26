-- VideoHub 数据库迁移脚本
-- 用途：把旧版数据库升级为当前结构：新增用户表、视频归属与封面字段、点赞记录表。
-- 使用方式：Get-Content -Raw migration.sql | mysql -u root -p --default-character-set=utf8mb4
-- 脚本可重复执行，已存在的表、字段、索引、外键会自动跳过。

USE video_db;

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS `app_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username`        VARCHAR(32)  NOT NULL COMMENT '登录名',
    `password_hash`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    `nickname`        VARCHAR(50)  NOT NULL COMMENT '显示昵称',
    `role`            VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用，1=正常',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_username` (`username`),
    KEY `idx_app_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 默认管理员：admin / admin123（仅保存 BCrypt 摘要，部署前请修改密码）
INSERT IGNORE INTO `app_user`
    (`id`, `username`, `password_hash`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
    (1, 'admin', '$2b$12$V4rbLdZQUtLLMHavf50ER.lC1wpUAF11CUVDgAejvkNh5aVd0k/P6', '管理员', 'ADMIN', 1, NOW(), NOW());

-- 2. video 表补充封面文件名字段、视频归属字段、索引和外键
DROP PROCEDURE IF EXISTS `migrate_video`;
DELIMITER //
CREATE PROCEDURE `migrate_video`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'video' AND column_name = 'cover_file_name'
    ) THEN
        ALTER TABLE `video`
            ADD COLUMN `cover_file_name` VARCHAR(255) DEFAULT NULL COMMENT '封面磁盘文件名' AFTER `url`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'video' AND column_name = 'owner_id'
    ) THEN
        ALTER TABLE `video`
            ADD COLUMN `owner_id` BIGINT DEFAULT NULL COMMENT '上传用户 ID' AFTER `like_count`;
    END IF;

    UPDATE `video` SET `owner_id` = 1 WHERE `owner_id` IS NULL;
    ALTER TABLE `video`
        MODIFY COLUMN `owner_id` BIGINT NOT NULL DEFAULT 1 COMMENT '上传用户 ID';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'video' AND index_name = 'idx_owner_id'
    ) THEN
        ALTER TABLE `video` ADD INDEX `idx_owner_id` (`owner_id`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = DATABASE() AND table_name = 'video'
          AND constraint_name = 'fk_video_owner' AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE `video`
            ADD CONSTRAINT `fk_video_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_user` (`id`);
    END IF;
END//
DELIMITER ;

CALL `migrate_video`();
DROP PROCEDURE IF EXISTS `migrate_video`;

-- 3. 点赞记录表：同一用户对同一视频只能点赞一次
CREATE TABLE IF NOT EXISTS `video_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户 ID',
    `video_id`    BIGINT   NOT NULL COMMENT '视频 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_like_user_video` (`user_id`, `video_id`),
    KEY `idx_video_like_video` (`video_id`),
    CONSTRAINT `fk_video_like_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
    CONSTRAINT `fk_video_like_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞记录表，保证同一用户对同一视频只能点赞一次';

-- 4. 为演示数据补充默认封面，上传的视频使用 /api/video/cover/{fileName}
UPDATE `video`
SET `cover_url` = CONCAT('/covers/demo-', MOD(`id` - 1, 6) + 1, '.svg')
WHERE `id` BETWEEN 1 AND 30 AND (`cover_url` IS NULL OR `cover_url` = '');
