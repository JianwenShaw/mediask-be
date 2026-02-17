-- MediAsk 开发环境数据库初始化脚本
-- 每次执行初始化脚本时，先清空现有表，便于快速重建开发数据
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `schedule_events`;
DROP TABLE IF EXISTS `appointment_events`;
DROP TABLE IF EXISTS `schedule_exceptions`;
DROP TABLE IF EXISTS `schedule_template_rules`;
DROP TABLE IF EXISTS `schedule_templates`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `appointment_slots`;
DROP TABLE IF EXISTS `doctor_schedules`;
DROP TABLE IF EXISTS `schedule_plan_constraint_snapshot`;
DROP TABLE IF EXISTS `schedule_plan_items`;
DROP TABLE IF EXISTS `schedule_plan`;
DROP TABLE IF EXISTS `calendar_day`;
DROP TABLE IF EXISTS `department_schedule_demand`;
DROP TABLE IF EXISTS `doctor_time_off`;
DROP TABLE IF EXISTS `doctor_availability_rules`;
DROP TABLE IF EXISTS `ai_metrics_dept_daily`;
DROP TABLE IF EXISTS `ai_metrics_daily`;
DROP TABLE IF EXISTS `ai_feedback_reviews`;
DROP TABLE IF EXISTS `ai_messages`;
DROP TABLE IF EXISTS `ai_conversations`;
DROP TABLE IF EXISTS `doctors`;
DROP TABLE IF EXISTS `departments`;
DROP TABLE IF EXISTS `hospitals`;
DROP TABLE IF EXISTS `role_permissions`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `permissions`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `test_connections`;

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
-- 医院组织与医生档案
-- =========================

CREATE TABLE IF NOT EXISTS `hospitals` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `hospital_name` VARCHAR(128) NOT NULL COMMENT '医院名称',
  `hospital_code` VARCHAR(64) NOT NULL COMMENT '医院编码',
  `hospital_level` VARCHAR(32) DEFAULT NULL COMMENT '医院等级',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital_code` (`hospital_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院表';

CREATE TABLE IF NOT EXISTS `departments` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `hospital_id` BIGINT NOT NULL COMMENT '医院ID',
  `dept_code` VARCHAR(64) NOT NULL COMMENT '科室编码',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '科室名称',
  `dept_intro` VARCHAR(1000) DEFAULT NULL COMMENT '科室简介',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospital_dept_code` (`hospital_id`, `dept_code`),
  KEY `idx_department_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

