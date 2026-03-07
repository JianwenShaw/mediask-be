-- ============================================================
-- 04-appointment.sql  —  预约挂号
-- ============================================================
-- 包含：appointments（增强：新增 conversation_id 关联AI会话）

CREATE TABLE `appointments` (
    `id`              BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `appt_no`         VARCHAR(32)    NOT NULL                                              COMMENT '预约单号',
    `patient_id`      BIGINT         NOT NULL                                              COMMENT '患者ID（users.id）',
    `doctor_id`       BIGINT         NOT NULL                                              COMMENT '医生ID（doctors.id）',
    `schedule_id`     BIGINT         NOT NULL                                              COMMENT '排班ID',
    `slot_id`         BIGINT         DEFAULT NULL                                          COMMENT '号源ID',
    `conversation_id` BIGINT         DEFAULT NULL                                          COMMENT '关联AI问诊会话ID',
    `appt_date`       DATE           NOT NULL                                              COMMENT '就诊日期',
    `time_period`     TINYINT        NOT NULL                                              COMMENT '时段 1-上午 2-下午 3-晚上',
    `appt_time`       TIME           NOT NULL                                              COMMENT '具体开始时间',
    `appt_end_time`   TIME           DEFAULT NULL                                          COMMENT '具体结束时间',
    `appt_status`     TINYINT        NOT NULL DEFAULT 1                                    COMMENT '预约状态 1-待支付 2-已预约 3-已就诊 4-已取消 5-爽约',
    `chief_complaint` VARCHAR(500)   DEFAULT NULL                                          COMMENT '主诉（AI生成或患者填写）',
    `appt_fee`        DECIMAL(10, 2) DEFAULT 0.00                                          COMMENT '挂号费',
    `paid_at`         DATETIME       DEFAULT NULL                                          COMMENT '支付时间',
    `visited_at`      DATETIME       DEFAULT NULL                                          COMMENT '就诊时间',
    `cancelled_at`    DATETIME       DEFAULT NULL                                          COMMENT '取消时间',
    `cancel_reason`   VARCHAR(255)   DEFAULT NULL                                          COMMENT '取消原因',
    `version`         INT            NOT NULL DEFAULT 0                                    COMMENT '乐观锁版本',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME       DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appt_no` (`appt_no`),
    UNIQUE KEY `uk_patient_start` (`patient_id`, `appt_date`, `appt_time`),
    KEY `idx_appt_patient` (`patient_id`),
    KEY `idx_appt_doctor` (`doctor_id`),
    KEY `idx_appt_schedule` (`schedule_id`),
    KEY `idx_appt_date` (`appt_date`),
    KEY `idx_appt_status` (`appt_status`),
    KEY `idx_appt_patient_date` (`patient_id`, `appt_date`),
    KEY `idx_appt_doctor_date_time` (`doctor_id`, `appt_date`, `appt_time`),
    KEY `idx_appt_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约挂号表';
