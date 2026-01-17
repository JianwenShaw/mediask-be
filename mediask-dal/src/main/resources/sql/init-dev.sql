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

-- =========================
-- 基础认证与授权表
-- =========================

CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `id_card_encrypted` VARCHAR(255) DEFAULT NULL COMMENT 'AES加密身份证号(Base64)',
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `user_type` TINYINT NOT NULL COMMENT '用户类型 1-患者 2-医生 3-管理员',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `gender` TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
  `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `permissions` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `perm_code` VARCHAR(64) NOT NULL COMMENT '权限编码',
  `perm_name` VARCHAR(64) NOT NULL COMMENT '权限名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `user_roles` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_roles_user_id` (`user_id`),
  KEY `idx_user_roles_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `role_permissions` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_permissions_role_id` (`role_id`),
  KEY `idx_role_permissions_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =========================
-- 排班管理表
-- =========================

CREATE TABLE IF NOT EXISTS `doctor_schedules` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `time_period` TINYINT NOT NULL COMMENT '时段 1-上午 2-下午 3-晚上',
  `total_slots` INT NOT NULL DEFAULT 0 COMMENT '总号源数',
  `available_slots` INT NOT NULL DEFAULT 0 COMMENT '剩余号源',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_doctor` (`doctor_id`),
  KEY `idx_schedule_date` (`schedule_date`),
  KEY `idx_schedule_doctor_date` (`doctor_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生排班表';

CREATE TABLE IF NOT EXISTS `appointment_slots` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
  `slot_time` TIME NOT NULL COMMENT '时段(如09:00)',
  `is_occupied` TINYINT NOT NULL DEFAULT 0 COMMENT '是否占用 0-空闲 1-占用',
  `appt_id` BIGINT DEFAULT NULL COMMENT '关联预约ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_slot_schedule` (`schedule_id`),
  KEY `idx_slot_time` (`slot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号源时段表';

-- =========================
-- 预约挂号表
-- =========================

CREATE TABLE IF NOT EXISTS `appointments` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `appt_no` VARCHAR(32) NOT NULL COMMENT '预约单号',
  `patient_id` BIGINT NOT NULL COMMENT '患者ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
  `appt_date` DATE NOT NULL COMMENT '就诊日期',
  `time_period` TINYINT NOT NULL COMMENT '时段 1-上午 2-下午 3-晚上',
  `appt_time` TIME NOT NULL COMMENT '具体时间段',
  `appt_status` TINYINT NOT NULL DEFAULT 1 COMMENT '预约状态 1-待支付 2-已预约 3-已就诊 4-已取消 5-爽约',
  `chief_complaint` VARCHAR(500) DEFAULT NULL COMMENT '主诉(AI生成)',
  `appt_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '挂号费',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `visited_at` DATETIME DEFAULT NULL COMMENT '就诊时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appt_no` (`appt_no`),
  KEY `idx_appt_patient` (`patient_id`),
  KEY `idx_appt_doctor` (`doctor_id`),
  KEY `idx_appt_date` (`appt_date`),
  KEY `idx_appt_status` (`appt_status`),
  KEY `idx_appt_patient_date` (`patient_id`, `appt_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';

-- =========================
-- 初始化测试数据
-- =========================

-- 插入测试医生用户
INSERT INTO `users` (`id`, `username`, `phone`, `password`, `user_type`, `real_name`, `gender`) VALUES
(100000000000000001, 'doctor1', '13800138001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 2, '张医生', 1),
(100000000000000002, 'doctor2', '13800138002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 2, '李医生', 1),
(100000000000000003, 'patient1', '13900139001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, '王患者', 1),
(100000000000000004, 'patient2', '13900139002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, '赵患者', 2);

-- 插入测试排班
INSERT INTO `doctor_schedules` (`id`, `doctor_id`, `schedule_date`, `time_period`, `total_slots`, `available_slots`, `status`) VALUES
(100000000000000001, 100000000000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 20, 20, 1),
(100000000000000002, 100000000000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 15, 15, 1),
(100000000000000003, 100000000000000002, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 25, 25, 1),
(100000000000000004, 100000000000000001, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 20, 20, 1);

-- 插入测试号源
INSERT INTO `appointment_slots` (`id`, `schedule_id`, `slot_time`, `is_occupied`) VALUES
-- 医生1 上午号源
(100000000000000001, 100000000000000001, '09:00:00', 0),
(100000000000000002, 100000000000000001, '09:15:00', 0),
(100000000000000003, 100000000000000001, '09:30:00', 0),
(100000000000000004, 100000000000000001, '09:45:00', 0),
(100000000000000005, 100000000000000001, '10:00:00', 0),
(100000000000000006, 100000000000000001, '10:15:00', 0),
(100000000000000007, 100000000000000001, '10:30:00', 0),
(100000000000000008, 100000000000000001, '10:45:00', 0),
(100000000000000009, 100000000000000001, '11:00:00', 0),
(100000000000000010, 100000000000000001, '11:15:00', 0),
-- 医生1 下午号源
(100000000000000011, 100000000000000002, '14:00:00', 0),
(100000000000000012, 100000000000000002, '14:15:00', 0),
(100000000000000013, 100000000000000002, '14:30:00', 0),
(100000000000000014, 100000000000000002, '14:45:00', 0),
(100000000000000015, 100000000000000002, '15:00:00', 0),
-- 医生2 上午号源
(100000000000000016, 100000000000000003, '09:00:00', 0),
(100000000000000017, 100000000000000003, '09:20:00', 0),
(100000000000000018, 100000000000000003, '09:40:00', 0),
(100000000000000019, 100000000000000003, '10:00:00', 0),
(100000000000000020, 100000000000000003, '10:20:00', 0);