CREATE TABLE IF NOT EXISTS `doctors` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `hospital_id` BIGINT NOT NULL COMMENT '所属医院ID',
  `dept_id` BIGINT NOT NULL COMMENT '所属科室ID',
  `doctor_code` VARCHAR(64) NOT NULL COMMENT '医生编码',
  `title` VARCHAR(64) DEFAULT NULL COMMENT '职称',
  `specialty` VARCHAR(1000) DEFAULT NULL COMMENT '擅长领域(JSON)',
  `introduction` VARCHAR(2000) DEFAULT NULL COMMENT '个人简介',
  `consultation_fee` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '诊疗费用',
  `license_number` VARCHAR(128) DEFAULT NULL COMMENT '执业证书号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_user` (`user_id`),
  UNIQUE KEY `uk_doctor_code` (`doctor_code`),
  KEY `idx_doctor_dept` (`dept_id`),
  KEY `idx_doctor_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生档案表';

-- =========================
-- AI问诊会话与指标
-- =========================

CREATE TABLE IF NOT EXISTS `ai_conversations` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `conversation_uuid` VARCHAR(64) NOT NULL COMMENT '业务会话UUID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `scene_type` VARCHAR(32) NOT NULL COMMENT '场景类型',
  `summary` VARCHAR(2000) DEFAULT NULL COMMENT '会话摘要',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-进行中 2-已结束',
  `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `ended_at` DATETIME DEFAULT NULL COMMENT '结束时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_uuid` (`conversation_uuid`),
  KEY `idx_ai_conv_user` (`user_id`),
  KEY `idx_ai_conv_started` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS `ai_messages` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
  `role` TINYINT NOT NULL COMMENT '消息角色 1-user 2-assistant 3-system',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `context` TEXT DEFAULT NULL COMMENT 'RAG上下文(JSON)',
  `tokens_used` INT DEFAULT NULL COMMENT '消耗Token数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_msg_conv` (`conversation_id`),
  KEY `idx_ai_msg_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

CREATE TABLE IF NOT EXISTS `ai_feedback_reviews` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
  `doctor_id` BIGINT NOT NULL COMMENT '复核医生ID',
  `department_id` BIGINT NOT NULL COMMENT '科室ID',
  `review_score` TINYINT NOT NULL COMMENT '复核评分 1-5',
  `is_adopted` TINYINT NOT NULL COMMENT '是否采纳 0-否 1-是',
  `review_comment` VARCHAR(1000) DEFAULT NULL COMMENT '复核意见',
  `reviewed_at` DATETIME NOT NULL COMMENT '复核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_review_date` (`reviewed_at`),
  KEY `idx_ai_review_dept` (`department_id`, `reviewed_at`),
  KEY `idx_ai_review_doctor` (`doctor_id`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI复核记录表';

CREATE TABLE IF NOT EXISTS `ai_metrics_daily` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `metric_date` DATE NOT NULL COMMENT '统计日期',
  `total_conversations` BIGINT NOT NULL DEFAULT 0 COMMENT '总会话数',
  `active_users` BIGINT NOT NULL DEFAULT 0 COMMENT '活跃用户数',
  `total_messages` BIGINT NOT NULL DEFAULT 0 COMMENT '总消息数',
  `total_reviews` BIGINT NOT NULL DEFAULT 0 COMMENT '总复核数',
  `avg_review_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '平均评分',
  `accuracy_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '准确率',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_metrics_daily_date` (`metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI日指标汇总表';

CREATE TABLE IF NOT EXISTS `ai_metrics_dept_daily` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `metric_date` DATE NOT NULL COMMENT '统计日期',
  `department_id` BIGINT NOT NULL COMMENT '科室ID',
  `total_conversations` BIGINT NOT NULL DEFAULT 0 COMMENT '会话数',
  `total_reviews` BIGINT NOT NULL DEFAULT 0 COMMENT '复核数',
  `avg_review_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '平均评分',
  `accuracy_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '准确率',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_metrics_dept_date` (`metric_date`, `department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分科室日指标表';

-- =========================
-- 排班管理表
-- =========================

CREATE TABLE IF NOT EXISTS `doctor_schedules` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `time_period` TINYINT NOT NULL COMMENT '时段 1-上午 2-下午 3-晚上',
  `period_start_time` TIME NOT NULL COMMENT '时段开始时间',
  `period_end_time` TIME NOT NULL COMMENT '时段结束时间',
  `slot_duration_minutes` INT NOT NULL DEFAULT 15 COMMENT '号源时长(分钟)',
  `total_slots` INT NOT NULL DEFAULT 0 COMMENT '总号源数',
  `available_slots` INT NOT NULL DEFAULT 0 COMMENT '剩余号源',
  `fee` DECIMAL(10,2) NOT NULL DEFAULT 50.00 COMMENT '挂号费',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停诊 1-开放 2-约满 3-过期',
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型 TEMPLATE/MANUAL',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_date_period` (`doctor_id`, `schedule_date`, `time_period`),
  KEY `idx_schedule_doctor` (`doctor_id`),
  KEY `idx_schedule_date` (`schedule_date`),
  KEY `idx_schedule_doctor_date` (`doctor_id`, `schedule_date`),
  KEY `idx_schedule_date_status` (`schedule_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生排班表';

CREATE TABLE IF NOT EXISTS `appointment_slots` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
  `slot_time` TIME NOT NULL COMMENT '时段开始(如09:00)',
  `slot_end_time` TIME NOT NULL COMMENT '时段结束',
  `is_occupied` TINYINT NOT NULL DEFAULT 0 COMMENT '是否占用 0-空闲 1-占用',
  `appt_id` BIGINT DEFAULT NULL COMMENT '关联预约ID',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_slot` (`schedule_id`, `slot_time`),
  KEY `idx_slot_schedule` (`schedule_id`),
  KEY `idx_slot_time` (`slot_time`),
  KEY `idx_slot_schedule_occupied` (`schedule_id`, `is_occupied`)
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
  `slot_id` BIGINT DEFAULT NULL COMMENT '号源ID',
  `appt_date` DATE NOT NULL COMMENT '就诊日期',
  `time_period` TINYINT NOT NULL COMMENT '时段 1-上午 2-下午 3-晚上',
  `appt_time` TIME NOT NULL COMMENT '具体时间段',
  `appt_end_time` TIME DEFAULT NULL COMMENT '结束时间',
  `appt_status` TINYINT NOT NULL DEFAULT 1 COMMENT '预约状态 1-待支付 2-已预约 3-已就诊 4-已取消 5-爽约',
  `chief_complaint` VARCHAR(500) DEFAULT NULL COMMENT '主诉(AI生成)',
  `appt_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '挂号费',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `visited_at` DATETIME DEFAULT NULL COMMENT '就诊时间',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appt_no` (`appt_no`),
  UNIQUE KEY `uk_patient_start` (`patient_id`, `appt_date`, `appt_time`),
  KEY `idx_appt_patient` (`patient_id`),
  KEY `idx_appt_doctor` (`doctor_id`),
  KEY `idx_appt_schedule` (`schedule_id`),
  KEY `idx_appt_date` (`appt_date`),
  KEY `idx_appt_status` (`appt_status`),
  KEY `idx_appt_patient_date` (`patient_id`, `appt_date`),
  KEY `idx_appt_doctor_date_time` (`doctor_id`, `appt_date`, `appt_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';

CREATE TABLE IF NOT EXISTS `schedule_templates` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `template_name` VARCHAR(64) NOT NULL COMMENT '模板名称',
  `effective_start_date` DATE NOT NULL COMMENT '生效开始日期',
  `effective_end_date` DATE NOT NULL COMMENT '生效结束日期',
  `cancel_deadline_minutes` INT NOT NULL DEFAULT 120 COMMENT '取消截止分钟',
  `default_fee` DECIMAL(10,2) NOT NULL DEFAULT 50.00 COMMENT '默认挂号费',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_doctor_status` (`doctor_id`, `status`),
  KEY `idx_template_effective` (`effective_start_date`, `effective_end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班模板表';

CREATE TABLE IF NOT EXISTS `schedule_template_rules` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `weekday` TINYINT NOT NULL COMMENT '周几 1-7',
  `time_period` TINYINT NOT NULL COMMENT '时段',
  `period_start_time` TIME NOT NULL COMMENT '开始时间',
  `period_end_time` TIME NOT NULL COMMENT '结束时间',
  `slot_duration_minutes` INT NOT NULL DEFAULT 15 COMMENT '号源时长',
  `slot_capacity` INT NOT NULL DEFAULT 20 COMMENT '号源容量',
  `fee` DECIMAL(10,2) NOT NULL DEFAULT 50.00 COMMENT '挂号费',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_weekday_period` (`template_id`, `weekday`, `time_period`),
  KEY `idx_rule_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班模板规则表';

CREATE TABLE IF NOT EXISTS `schedule_exceptions` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `exception_date` DATE NOT NULL COMMENT '例外日期',
  `action_type` VARCHAR(16) NOT NULL COMMENT '例外动作 CLOSE/OPEN/ADJUST',
  `override_start_time` TIME DEFAULT NULL COMMENT '覆盖开始时间',
  `override_end_time` TIME DEFAULT NULL COMMENT '覆盖结束时间',
  `override_capacity` INT DEFAULT NULL COMMENT '覆盖容量',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '原因',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_exception_doctor_date` (`doctor_id`, `exception_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班例外规则表';

CREATE TABLE IF NOT EXISTS `doctor_availability_rules` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `weekday` TINYINT NOT NULL COMMENT '周几 1-7',
  `period_code` TINYINT NOT NULL COMMENT '时段编码 1上午 2下午 3晚上',
  `is_available` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可排班',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '偏好优先级，越大越偏好',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_weekday_period` (`doctor_id`, `weekday`, `period_code`),
  KEY `idx_doc_availability_status` (`doctor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生可排班规则';

CREATE TABLE IF NOT EXISTS `doctor_time_off` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `start_date` DATE NOT NULL COMMENT '请假开始日期',
  `end_date` DATE NOT NULL COMMENT '请假结束日期',
  `period_code` TINYINT DEFAULT NULL COMMENT '请假时段，NULL表示全天',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '请假原因',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_time_off_range` (`doctor_id`, `start_date`, `end_date`),
  KEY `idx_doc_time_off_status` (`doctor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生请假规则';

CREATE TABLE IF NOT EXISTS `department_schedule_demand` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `department_id` BIGINT NOT NULL COMMENT '科室ID',
  `demand_date` DATE NOT NULL COMMENT '需求日期',
  `period_code` TINYINT NOT NULL COMMENT '时段编码 1上午 2下午 3晚上',
  `required_doctors` INT NOT NULL COMMENT '最少排班医生数',
  `min_senior_doctors` INT NOT NULL DEFAULT 0 COMMENT '最少资深医生数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_demand_date_period` (`department_id`, `demand_date`, `period_code`),
  KEY `idx_dept_demand_range` (`department_id`, `demand_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室排班需求';

CREATE TABLE IF NOT EXISTS `calendar_day` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `calendar_date` DATE NOT NULL COMMENT '自然日',
  `is_holiday` TINYINT NOT NULL DEFAULT 0 COMMENT '是否法定节假日',
  `is_makeup_workday` TINYINT NOT NULL DEFAULT 0 COMMENT '是否调休工作日',
  `holiday_name` VARCHAR(64) DEFAULT NULL COMMENT '节假日名称',
  `region_code` VARCHAR(32) NOT NULL DEFAULT 'CN-NATIONAL' COMMENT '地区编码',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_day_region` (`calendar_date`, `region_code`),
  KEY `idx_calendar_day_flags` (`calendar_date`, `is_holiday`, `is_makeup_workday`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='法定节假日与调休日历';

CREATE TABLE IF NOT EXISTS `schedule_plan` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `plan_code` VARCHAR(64) NOT NULL COMMENT '方案编码',
  `department_id` BIGINT NOT NULL COMMENT '科室ID',
  `start_date` DATE NOT NULL COMMENT '排班开始日期',
  `end_date` DATE NOT NULL COMMENT '排班结束日期',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `plan_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
  `solver_strategy` VARCHAR(32) DEFAULT NULL COMMENT '求解策略',
  `generated_by` BIGINT DEFAULT NULL COMMENT '生成人',
  `total_score` DECIMAL(8,2) DEFAULT NULL COMMENT '方案总分',
  `hard_violation_count` INT NOT NULL DEFAULT 0 COMMENT '硬约束违例数',
  `warnings_json` JSON DEFAULT NULL COMMENT '告警信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_plan_code_ver` (`plan_code`, `version_no`),
  KEY `idx_schedule_plan_dept_range` (`department_id`, `start_date`, `end_date`, `plan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班方案主表';

CREATE TABLE IF NOT EXISTS `schedule_plan_items` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `plan_id` BIGINT NOT NULL COMMENT '方案ID',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `period_code` TINYINT NOT NULL COMMENT '时段编码',
  `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
  `is_senior` TINYINT NOT NULL DEFAULT 0 COMMENT '是否资深医生',
  `reason_json` JSON DEFAULT NULL COMMENT '分配原因',
  `penalty_json` JSON DEFAULT NULL COMMENT '惩罚项',
  `score_delta` DECIMAL(8,2) DEFAULT NULL COMMENT '分配增量分',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_slot_doctor` (`plan_id`, `schedule_date`, `period_code`, `doctor_id`),
  KEY `idx_plan_item_slot` (`plan_id`, `schedule_date`, `period_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班方案明细';

CREATE TABLE IF NOT EXISTS `schedule_plan_constraint_snapshot` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `plan_id` BIGINT NOT NULL COMMENT '方案ID',
  `snapshot_type` VARCHAR(32) NOT NULL COMMENT 'DOCTOR_RULES/TIME_OFF/DEMAND/CALENDAR/HARD_SOFT',
  `snapshot_json` JSON NOT NULL COMMENT '快照内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_snapshot_type` (`plan_id`, `snapshot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班约束快照';

CREATE TABLE IF NOT EXISTS `appointment_events` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `appointment_id` BIGINT NOT NULL COMMENT '预约ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型',
  `from_status` TINYINT DEFAULT NULL COMMENT '原状态',
  `to_status` TINYINT DEFAULT NULL COMMENT '新状态',
  `operator_type` VARCHAR(16) DEFAULT NULL COMMENT '操作者类型',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作者ID',
  `payload_json` JSON DEFAULT NULL COMMENT '事件载荷',
  `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_appt_event_time` (`appointment_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约事件表';

CREATE TABLE IF NOT EXISTS `schedule_events` (
  `id` BIGINT NOT NULL COMMENT '雪花ID',
  `schedule_id` BIGINT NOT NULL COMMENT '排班ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型',
  `from_status` TINYINT DEFAULT NULL COMMENT '原状态',
  `to_status` TINYINT DEFAULT NULL COMMENT '新状态',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作者ID',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '原因',
  `payload_json` JSON DEFAULT NULL COMMENT '事件载荷',
  `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_schedule_event_time` (`schedule_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班事件表';

-- =========================
-- 初始化测试数据
-- =========================

-- 插入测试医生用户
INSERT INTO `users` (`id`, `username`, `phone`, `password`, `user_type`, `real_name`, `gender`) VALUES
(100000000001, 'doctor1', '13800138001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 2, '张医生', 1),
(100000000002, 'doctor2', '13800138002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 2, '李医生', 1),
(100000000003, 'patient1', '13900139001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, '王患者', 1),
(100000000004, 'patient2', '13900139002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, '赵患者', 2),
(100000000005, 'admin', '13700137000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 3, '系统管理员', 1);

INSERT INTO `roles` (`id`, `role_code`, `role_name`, `description`) VALUES
(100000000001, 'admin', '管理员', '系统管理员'),
(100000000002, 'doctor', '医生', '医生角色'),
(100000000003, 'patient', '患者', '患者角色');

INSERT INTO `permissions` (`id`, `perm_code`, `perm_name`, `description`) VALUES
(100000000001, 'schedule:create', '创建排班', '创建排班和模板'),
(100000000002, 'schedule:update', '更新排班', '停诊开诊和更新模板'),
(100000000003, 'schedule:query', '查询排班', '查询排班和模板'),
(100000000004, 'schedule:delete', '删除排班', '删除排班'),
(100000000005, 'ai:metrics:view', '查看AI指标', '查看AI问诊指标');

INSERT INTO `user_roles` (`id`, `user_id`, `role_id`) VALUES
(100000000001, 100000000001, 100000000002),
(100000000002, 100000000002, 100000000002),
(100000000003, 100000000003, 100000000003),
(100000000004, 100000000004, 100000000003),
(100000000005, 100000000005, 100000000001);

INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`) VALUES
(100000000001, 100000000001, 100000000001),
(100000000002, 100000000001, 100000000002),
(100000000003, 100000000001, 100000000003),
(100000000004, 100000000001, 100000000004),
(100000000005, 100000000001, 100000000005),
(100000000006, 100000000002, 100000000003);

INSERT INTO `hospitals` (`id`, `hospital_name`, `hospital_code`, `hospital_level`, `address`, `contact_phone`, `status`) VALUES
(100000000001, 'MediAsk附属医院', 'MDA001', '三级甲等', '示例路100号', '010-88886666', 1);

INSERT INTO `departments` (`id`, `hospital_id`, `dept_code`, `dept_name`, `dept_intro`, `display_order`, `status`) VALUES
(100000000001, 100000000001, 'INT', '内科', '内科门诊', 1, 1),
(100000000002, 100000000001, 'SUR', '外科', '外科门诊', 2, 1);

INSERT INTO `doctors` (
  `id`, `user_id`, `hospital_id`, `dept_id`, `doctor_code`, `title`, `specialty`, `introduction`,
  `consultation_fee`, `license_number`, `status`
) VALUES
(100000000001, 100000000001, 100000000001, 100000000001, 'D001', '主治医师',
 '[\"内分泌\",\"高血压\"]', '擅长慢病管理', 50.00, 'LIC-000001', 1),
(100000000002, 100000000002, 100000000001, 100000000002, 'D002', '副主任医师',
 '[\"普通外科\"]', '擅长普外手术咨询', 60.00, 'LIC-000002', 1);

-- 插入排班优化相关测试数据
INSERT INTO `doctor_availability_rules`
(`id`, `doctor_id`, `weekday`, `period_code`, `is_available`, `priority`, `status`) VALUES
(100000000001, 100000000001, 1, 1, 1, 9, 1),
(100000000002, 100000000001, 1, 2, 1, 8, 1),
(100000000003, 100000000001, 2, 1, 1, 9, 1),
(100000000004, 100000000002, 1, 1, 1, 10, 1),
(100000000005, 100000000002, 2, 1, 1, 10, 1);

INSERT INTO `doctor_time_off`
(`id`, `doctor_id`, `start_date`, `end_date`, `period_code`, `reason`, `status`) VALUES
(100000000001, 100000000001, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL, '培训停诊', 1);

INSERT INTO `department_schedule_demand`
(`id`, `department_id`, `demand_date`, `period_code`, `required_doctors`, `min_senior_doctors`, `status`) VALUES
(100000000001, 100000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, 0, 1),
(100000000002, 100000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 1, 0, 1),
(100000000003, 100000000001, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 1, 0, 1);

INSERT INTO `calendar_day`
(`id`, `calendar_date`, `is_holiday`, `is_makeup_workday`, `holiday_name`, `region_code`, `status`) VALUES
(100000000001, '2026-01-01', 1, 0, '元旦', 'CN-NATIONAL', 1),
(100000000002, '2026-02-17', 1, 0, '春节', 'CN-NATIONAL', 1),
(100000000003, '2026-02-18', 1, 0, '春节', 'CN-NATIONAL', 1),
(100000000004, '2026-02-22', 0, 1, '春节调休上班', 'CN-NATIONAL', 1),
(100000000005, '2026-04-05', 1, 0, '清明节', 'CN-NATIONAL', 1),
(100000000006, '2026-05-01', 1, 0, '劳动节', 'CN-NATIONAL', 1);

-- 插入测试排班
INSERT INTO `doctor_schedules` (
  `id`, `doctor_id`, `schedule_date`, `time_period`, `period_start_time`, `period_end_time`,
  `slot_duration_minutes`, `total_slots`, `available_slots`, `fee`, `status`
) VALUES
(100000000001, 100000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 15, 20, 20, 50.00, 1),
(100000000002, 100000000001, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, '14:00:00', '18:00:00', 15, 15, 15, 50.00, 1),
(100000000003, 100000000002, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, '08:00:00', '12:00:00', 20, 25, 25, 60.00, 1),
(100000000004, 100000000001, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, '08:00:00', '12:00:00', 15, 20, 20, 50.00, 1);

-- 插入测试号源
INSERT INTO `appointment_slots` (`id`, `schedule_id`, `slot_time`, `slot_end_time`, `is_occupied`) VALUES
-- 医生1 上午号源
(100000000001, 100000000001, '09:00:00', '09:15:00', 0),
(100000000002, 100000000001, '09:15:00', '09:30:00', 0),
(100000000003, 100000000001, '09:30:00', '09:45:00', 0),
(100000000004, 100000000001, '09:45:00', '10:00:00', 0),
(100000000005, 100000000001, '10:00:00', '10:15:00', 0),
(100000000006, 100000000001, '10:15:00', '10:30:00', 0),
(100000000007, 100000000001, '10:30:00', '10:45:00', 0),
(100000000008, 100000000001, '10:45:00', '11:00:00', 0),
(100000000009, 100000000001, '11:00:00', '11:15:00', 0),
(100000000010, 100000000001, '11:15:00', '11:30:00', 0),
-- 医生1 下午号源
(100000000011, 100000000002, '14:00:00', '14:15:00', 0),
(100000000012, 100000000002, '14:15:00', '14:30:00', 0),
(100000000013, 100000000002, '14:30:00', '14:45:00', 0),
(100000000014, 100000000002, '14:45:00', '15:00:00', 0),
(100000000015, 100000000002, '15:00:00', '15:15:00', 0),
-- 医生2 上午号源
(100000000016, 100000000003, '09:00:00', '09:20:00', 0),
(100000000017, 100000000003, '09:20:00', '09:40:00', 0),
(100000000018, 100000000003, '09:40:00', '10:00:00', 0),
(100000000019, 100000000003, '10:00:00', '10:20:00', 0),
(100000000020, 100000000003, '10:20:00', '10:40:00', 0);

-- 插入AI会话与消息示例
INSERT INTO `ai_conversations` (`id`, `conversation_uuid`, `user_id`, `scene_type`, `summary`, `status`, `started_at`) VALUES
(100000000001, 'conv-demo-0001', 100000000003, 'pre_diagnosis', '头痛三天，伴随低热', 2, NOW());

INSERT INTO `ai_messages` (`id`, `conversation_id`, `role`, `content`, `context`, `tokens_used`, `created_at`) VALUES
(100000000001, 100000000001, 1, '我最近三天头痛，还有点发烧', NULL, 120, NOW()),
(100000000002, 100000000001, 2, '建议先做体温和血常规检查，必要时就诊神经内科。', '{\"rag\":[\"发热头痛鉴别诊断\"]}', 260, NOW());

SET FOREIGN_KEY_CHECKS = 1;
