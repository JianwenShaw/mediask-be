-- ============================================================
-- 03-scheduling.sql  —  排班管理
-- ============================================================
-- 包含：doctor_availability_rules, doctor_time_off,
--       department_schedule_demand (改为周模板),
--       calendar_day, doctor_schedules (增强),
--       appointment_slots, schedule_plan (内联快照),
--       schedule_plan_items, schedule_rule_profile
--
-- 变更说明：
--   1. 删除 schedule_templates / schedule_template_rules（与 availability_rules 重叠）
--   2. 删除 schedule_exceptions（合并入 doctor_time_off）
--   3. 删除 schedule_plan_constraint_snapshot（内联入 schedule_plan）
--   4. department_schedule_demand 从按日需求改为周模板
--   5. doctor_schedules.source_type 改为 plan_item_id 精确关联
--   6. doctor_time_off 新增 off_type 区分请假/停诊/调班

-- ----- 医生可排班规则（偏好矩阵，不变） -----
CREATE TABLE `doctor_availability_rules` (
    `id`           BIGINT   NOT NULL                                              COMMENT '雪花ID',
    `doctor_id`    BIGINT   NOT NULL                                              COMMENT '医生ID',
    `weekday`      TINYINT  NOT NULL                                              COMMENT '周几 1-7（周一~周日）',
    `period_code`  TINYINT  NOT NULL                                              COMMENT '时段编码 1-上午 2-下午 3-晚上',
    `is_available` TINYINT  NOT NULL DEFAULT 1                                    COMMENT '是否可排班 0-不可 1-可',
    `priority`     INT      NOT NULL DEFAULT 0                                    COMMENT '偏好优先级，越大越偏好',
    `status`       TINYINT  NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   DATETIME DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_weekday_period` (`doctor_id`, `weekday`, `period_code`),
    KEY `idx_doc_availability_status` (`doctor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生可排班规则';

-- ----- 医生请假/停诊（增强：新增 off_type 吸收原 schedule_exceptions） -----
CREATE TABLE `doctor_time_off` (
    `id`          BIGINT       NOT NULL                                              COMMENT '雪花ID',
    `doctor_id`   BIGINT       NOT NULL                                              COMMENT '医生ID',
    `off_type`    VARCHAR(16)  NOT NULL DEFAULT 'LEAVE'                              COMMENT '类型 LEAVE-请假 CLOSE-停诊 SWAP-调班',
    `start_date`  DATE         NOT NULL                                              COMMENT '开始日期',
    `end_date`    DATE         NOT NULL                                              COMMENT '结束日期',
    `period_code` TINYINT      DEFAULT NULL                                          COMMENT '时段编码，NULL=全天',
    `reason`      VARCHAR(255) DEFAULT NULL                                          COMMENT '原因',
    `status`      TINYINT      NOT NULL DEFAULT 1                                    COMMENT '状态 0-已撤销 1-生效',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME     DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_time_off_range` (`doctor_id`, `start_date`, `end_date`),
    KEY `idx_doc_time_off_status` (`doctor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生请假/停诊表';

-- ----- 科室排班需求（改为周模板，不再按日） -----
-- 设计变更：去除 demand_date，改为 weekday 周模板模式。
-- 排班引擎按 weekday 匹配当周日期来确定每日需求。
CREATE TABLE `department_schedule_demand` (
    `id`                 BIGINT   NOT NULL                                              COMMENT '雪花ID',
    `department_id`      BIGINT   NOT NULL                                              COMMENT '科室ID',
    `weekday`            TINYINT  NOT NULL                                              COMMENT '周几 1-7',
    `period_code`        TINYINT  NOT NULL                                              COMMENT '时段编码 1-上午 2-下午 3-晚上',
    `required_doctors`   INT      NOT NULL                                              COMMENT '最少排班医生数',
    `min_senior_doctors` INT      NOT NULL DEFAULT 0                                    COMMENT '最少资深医生数',
    `status`             TINYINT  NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`         DATETIME DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_demand_weekday_period` (`department_id`, `weekday`, `period_code`),
    KEY `idx_dept_demand_status` (`department_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室排班需求（周模板）';

-- ----- 法定节假日与调休日历（不变） -----
CREATE TABLE `calendar_day` (
    `id`                 BIGINT      NOT NULL                                              COMMENT '雪花ID',
    `calendar_date`      DATE        NOT NULL                                              COMMENT '自然日',
    `is_holiday`         TINYINT     NOT NULL DEFAULT 0                                    COMMENT '是否法定节假日',
    `is_makeup_workday`  TINYINT     NOT NULL DEFAULT 0                                    COMMENT '是否调休工作日',
    `holiday_name`       VARCHAR(64) DEFAULT NULL                                          COMMENT '节假日名称',
    `region_code`        VARCHAR(32) NOT NULL DEFAULT 'CN-NATIONAL'                        COMMENT '地区编码',
    `status`             TINYINT     NOT NULL DEFAULT 1                                    COMMENT '状态',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`         DATETIME    DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_calendar_day_region` (`calendar_date`, `region_code`),
    KEY `idx_calendar_day_flags` (`calendar_date`, `is_holiday`, `is_makeup_workday`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='法定节假日与调休日历';

-- ----- 医生排班表（增强：source_type → plan_item_id） -----
CREATE TABLE `doctor_schedules` (
    `id`                    BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `doctor_id`             BIGINT         NOT NULL                                              COMMENT '医生ID',
    `schedule_date`         DATE           NOT NULL                                              COMMENT '排班日期',
    `time_period`           TINYINT        NOT NULL                                              COMMENT '时段 1-上午 2-下午 3-晚上',
    `period_start_time`     TIME           NOT NULL                                              COMMENT '时段开始时间',
    `period_end_time`       TIME           NOT NULL                                              COMMENT '时段结束时间',
    `slot_duration_minutes` INT            NOT NULL DEFAULT 15                                   COMMENT '号源时长（分钟）',
    `total_slots`           INT            NOT NULL DEFAULT 0                                    COMMENT '总号源数',
    `available_slots`       INT            NOT NULL DEFAULT 0                                    COMMENT '剩余号源',
    `fee`                   DECIMAL(10, 2) NOT NULL DEFAULT 50.00                                COMMENT '挂号费',
    `status`                TINYINT        NOT NULL DEFAULT 1                                    COMMENT '状态 0-停诊 1-开放 2-约满 3-过期',
    `plan_item_id`          BIGINT         DEFAULT NULL                                          COMMENT '关联排班方案明细ID（NULL=手动创建）',
    `version`               INT            NOT NULL DEFAULT 0                                    COMMENT '乐观锁版本',
    `created_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`            DATETIME       DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctor_date_period` (`doctor_id`, `schedule_date`, `time_period`),
    KEY `idx_schedule_doctor` (`doctor_id`),
    KEY `idx_schedule_date` (`schedule_date`),
    KEY `idx_schedule_doctor_date` (`doctor_id`, `schedule_date`),
    KEY `idx_schedule_date_status` (`schedule_date`, `status`),
    KEY `idx_schedule_plan_item` (`plan_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生排班表';

-- ----- 号源时段表（不变） -----
CREATE TABLE `appointment_slots` (
    `id`            BIGINT   NOT NULL                                              COMMENT '雪花ID',
    `schedule_id`   BIGINT   NOT NULL                                              COMMENT '排班ID',
    `slot_time`     TIME     NOT NULL                                              COMMENT '时段开始（如09:00）',
    `slot_end_time` TIME     NOT NULL                                              COMMENT '时段结束',
    `is_occupied`   TINYINT  NOT NULL DEFAULT 0                                    COMMENT '是否占用 0-空闲 1-占用',
    `appt_id`       BIGINT   DEFAULT NULL                                          COMMENT '关联预约ID',
    `version`       INT      NOT NULL DEFAULT 0                                    COMMENT '乐观锁版本',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_slot` (`schedule_id`, `slot_time`),
    KEY `idx_slot_schedule` (`schedule_id`),
    KEY `idx_slot_time` (`slot_time`),
    KEY `idx_slot_schedule_occupied` (`schedule_id`, `is_occupied`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号源时段表';

-- ----- 排班方案主表（增强：内联约束快照） -----
-- 设计变更：将原独立的 schedule_plan_constraint_snapshot 表合并为 JSON 字段，
-- 包括 doctor_rules_snapshot, time_off_snapshot, demand_snapshot, calendar_snapshot, dsl_snapshot, solver_meta
CREATE TABLE `schedule_plan` (
    `id`                     BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `plan_code`              VARCHAR(64)    NOT NULL                                              COMMENT '方案编码',
    `department_id`          BIGINT         NOT NULL                                              COMMENT '科室ID',
    `start_date`             DATE           NOT NULL                                              COMMENT '排班开始日期',
    `end_date`               DATE           NOT NULL                                              COMMENT '排班结束日期',
    `version_no`             INT            NOT NULL DEFAULT 1                                    COMMENT '版本号',
    `plan_status`            VARCHAR(16)    NOT NULL DEFAULT 'DRAFT'                              COMMENT '状态 DRAFT/PUBLISHED/ARCHIVED',
    `solver_strategy`        VARCHAR(32)    DEFAULT NULL                                          COMMENT '求解策略 RULE_GREEDY/LOCAL_SEARCH/CP_SAT/AUTO',
    `generated_by`           BIGINT         DEFAULT NULL                                          COMMENT '生成人ID',
    `total_score`            DECIMAL(8, 2)  DEFAULT NULL                                          COMMENT '方案总分',
    `hard_violation_count`   INT            NOT NULL DEFAULT 0                                    COMMENT '硬约束违例数',
    `warnings_json`          JSON           DEFAULT NULL                                          COMMENT '告警信息',
    `doctor_rules_snapshot`  JSON           DEFAULT NULL                                          COMMENT '快照：医生可排班规则',
    `time_off_snapshot`      JSON           DEFAULT NULL                                          COMMENT '快照：医生请假/停诊',
    `demand_snapshot`        JSON           DEFAULT NULL                                          COMMENT '快照：科室需求模板',
    `calendar_snapshot`      JSON           DEFAULT NULL                                          COMMENT '快照：节假日日历',
    `dsl_snapshot`           JSON           DEFAULT NULL                                          COMMENT '快照：约束DSL源文',
    `solver_meta`            JSON           DEFAULT NULL                                          COMMENT '求解元信息（seed, inputHash, dslHash, actualSolver, fallbackReason等）',
    `conflict_report`        JSON           DEFAULT NULL                                          COMMENT '无解诊断/冲突报告',
    `created_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`             DATETIME       DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_plan_code_ver` (`plan_code`, `version_no`),
    KEY `idx_schedule_plan_dept_range` (`department_id`, `start_date`, `end_date`, `plan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班方案主表';

-- ----- 排班方案明细（不变） -----
CREATE TABLE `schedule_plan_items` (
    `id`            BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `plan_id`       BIGINT        NOT NULL                                              COMMENT '方案ID',
    `schedule_date` DATE          NOT NULL                                              COMMENT '排班日期',
    `period_code`   TINYINT       NOT NULL                                              COMMENT '时段编码',
    `doctor_id`     BIGINT        NOT NULL                                              COMMENT '医生ID',
    `is_senior`     TINYINT       NOT NULL DEFAULT 0                                    COMMENT '是否资深医生',
    `reason_json`   JSON          DEFAULT NULL                                          COMMENT '分配原因',
    `penalty_json`  JSON          DEFAULT NULL                                          COMMENT '惩罚项',
    `score_delta`   DECIMAL(8, 2) DEFAULT NULL                                          COMMENT '分配增量分',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_slot_doctor` (`plan_id`, `schedule_date`, `period_code`, `doctor_id`),
    KEY `idx_plan_item_slot` (`plan_id`, `schedule_date`, `period_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班方案明细';

-- ----- 排班规则配置版本表（不变） -----
CREATE TABLE `schedule_rule_profile` (
    `id`                  BIGINT       NOT NULL                                              COMMENT '雪花ID',
    `department_id`       BIGINT       NOT NULL                                              COMMENT '科室ID',
    `profile_code`        VARCHAR(64)  NOT NULL                                              COMMENT '规则配置编码',
    `profile_name`        VARCHAR(128) NOT NULL                                              COMMENT '规则配置名称',
    `version_no`          INT          NOT NULL DEFAULT 1                                    COMMENT '版本号',
    `profile_status`      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT'                              COMMENT '状态 DRAFT/PUBLISHED/ARCHIVED',
    `constraint_dsl_json` JSON         NOT NULL                                              COMMENT 'JSON DSL 约束配置',
    `description`         VARCHAR(255) DEFAULT NULL                                          COMMENT '描述',
    `updated_by`          BIGINT       DEFAULT NULL                                          COMMENT '最近操作人',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`          DATETIME     DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_profile_code_ver` (`department_id`, `profile_code`, `version_no`),
    KEY `idx_rule_profile_status` (`department_id`, `profile_code`, `profile_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班规则配置版本表';
