-- MediAsk 开发环境数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `mediask_dev` 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `mediask_dev`;

-- 连接测试表
CREATE TABLE IF NOT EXISTS `test_connections` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '测试消息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接测试表';
