-- ============================================================
-- 02-hospital-org.sql  --  Organization and doctor roster (V3)
-- ============================================================

CREATE TABLE `hospitals` (
    `id`             BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `hospital_name`  VARCHAR(128) NOT NULL COMMENT 'Hospital name',
    `hospital_code`  VARCHAR(64)  NOT NULL COMMENT 'Hospital code',
    `hospital_level` VARCHAR(32)  DEFAULT NULL COMMENT 'Hospital level',
    `address`        VARCHAR(255) DEFAULT NULL COMMENT 'Address',
    `contact_phone`  VARCHAR(20)  DEFAULT NULL COMMENT 'Contact phone',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hospitals_code` (`hospital_code`),
    CONSTRAINT `chk_hospitals_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hospital table';

CREATE TABLE `departments` (
    `id`             BIGINT        NOT NULL COMMENT 'Snowflake ID',
    `hospital_id`    BIGINT        NOT NULL COMMENT 'Hospital ID',
    `dept_code`      VARCHAR(64)   NOT NULL COMMENT 'Department code',
    `dept_name`      VARCHAR(128)  NOT NULL COMMENT 'Department name',
    `dept_type`      VARCHAR(20)   NOT NULL DEFAULT 'OUTPATIENT' COMMENT 'OUTPATIENT/INPATIENT/SHARED',
    `dept_intro`     VARCHAR(1000) DEFAULT NULL COMMENT 'Department introduction',
    `display_order`  INT           NOT NULL DEFAULT 0 COMMENT 'Display order',
    `status`         VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_departments_hospital_code` (`hospital_id`, `dept_code`),
    KEY `idx_departments_hospital` (`hospital_id`),
    CONSTRAINT `fk_departments_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospitals` (`id`),
    CONSTRAINT `chk_departments_type` CHECK (`dept_type` IN ('OUTPATIENT', 'INPATIENT', 'SHARED')),
    CONSTRAINT `chk_departments_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Department table';

CREATE TABLE `doctors` (
    `id`                         BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `user_id`                    BIGINT         NOT NULL COMMENT 'User ID',
    `hospital_id`                BIGINT         NOT NULL COMMENT 'Primary hospital ID',
    `doctor_code`                VARCHAR(64)    NOT NULL COMMENT 'Doctor code',
    `title`                      VARCHAR(64)    DEFAULT NULL COMMENT 'Professional title',
    `specialty_summary`          VARCHAR(1000)  DEFAULT NULL COMMENT 'Specialty summary',
    `introduction`               VARCHAR(2000)  DEFAULT NULL COMMENT 'Profile intro',
    `license_number_encrypted`   VARCHAR(255)   DEFAULT NULL COMMENT 'Encrypted license number',
    `default_consultation_fee`   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Default consultation fee',
    `doctor_status`              VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/SUSPENDED',
    `created_at`                 DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                 DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctors_user` (`user_id`),
    UNIQUE KEY `uk_doctors_code` (`doctor_code`),
    KEY `idx_doctors_hospital` (`hospital_id`),
    CONSTRAINT `fk_doctors_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_doctors_hospital` FOREIGN KEY (`hospital_id`) REFERENCES `hospitals` (`id`),
    CONSTRAINT `chk_doctors_status` CHECK (`doctor_status` IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT `chk_doctors_fee` CHECK (`default_consultation_fee` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Doctor master profile';

CREATE TABLE `doctor_department_rel` (
    `id`            BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `doctor_id`     BIGINT      NOT NULL COMMENT 'Doctor ID',
    `department_id` BIGINT      NOT NULL COMMENT 'Department ID',
    `is_primary`    TINYINT     NOT NULL DEFAULT 0 COMMENT 'Primary department flag',
    `can_schedule`  TINYINT     NOT NULL DEFAULT 1 COMMENT 'Can be scheduled',
    `valid_from`    DATE        DEFAULT NULL COMMENT 'Valid from date',
    `valid_until`   DATE        DEFAULT NULL COMMENT 'Valid until date',
    `status`        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctor_department_rel` (`doctor_id`, `department_id`, `valid_from`),
    KEY `idx_doctor_department_department` (`department_id`, `status`),
    CONSTRAINT `fk_doctor_department_rel_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_doctor_department_rel_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_doctor_department_rel_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_doctor_department_rel_time` CHECK (`valid_until` IS NULL OR `valid_from` IS NULL OR `valid_until` >= `valid_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Doctor department relation';
