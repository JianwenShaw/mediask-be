-- ============================================================
-- 03-scheduling.sql  --  Scheduling planning domain (V3)
-- ============================================================

CREATE TABLE `schedule_ruleset` (
    `id`             BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `department_id`  BIGINT       NOT NULL COMMENT 'Department ID',
    `ruleset_code`   VARCHAR(64)  NOT NULL COMMENT 'Ruleset code',
    `ruleset_name`   VARCHAR(128) NOT NULL COMMENT 'Ruleset name',
    `version_no`     INT          NOT NULL DEFAULT 1 COMMENT 'Version number',
    `ruleset_status` VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
    `description`    VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `published_at`   DATETIME     DEFAULT NULL COMMENT 'Published at',
    `created_by`     BIGINT       DEFAULT NULL COMMENT 'Created by user',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_ruleset_code_version` (`department_id`, `ruleset_code`, `version_no`),
    KEY `idx_schedule_ruleset_status` (`department_id`, `ruleset_status`),
    CONSTRAINT `fk_schedule_ruleset_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `fk_schedule_ruleset_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_schedule_ruleset_status` CHECK (`ruleset_status` IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling ruleset header';

CREATE TABLE `schedule_ruleset_item` (
    `id`                 BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `ruleset_id`         BIGINT       NOT NULL COMMENT 'Ruleset ID',
    `rule_code`          VARCHAR(64)  NOT NULL COMMENT 'Rule code',
    `rule_type`          VARCHAR(64)  NOT NULL COMMENT 'Rule type',
    `rule_scope`         VARCHAR(32)  NOT NULL COMMENT 'GLOBAL/DEPARTMENT/DOCTOR',
    `is_hard_constraint` TINYINT      NOT NULL DEFAULT 1 COMMENT 'Hard constraint flag',
    `priority`           INT          NOT NULL DEFAULT 0 COMMENT 'Priority',
    `weight`             DECIMAL(8,2) NOT NULL DEFAULT 1.00 COMMENT 'Score weight',
    `rule_expr_json`     JSON         NOT NULL COMMENT 'Rule expression',
    `description`        VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `status`             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_ruleset_item_code` (`ruleset_id`, `rule_code`),
    KEY `idx_schedule_ruleset_item_type` (`ruleset_id`, `rule_type`, `status`),
    CONSTRAINT `fk_schedule_ruleset_item_ruleset` FOREIGN KEY (`ruleset_id`) REFERENCES `schedule_ruleset` (`id`),
    CONSTRAINT `chk_schedule_ruleset_item_scope` CHECK (`rule_scope` IN ('GLOBAL', 'DEPARTMENT', 'DOCTOR')),
    CONSTRAINT `chk_schedule_ruleset_item_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_schedule_ruleset_item_weight` CHECK (`weight` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling ruleset item';

CREATE TABLE `doctor_availability_rule` (
    `id`              BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `doctor_id`       BIGINT      NOT NULL COMMENT 'Doctor ID',
    `department_id`   BIGINT      NOT NULL COMMENT 'Department ID',
    `weekday`         TINYINT     NOT NULL COMMENT '1-7 for Monday-Sunday',
    `period_code`     TINYINT     NOT NULL COMMENT '1 morning 2 afternoon 3 evening',
    `clinic_type`     VARCHAR(20) NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/SPECIAL/EXPERT',
    `is_available`    TINYINT     NOT NULL DEFAULT 1 COMMENT 'Availability flag',
    `priority`        INT         NOT NULL DEFAULT 0 COMMENT 'Preference priority',
    `effective_from`  DATE        NOT NULL DEFAULT '1970-01-01' COMMENT 'Effective from date',
    `effective_until` DATE        NOT NULL DEFAULT '9999-12-31' COMMENT 'Effective until date',
    `status`          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctor_availability_rule` (`doctor_id`, `department_id`, `weekday`, `period_code`, `clinic_type`, `effective_from`),
    KEY `idx_doctor_availability_department` (`department_id`, `weekday`, `period_code`, `status`),
    CONSTRAINT `fk_doctor_availability_rule_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_doctor_availability_rule_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_doctor_availability_rule_weekday` CHECK (`weekday` BETWEEN 1 AND 7),
    CONSTRAINT `chk_doctor_availability_rule_period` CHECK (`period_code` BETWEEN 1 AND 3),
    CONSTRAINT `chk_doctor_availability_rule_type` CHECK (`clinic_type` IN ('GENERAL', 'SPECIAL', 'EXPERT')),
    CONSTRAINT `chk_doctor_availability_rule_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_doctor_availability_rule_time` CHECK (`effective_until` IS NULL OR `effective_from` IS NULL OR `effective_until` >= `effective_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Doctor availability rule';

CREATE TABLE `doctor_unavailability` (
    `id`               BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `doctor_id`        BIGINT       NOT NULL COMMENT 'Doctor ID',
    `department_id`    BIGINT       DEFAULT NULL COMMENT 'Department ID',
    `unavailable_type` VARCHAR(20)  NOT NULL COMMENT 'LEAVE/TRAINING/MEETING/CLOSE_CLINIC/MANUAL_BLOCK',
    `start_datetime`   DATETIME     NOT NULL COMMENT 'Start datetime',
    `end_datetime`     DATETIME     NOT NULL COMMENT 'End datetime',
    `reason`           VARCHAR(255) DEFAULT NULL COMMENT 'Reason',
    `source_type`      VARCHAR(32)  DEFAULT NULL COMMENT 'Source type',
    `source_ref_id`    BIGINT       DEFAULT NULL COMMENT 'Source business ID',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_doctor_unavailability_doctor` (`doctor_id`, `start_datetime`, `end_datetime`, `status`),
    KEY `idx_doctor_unavailability_department` (`department_id`, `start_datetime`, `end_datetime`),
    CONSTRAINT `fk_doctor_unavailability_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_doctor_unavailability_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_doctor_unavailability_type` CHECK (`unavailable_type` IN ('LEAVE', 'TRAINING', 'MEETING', 'CLOSE_CLINIC', 'MANUAL_BLOCK')),
    CONSTRAINT `chk_doctor_unavailability_status` CHECK (`status` IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT `chk_doctor_unavailability_time` CHECK (`end_datetime` > `start_datetime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Doctor unavailable window';

CREATE TABLE `calendar_day` (
    `id`                BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `calendar_date`     DATE        NOT NULL COMMENT 'Calendar date',
    `region_code`       VARCHAR(32) NOT NULL DEFAULT 'CN-NATIONAL' COMMENT 'Region code',
    `day_type`          VARCHAR(20) NOT NULL COMMENT 'WORKDAY/WEEKEND/HOLIDAY/MAKEUP_WORKDAY',
    `is_holiday`        TINYINT     NOT NULL DEFAULT 0 COMMENT 'Holiday flag',
    `is_makeup_workday` TINYINT     NOT NULL DEFAULT 0 COMMENT 'Makeup workday flag',
    `holiday_name`      VARCHAR(64) DEFAULT NULL COMMENT 'Holiday name',
    `status`            VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_calendar_day_region` (`calendar_date`, `region_code`),
    CONSTRAINT `chk_calendar_day_type` CHECK (`day_type` IN ('WORKDAY', 'WEEKEND', 'HOLIDAY', 'MAKEUP_WORKDAY')),
    CONSTRAINT `chk_calendar_day_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_calendar_day_flags` CHECK (
        (`day_type` = 'HOLIDAY' AND `is_holiday` = 1 AND `is_makeup_workday` = 0)
        OR (`day_type` = 'MAKEUP_WORKDAY' AND `is_holiday` = 0 AND `is_makeup_workday` = 1)
        OR (`day_type` = 'WORKDAY' AND `is_holiday` = 0 AND `is_makeup_workday` = 0)
        OR (`day_type` = 'WEEKEND' AND `is_holiday` = 0 AND `is_makeup_workday` = 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Calendar day table';

CREATE TABLE `schedule_demand_template` (
    `id`                        BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `department_id`             BIGINT       NOT NULL COMMENT 'Department ID',
    `weekday`                   TINYINT      NOT NULL COMMENT '1-7',
    `period_code`               TINYINT      NOT NULL COMMENT '1 morning 2 afternoon 3 evening',
    `clinic_type`               VARCHAR(20)  NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/SPECIAL/EXPERT',
    `required_doctor_count`     INT          NOT NULL COMMENT 'Required doctor count',
    `required_senior_count`     INT          NOT NULL DEFAULT 0 COMMENT 'Required senior doctor count',
    `suggested_slot_count`      INT          NOT NULL DEFAULT 0 COMMENT 'Suggested slot count',
    `min_slot_interval_minutes` INT          NOT NULL DEFAULT 15 COMMENT 'Suggested slot interval',
    `effective_from`            DATE         NOT NULL DEFAULT '1970-01-01' COMMENT 'Effective from date',
    `effective_until`           DATE         NOT NULL DEFAULT '9999-12-31' COMMENT 'Effective until date',
    `status`                    VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_demand_template` (`department_id`, `weekday`, `period_code`, `clinic_type`, `effective_from`),
    CONSTRAINT `fk_schedule_demand_template_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_schedule_demand_template_weekday` CHECK (`weekday` BETWEEN 1 AND 7),
    CONSTRAINT `chk_schedule_demand_template_period` CHECK (`period_code` BETWEEN 1 AND 3),
    CONSTRAINT `chk_schedule_demand_template_type` CHECK (`clinic_type` IN ('GENERAL', 'SPECIAL', 'EXPERT')),
    CONSTRAINT `chk_schedule_demand_template_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_schedule_demand_template_count` CHECK (`required_doctor_count` >= 0 AND `required_senior_count` >= 0 AND `suggested_slot_count` >= 0),
    CONSTRAINT `chk_schedule_demand_template_interval` CHECK (`min_slot_interval_minutes` > 0),
    CONSTRAINT `chk_schedule_demand_template_time` CHECK (`effective_until` IS NULL OR `effective_from` IS NULL OR `effective_until` >= `effective_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling demand template';

CREATE TABLE `schedule_demand_override` (
    `id`                        BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `department_id`             BIGINT       NOT NULL COMMENT 'Department ID',
    `target_date`               DATE         NOT NULL COMMENT 'Target date',
    `period_code`               TINYINT      NOT NULL COMMENT '1 morning 2 afternoon 3 evening',
    `clinic_type`               VARCHAR(20)  NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/SPECIAL/EXPERT',
    `required_doctor_count`     INT          NOT NULL COMMENT 'Required doctor count',
    `required_senior_count`     INT          NOT NULL DEFAULT 0 COMMENT 'Required senior doctor count',
    `suggested_slot_count`      INT          NOT NULL DEFAULT 0 COMMENT 'Suggested slot count',
    `override_reason`           VARCHAR(255) DEFAULT NULL COMMENT 'Override reason',
    `priority`                  INT          NOT NULL DEFAULT 0 COMMENT 'Priority',
    `status`                    VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_demand_override` (`department_id`, `target_date`, `period_code`, `clinic_type`),
    CONSTRAINT `fk_schedule_demand_override_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_schedule_demand_override_period` CHECK (`period_code` BETWEEN 1 AND 3),
    CONSTRAINT `chk_schedule_demand_override_type` CHECK (`clinic_type` IN ('GENERAL', 'SPECIAL', 'EXPERT')),
    CONSTRAINT `chk_schedule_demand_override_status` CHECK (`status` IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT `chk_schedule_demand_override_count` CHECK (`required_doctor_count` >= 0 AND `required_senior_count` >= 0 AND `suggested_slot_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling demand override';

CREATE TABLE `schedule_generation_job` (
    `id`                  BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `department_id`       BIGINT       NOT NULL COMMENT 'Department ID',
    `ruleset_id`          BIGINT       NOT NULL COMMENT 'Ruleset ID',
    `job_type`            VARCHAR(16)  NOT NULL DEFAULT 'FORMAL' COMMENT 'TRIAL/FORMAL/REGENERATE',
    `start_date`          DATE         NOT NULL COMMENT 'Planning start date',
    `end_date`            DATE         NOT NULL COMMENT 'Planning end date',
    `solver_strategy`     VARCHAR(32)  DEFAULT NULL COMMENT 'Solver strategy',
    `job_status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED',
    `submitted_by`        BIGINT       DEFAULT NULL COMMENT 'Submitted by user',
    `submitted_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submitted at',
    `started_at`          DATETIME     DEFAULT NULL COMMENT 'Started at',
    `finished_at`         DATETIME     DEFAULT NULL COMMENT 'Finished at',
    `fail_reason`         VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
    `input_snapshot_hash` VARCHAR(64)  DEFAULT NULL COMMENT 'Input snapshot hash',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_schedule_generation_job_status` (`department_id`, `start_date`, `end_date`, `job_status`),
    CONSTRAINT `fk_schedule_generation_job_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `fk_schedule_generation_job_ruleset` FOREIGN KEY (`ruleset_id`) REFERENCES `schedule_ruleset` (`id`),
    CONSTRAINT `fk_schedule_generation_job_submitted_by` FOREIGN KEY (`submitted_by`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_schedule_generation_job_type` CHECK (`job_type` IN ('TRIAL', 'FORMAL', 'REGENERATE')),
    CONSTRAINT `chk_schedule_generation_job_status` CHECK (`job_status` IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT `chk_schedule_generation_job_time` CHECK (`end_date` >= `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling generation job';

CREATE TABLE `schedule_generation_result` (
    `id`                   BIGINT        NOT NULL COMMENT 'Snowflake ID',
    `job_id`               BIGINT        NOT NULL COMMENT 'Job ID',
    `result_no`            INT           NOT NULL DEFAULT 1 COMMENT 'Result ordinal',
    `result_status`        VARCHAR(16)   NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/SELECTED/PUBLISHED/DISCARDED',
    `score`                DECIMAL(10,2) DEFAULT NULL COMMENT 'Total score',
    `hard_violation_count` INT           NOT NULL DEFAULT 0 COMMENT 'Hard violation count',
    `soft_violation_score` DECIMAL(10,2) DEFAULT NULL COMMENT 'Soft violation score',
    `warning_count`        INT           NOT NULL DEFAULT 0 COMMENT 'Warning count',
    `summary_json`         JSON          DEFAULT NULL COMMENT 'Summary payload',
    `diagnostics_json`     JSON          DEFAULT NULL COMMENT 'Diagnostics payload',
    `is_selected`          TINYINT       NOT NULL DEFAULT 0 COMMENT 'Selected result flag',
    `selected_job_id`      BIGINT GENERATED ALWAYS AS (CASE WHEN `result_status` = 'SELECTED' THEN `job_id` ELSE NULL END) STORED COMMENT 'Selected result helper',
    `published_job_id`     BIGINT GENERATED ALWAYS AS (CASE WHEN `result_status` = 'PUBLISHED' THEN `job_id` ELSE NULL END) STORED COMMENT 'Published result helper',
    `published_at`         DATETIME      DEFAULT NULL COMMENT 'Published at',
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_generation_result_no` (`job_id`, `result_no`),
    UNIQUE KEY `uk_schedule_generation_result_selected` (`selected_job_id`),
    UNIQUE KEY `uk_schedule_generation_result_published` (`published_job_id`),
    KEY `idx_schedule_generation_result_status` (`job_id`, `result_status`, `score`),
    CONSTRAINT `fk_schedule_generation_result_job` FOREIGN KEY (`job_id`) REFERENCES `schedule_generation_job` (`id`),
    CONSTRAINT `chk_schedule_generation_result_status` CHECK (`result_status` IN ('GENERATED', 'SELECTED', 'PUBLISHED', 'DISCARDED')),
    CONSTRAINT `chk_schedule_generation_result_selected` CHECK (
        (`result_status` IN ('SELECTED', 'PUBLISHED') AND `is_selected` = 1)
        OR (`result_status` IN ('GENERATED', 'DISCARDED') AND `is_selected` = 0)
    ),
    CONSTRAINT `chk_schedule_generation_result_published_at` CHECK (
        (`result_status` = 'PUBLISHED' AND `published_at` IS NOT NULL)
        OR (`result_status` <> 'PUBLISHED' AND `published_at` IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling generation result';

CREATE TABLE `schedule_generation_assignment` (
    `id`                   BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `result_id`            BIGINT         NOT NULL COMMENT 'Result ID',
    `doctor_id`            BIGINT         NOT NULL COMMENT 'Doctor ID',
    `department_id`        BIGINT         NOT NULL COMMENT 'Department ID',
    `schedule_date`        DATE           NOT NULL COMMENT 'Schedule date',
    `period_code`          TINYINT        NOT NULL COMMENT '1 morning 2 afternoon 3 evening',
    `clinic_type`          VARCHAR(20)    NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/SPECIAL/EXPERT',
    `is_senior_slot`       TINYINT        NOT NULL DEFAULT 0 COMMENT 'Senior doctor slot',
    `suggested_start_time` TIME           NOT NULL COMMENT 'Suggested start time',
    `suggested_end_time`   TIME           NOT NULL COMMENT 'Suggested end time',
    `suggested_fee`        DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Suggested fee',
    `suggested_capacity`   INT            NOT NULL DEFAULT 0 COMMENT 'Suggested slot count',
    `assignment_status`    VARCHAR(16)    NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/SKIPPED/PUBLISHED',
    `reason_json`          JSON           DEFAULT NULL COMMENT 'Allocation reasons',
    `score_delta`          DECIMAL(10,2)  DEFAULT NULL COMMENT 'Score delta',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_generation_assignment` (`result_id`, `doctor_id`, `department_id`, `schedule_date`, `period_code`, `clinic_type`),
    UNIQUE KEY `uk_schedule_generation_assignment_doctor_period` (`result_id`, `doctor_id`, `schedule_date`, `period_code`),
    KEY `idx_schedule_generation_assignment_department` (`department_id`, `schedule_date`, `period_code`),
    KEY `idx_schedule_generation_assignment_doctor` (`doctor_id`, `schedule_date`),
    CONSTRAINT `fk_schedule_generation_assignment_result` FOREIGN KEY (`result_id`) REFERENCES `schedule_generation_result` (`id`),
    CONSTRAINT `fk_schedule_generation_assignment_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_schedule_generation_assignment_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_schedule_generation_assignment_period` CHECK (`period_code` BETWEEN 1 AND 3),
    CONSTRAINT `chk_schedule_generation_assignment_type` CHECK (`clinic_type` IN ('GENERAL', 'SPECIAL', 'EXPERT')),
    CONSTRAINT `chk_schedule_generation_assignment_status` CHECK (`assignment_status` IN ('PLANNED', 'SKIPPED', 'PUBLISHED')),
    CONSTRAINT `chk_schedule_generation_assignment_time` CHECK (`suggested_end_time` > `suggested_start_time`),
    CONSTRAINT `chk_schedule_generation_assignment_capacity` CHECK (`suggested_capacity` >= 0 AND `suggested_fee` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scheduling assignment result';
