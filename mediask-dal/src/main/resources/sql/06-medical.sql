-- ============================================================
-- 06-medical.sql  --  EMR and prescription (V3)
-- ============================================================

CREATE TABLE `drug_catalog` (
    `id`             BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `drug_code`      VARCHAR(64)    NOT NULL COMMENT 'Drug code',
    `drug_name`      VARCHAR(128)   NOT NULL COMMENT 'Drug name',
    `generic_name`   VARCHAR(128)   DEFAULT NULL COMMENT 'Generic name',
    `specification`  VARCHAR(128)   DEFAULT NULL COMMENT 'Specification',
    `unit`           VARCHAR(20)    DEFAULT NULL COMMENT 'Unit',
    `manufacturer`   VARCHAR(128)   DEFAULT NULL COMMENT 'Manufacturer',
    `unit_price`     DECIMAL(10, 2) DEFAULT NULL COMMENT 'Unit price',
    `drug_category`  VARCHAR(32)    DEFAULT NULL COMMENT 'Drug category',
    `status`         VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_drug_catalog_code` (`drug_code`),
    KEY `idx_drug_catalog_name` (`drug_name`),
    CONSTRAINT `chk_drug_catalog_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_drug_catalog_price` CHECK (`unit_price` IS NULL OR `unit_price` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Drug catalog';

CREATE TABLE `emr_record` (
    `id`                        BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `record_no`                 VARCHAR(32)  NOT NULL COMMENT 'EMR number',
    `encounter_id`              BIGINT       NOT NULL COMMENT 'Encounter ID',
    `patient_id`                BIGINT       NOT NULL COMMENT 'Patient user ID',
    `doctor_id`                 BIGINT       NOT NULL COMMENT 'Doctor ID',
    `department_id`             BIGINT       NOT NULL COMMENT 'Department ID',
    `record_status`             VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SIGNED/REVISED/ARCHIVED',
    `primary_diagnosis_summary` VARCHAR(500) DEFAULT NULL COMMENT 'Primary diagnosis summary',
    `signed_at`                 DATETIME     DEFAULT NULL COMMENT 'Signed at',
    `current_revision_no`       INT          NOT NULL DEFAULT 0 COMMENT 'Current revision number',
    `version`                   INT          NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emr_record_no` (`record_no`),
    UNIQUE KEY `uk_emr_record_encounter` (`encounter_id`),
    KEY `idx_emr_record_patient` (`patient_id`, `created_at`),
    CONSTRAINT `fk_emr_record_encounter` FOREIGN KEY (`encounter_id`) REFERENCES `visit_encounter` (`id`),
    CONSTRAINT `fk_emr_record_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_emr_record_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_emr_record_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_emr_record_status` CHECK (`record_status` IN ('DRAFT', 'SIGNED', 'REVISED', 'ARCHIVED')),
    CONSTRAINT `chk_emr_record_revision` CHECK (`current_revision_no` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='EMR index header';

CREATE TABLE `emr_record_content` (
    `id`                         BIGINT     NOT NULL COMMENT 'Snowflake ID',
    `record_id`                  BIGINT     NOT NULL COMMENT 'EMR record ID',
    `chief_complaint_encrypted`  MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted chief complaint',
    `present_illness_encrypted`  MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted present illness',
    `past_history_encrypted`     MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted past history',
    `physical_exam_encrypted`    MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted physical exam',
    `treatment_plan_encrypted`   MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted treatment plan',
    `ai_reference_summary`       VARCHAR(1000) DEFAULT NULL COMMENT 'AI reference summary',
    `content_hash`               VARCHAR(64) DEFAULT NULL COMMENT 'Content hash',
    `created_at`                 DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                 DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emr_record_content_record` (`record_id`),
    CONSTRAINT `fk_emr_record_content_record` FOREIGN KEY (`record_id`) REFERENCES `emr_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='EMR encrypted content';

CREATE TABLE `emr_record_revision` (
    `id`                 BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `record_id`          BIGINT       NOT NULL COMMENT 'EMR record ID',
    `revision_no`        INT          NOT NULL COMMENT 'Revision number',
    `revision_status`    VARCHAR(16)  NOT NULL COMMENT 'DRAFT/SIGNED/SUPERSEDED',
    `snapshot_encrypted` MEDIUMTEXT   NOT NULL COMMENT 'Encrypted snapshot',
    `change_reason`      VARCHAR(255) DEFAULT NULL COMMENT 'Change reason',
    `changed_by`         BIGINT       NOT NULL COMMENT 'Changed by user ID',
    `signed_by`          BIGINT       DEFAULT NULL COMMENT 'Signed by user ID',
    `signed_at`          DATETIME     DEFAULT NULL COMMENT 'Signed at',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emr_record_revision_no` (`record_id`, `revision_no`),
    KEY `idx_emr_record_revision_signed` (`record_id`, `signed_at`),
    CONSTRAINT `fk_emr_record_revision_record` FOREIGN KEY (`record_id`) REFERENCES `emr_record` (`id`),
    CONSTRAINT `fk_emr_record_revision_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_emr_record_revision_signed_by` FOREIGN KEY (`signed_by`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_emr_record_revision_status` CHECK (`revision_status` IN ('DRAFT', 'SIGNED', 'SUPERSEDED')),
    CONSTRAINT `chk_emr_record_revision_no` CHECK (`revision_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='EMR revision chain';

CREATE TABLE `emr_diagnosis` (
    `id`              BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `record_id`       BIGINT       NOT NULL COMMENT 'EMR record ID',
    `diagnosis_type`  VARCHAR(16)  NOT NULL COMMENT 'PRIMARY/SECONDARY/PROBLEM_LIST',
    `icd_code`        VARCHAR(32)  DEFAULT NULL COMMENT 'ICD code',
    `diagnosis_name`  VARCHAR(255) DEFAULT NULL COMMENT 'Masked diagnosis name',
    `diagnosis_name_encrypted` MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted diagnosis name',
    `is_primary`      TINYINT      NOT NULL DEFAULT 0 COMMENT 'Primary diagnosis flag',
    `sort_order`      INT          NOT NULL DEFAULT 0 COMMENT 'Sort order',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_emr_diagnosis_record` (`record_id`, `sort_order`),
    CONSTRAINT `fk_emr_diagnosis_record` FOREIGN KEY (`record_id`) REFERENCES `emr_record` (`id`),
    CONSTRAINT `chk_emr_diagnosis_type` CHECK (`diagnosis_type` IN ('PRIMARY', 'SECONDARY', 'PROBLEM_LIST')),
    CONSTRAINT `chk_emr_diagnosis_primary` CHECK (`is_primary` IN (0, 1)),
    CONSTRAINT `chk_emr_diagnosis_name` CHECK (`diagnosis_name` IS NOT NULL OR `diagnosis_name_encrypted` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Structured diagnosis';

CREATE TABLE `emr_observation` (
    `id`                 BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `record_id`          BIGINT       NOT NULL COMMENT 'EMR record ID',
    `observation_type`   VARCHAR(32)  NOT NULL COMMENT 'VITAL_SIGN/EXAM/LAB/NOTE',
    `observation_name`   VARCHAR(128) NOT NULL COMMENT 'Observation name',
    `observation_value`  VARCHAR(255) DEFAULT NULL COMMENT 'Masked observation value',
    `observation_value_encrypted` MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted observation value',
    `unit`               VARCHAR(32)  DEFAULT NULL COMMENT 'Unit',
    `sort_order`         INT          NOT NULL DEFAULT 0 COMMENT 'Sort order',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_emr_observation_record` (`record_id`, `sort_order`),
    CONSTRAINT `fk_emr_observation_record` FOREIGN KEY (`record_id`) REFERENCES `emr_record` (`id`),
    CONSTRAINT `chk_emr_observation_type` CHECK (`observation_type` IN ('VITAL_SIGN', 'EXAM', 'LAB', 'NOTE')),
    CONSTRAINT `chk_emr_observation_value` CHECK (`observation_value` IS NOT NULL OR `observation_value_encrypted` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='EMR structured observation';

CREATE TABLE `prescription_order` (
    `id`                   BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `prescription_no`      VARCHAR(32)    NOT NULL COMMENT 'Prescription number',
    `record_id`            BIGINT         NOT NULL COMMENT 'EMR record ID',
    `encounter_id`         BIGINT         NOT NULL COMMENT 'Encounter ID',
    `patient_id`           BIGINT         NOT NULL COMMENT 'Patient user ID',
    `doctor_id`            BIGINT         NOT NULL COMMENT 'Doctor ID',
    `prescription_type`    VARCHAR(16)    NOT NULL DEFAULT 'WESTERN' COMMENT 'WESTERN/CHINESE',
    `prescription_status`  VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SIGNED/DISPENSED/VOID',
    `total_amount`         DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Total amount',
    `notes`                VARCHAR(500)   DEFAULT NULL COMMENT 'Notes',
    `signed_at`            DATETIME       DEFAULT NULL COMMENT 'Signed at',
    `version`              INT            NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prescription_order_no` (`prescription_no`),
    KEY `idx_prescription_order_record` (`record_id`, `created_at`),
    CONSTRAINT `fk_prescription_order_record` FOREIGN KEY (`record_id`) REFERENCES `emr_record` (`id`),
    CONSTRAINT `fk_prescription_order_encounter` FOREIGN KEY (`encounter_id`) REFERENCES `visit_encounter` (`id`),
    CONSTRAINT `fk_prescription_order_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_prescription_order_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `chk_prescription_order_type` CHECK (`prescription_type` IN ('WESTERN', 'CHINESE')),
    CONSTRAINT `chk_prescription_order_status` CHECK (`prescription_status` IN ('DRAFT', 'SIGNED', 'DISPENSED', 'VOID')),
    CONSTRAINT `chk_prescription_order_amount` CHECK (`total_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prescription order';

CREATE TABLE `prescription_item` (
    `id`                      BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `prescription_id`         BIGINT         NOT NULL COMMENT 'Prescription ID',
    `drug_id`                 BIGINT         DEFAULT NULL COMMENT 'Drug catalog ID',
    `drug_name_snapshot`      VARCHAR(128)   NOT NULL COMMENT 'Drug name snapshot',
    `specification_snapshot`  VARCHAR(128)   DEFAULT NULL COMMENT 'Specification snapshot',
    `dosage`                  VARCHAR(64)    DEFAULT NULL COMMENT 'Dosage',
    `frequency`               VARCHAR(64)    DEFAULT NULL COMMENT 'Frequency',
    `route`                   VARCHAR(64)    DEFAULT NULL COMMENT 'Route',
    `duration_days`           INT            DEFAULT NULL COMMENT 'Duration days',
    `quantity`                INT            NOT NULL DEFAULT 1 COMMENT 'Quantity',
    `unit_price`              DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Unit price snapshot',
    `subtotal`                DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Subtotal',
    `created_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_prescription_item_prescription` (`prescription_id`),
    KEY `idx_prescription_item_drug` (`drug_id`),
    CONSTRAINT `fk_prescription_item_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription_order` (`id`),
    CONSTRAINT `fk_prescription_item_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_catalog` (`id`),
    CONSTRAINT `chk_prescription_item_days` CHECK (`duration_days` IS NULL OR `duration_days` >= 0),
    CONSTRAINT `chk_prescription_item_amount` CHECK (`quantity` > 0 AND `unit_price` >= 0 AND `subtotal` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prescription item';

CREATE TABLE `prescription_snapshot` (
    `id`                 BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `prescription_id`    BIGINT      NOT NULL COMMENT 'Prescription ID',
    `snapshot_type`      VARCHAR(16) NOT NULL COMMENT 'SIGN/DISPENSE/VOID',
    `snapshot_encrypted` MEDIUMTEXT  NOT NULL COMMENT 'Encrypted snapshot',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_prescription_snapshot_prescription` (`prescription_id`, `snapshot_type`),
    CONSTRAINT `fk_prescription_snapshot_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription_order` (`id`),
    CONSTRAINT `chk_prescription_snapshot_type` CHECK (`snapshot_type` IN ('SIGN', 'DISPENSE', 'VOID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prescription immutable snapshot';

CREATE TABLE `medication_dispense` (
    `id`               BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `prescription_id`  BIGINT       NOT NULL COMMENT 'Prescription ID',
    `dispense_status`  VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DISPENSED/RETURNED',
    `dispensed_by`     BIGINT       DEFAULT NULL COMMENT 'Dispensed by user ID',
    `dispensed_at`     DATETIME     DEFAULT NULL COMMENT 'Dispensed at',
    `remark`           VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_medication_dispense_prescription` (`prescription_id`),
    CONSTRAINT `fk_medication_dispense_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescription_order` (`id`),
    CONSTRAINT `fk_medication_dispense_dispensed_by` FOREIGN KEY (`dispensed_by`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_medication_dispense_status` CHECK (`dispense_status` IN ('PENDING', 'DISPENSED', 'RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Medication dispense record';
