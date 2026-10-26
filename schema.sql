-- VideoHub 数据库结构脚本
-- MySQL 8.x：创建数据库、用户表、视频表、点赞记录表。

CREATE DATABASE IF NOT EXISTS video_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE video_db;

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

-- 默认管理员：admin / admin123（密码仅保存 BCrypt 摘要，首次登录后请修改）
INSERT IGNORE INTO `app_user`
    (`id`, `username`, `password_hash`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
    (1, 'admin', '$2b$12$V4rbLdZQUtLLMHavf50ER.lC1wpUAF11CUVDgAejvkNh5aVd0k/P6', '管理员', 'ADMIN', 1, NOW(), NOW());

CREATE TABLE IF NOT EXISTS `video` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`           VARCHAR(200) NOT NULL COMMENT '视频标题',
    `description`     VARCHAR(500) DEFAULT NULL COMMENT '视频简介',
    `file_name`       VARCHAR(255) NOT NULL COMMENT '磁盘存储文件名',
    `original_name`   VARCHAR(255) NOT NULL COMMENT '用户原始文件名',
    `url`             VARCHAR(500) NOT NULL COMMENT '访问路径',
    `cover_file_name` VARCHAR(255) DEFAULT NULL COMMENT '封面磁盘文件名',
    `cover_url`       VARCHAR(500) DEFAULT NULL COMMENT '封面访问路径',
    `size`            BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `duration`        INT          NOT NULL DEFAULT 0 COMMENT '时长（秒）',
    `format`          VARCHAR(20)  DEFAULT NULL COMMENT '格式：mp4/webm/mkv/mov/avi',
    `category`        VARCHAR(50)  NOT NULL DEFAULT '默认' COMMENT '分类',
    `tags`            VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    `play_count`      BIGINT       NOT NULL DEFAULT 0 COMMENT '播放量',
    `like_count`      BIGINT       NOT NULL DEFAULT 0 COMMENT '点赞数',
    `owner_id`        BIGINT       NOT NULL DEFAULT 1 COMMENT '上传用户 ID',
    `uploader`        VARCHAR(50)  NOT NULL DEFAULT 'anonymous' COMMENT '上传者显示名称快照',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用，1=正常',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status_deleted` (`status`, `deleted`),
    KEY `idx_play_count` (`play_count`),
    KEY `idx_owner_id` (`owner_id`),
    CONSTRAINT `fk_video_owner` FOREIGN KEY (`owner_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频信息表';

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
