-- VideoHub database schema
-- MySQL 8.x

CREATE DATABASE IF NOT EXISTS video_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE video_db;

CREATE TABLE IF NOT EXISTS `video` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         VARCHAR(200) NOT NULL COMMENT '视频标题',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '视频简介',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '磁盘存储文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '用户原始文件名',
    `url`           VARCHAR(500) NOT NULL COMMENT '访问路径',
    `cover_url`     VARCHAR(500) DEFAULT NULL COMMENT '封面路径',
    `size`          BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `duration`      INT          NOT NULL DEFAULT 0 COMMENT '时长（秒）',
    `format`        VARCHAR(20)  DEFAULT NULL COMMENT '格式：mp4/webm/mkv/mov/avi',
    `category`      VARCHAR(50)  NOT NULL DEFAULT '默认' COMMENT '分类',
    `tags`          VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    `play_count`    BIGINT       NOT NULL DEFAULT 0 COMMENT '播放量',
    `like_count`    BIGINT       NOT NULL DEFAULT 0 COMMENT '点赞数',
    `uploader`      VARCHAR(50)  NOT NULL DEFAULT 'anonymous' COMMENT '上传者',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用，1=正常',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status_deleted` (`status`, `deleted`),
    KEY `idx_play_count` (`play_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频信息表';


