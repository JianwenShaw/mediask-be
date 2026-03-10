-- ============================================================
-- 04-appointment.sql  --  Clinic session and registration (V3)
-- ============================================================

CREATE TABLE `clinic_session` (
    `id`                   BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `department_id`        BIGINT         NOT NULL COMMENT 'Department ID',
    `doctor_id`            BIGINT         NOT NULL COMMENT 'Doctor ID',
    `source_assignment_id` BIGINT         DEFAULT NULL COMMENT 'Source schedule assignment ID',
    `session_date`         DATE           NOT NULL COMMENT 'Session date',
    `period_code`          TINYINT        NOT NULL COMMENT '1 morning 2 afternoon 3 evening',
    `clinic_type`          VARCHAR(20)    NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL/SPECIAL/EXPERT',
    `start_time`           TIME           NOT NULL COMMENT 'Session start time',
    `end_time`             TIME           NOT NULL COMMENT 'Session end time',
    `fee`                  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Session fee',
    `capacity`             INT            NOT NULL DEFAULT 0 COMMENT 'Total slot count',
    `remaining_count`      INT            NOT NULL DEFAULT 0 COMMENT 'Derived remaining count snapshot',
    `session_status`       VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OPEN/CLOSED/CANCELLED',
    `allow_registration`   TINYINT        NOT NULL DEFAULT 0 COMMENT 'Registration open flag',
    `allow_cancellation`   TINYINT        NOT NULL DEFAULT 1 COMMENT 'Cancellation allowed flag',
    `published_at`         DATETIME       DEFAULT NULL COMMENT 'Published at',
    `cancelled_reason`     VARCHAR(255)   DEFAULT NULL COMMENT 'Cancellation reason',
    `version`              INT            NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clinic_session_unique` (`doctor_id`, `department_id`, `session_date`, `period_code`, `clinic_type`),
    UNIQUE KEY `uk_clinic_session_doctor_period` (`doctor_id`, `session_date`, `period_code`),
    KEY `idx_clinic_session_department_date` (`department_id`, `session_date`, `session_status`),
    CONSTRAINT `fk_clinic_session_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `fk_clinic_session_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_clinic_session_assignment` FOREIGN KEY (`source_assignment_id`) REFERENCES `schedule_generation_assignment` (`id`),
    CONSTRAINT `chk_clinic_session_period` CHECK (`period_code` BETWEEN 1 AND 3),
    CONSTRAINT `chk_clinic_session_type` CHECK (`clinic_type` IN ('GENERAL', 'SPECIAL', 'EXPERT')),
    CONSTRAINT `chk_clinic_session_status` CHECK (`session_status` IN ('DRAFT', 'PUBLISHED', 'OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT `chk_clinic_session_time` CHECK (`end_time` > `start_time`),
    CONSTRAINT `chk_clinic_session_capacity` CHECK (`capacity` >= 0 AND `remaining_count` >= 0 AND `remaining_count` <= `capacity` AND `fee` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Published clinic session';

CREATE TABLE `clinic_slot` (
    `id`                BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `session_id`        BIGINT      NOT NULL COMMENT 'Clinic session ID',
    `slot_seq`          INT         NOT NULL COMMENT 'Slot sequence in session',
    `slot_start_time`   TIME        NOT NULL COMMENT 'Slot start time',
    `slot_end_time`     TIME        NOT NULL COMMENT 'Slot end time',
    `slot_status`       VARCHAR(16) NOT NULL DEFAULT 'FREE' COMMENT 'FREE/HELD/BOOKED/CANCELLED/USED',
    `current_hold_id`   BIGINT      DEFAULT NULL COMMENT 'Current active hold ID',
    `current_order_id`  BIGINT      DEFAULT NULL COMMENT 'Current active order ID',
    `version`           INT         NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clinic_slot_seq` (`session_id`, `slot_seq`),
    UNIQUE KEY `uk_clinic_slot_time` (`session_id`, `slot_start_time`),
    KEY `idx_clinic_slot_status` (`session_id`, `slot_status`),
    UNIQUE KEY `uk_clinic_slot_id_session` (`id`, `session_id`),
    CONSTRAINT `fk_clinic_slot_session` FOREIGN KEY (`session_id`) REFERENCES `clinic_session` (`id`),
    CONSTRAINT `chk_clinic_slot_status` CHECK (`slot_status` IN ('FREE', 'HELD', 'BOOKED', 'CANCELLED', 'USED')),
    CONSTRAINT `chk_clinic_slot_seq` CHECK (`slot_seq > 0`),
    CONSTRAINT `chk_clinic_slot_time` CHECK (`slot_end_time` > `slot_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Clinic slot inventory';

CREATE TABLE `registration_hold` (
    `id`             BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `hold_no`        VARCHAR(32) NOT NULL COMMENT 'Hold number',
    `slot_id`        BIGINT      NOT NULL COMMENT 'Slot ID',
    `patient_id`     BIGINT      NOT NULL COMMENT 'Patient user ID',
    `hold_status`    VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RELEASED/EXPIRED/CONFIRMED',
    `expire_at`      DATETIME    NOT NULL COMMENT 'Hold expiration time',
    `released_at`    DATETIME    DEFAULT NULL COMMENT 'Released at',
    `release_reason` VARCHAR(255) DEFAULT NULL COMMENT 'Release reason',
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration_hold_no` (`hold_no`),
    KEY `idx_registration_hold_slot` (`slot_id`, `hold_status`, `expire_at`),
    KEY `idx_registration_hold_expire` (`hold_status`, `expire_at`, `id`),
    KEY `idx_registration_hold_patient` (`patient_id`, `created_at`),
    CONSTRAINT `fk_registration_hold_slot` FOREIGN KEY (`slot_id`) REFERENCES `clinic_slot` (`id`),
    CONSTRAINT `fk_registration_hold_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_registration_hold_status` CHECK (`hold_status` IN ('ACTIVE', 'RELEASED', 'EXPIRED', 'CONFIRMED')),
    CONSTRAINT `chk_registration_hold_expire` CHECK (`expire_at` > `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pre-payment slot hold';

CREATE TABLE `registration_order` (
    `id`                       BIGINT         NOT NULL COMMENT 'Snowflake ID',
    `order_no`                 VARCHAR(32)    NOT NULL COMMENT 'Registration order number',
    `patient_id`               BIGINT         NOT NULL COMMENT 'Patient user ID',
    `doctor_id`                BIGINT         NOT NULL COMMENT 'Doctor ID',
    `department_id`            BIGINT         NOT NULL COMMENT 'Department ID',
    `session_id`               BIGINT         NOT NULL COMMENT 'Clinic session ID',
    `slot_id`                  BIGINT         NOT NULL COMMENT 'Clinic slot ID',
    `hold_id`                  BIGINT         DEFAULT NULL COMMENT 'Registration hold ID',
    `source_ai_session_id`     BIGINT         DEFAULT NULL COMMENT 'Related AI session ID',
    `order_status`             VARCHAR(16)    NOT NULL DEFAULT 'INIT' COMMENT 'INIT/HELD/CONFIRMED/CANCELLED/VISITED/NO_SHOW/REFUNDED',
    `chief_complaint_summary`  VARCHAR(500)   DEFAULT NULL COMMENT 'Chief complaint summary',
    `fee_amount`               DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Fee amount',
    `paid_at`                  DATETIME       DEFAULT NULL COMMENT 'Paid at',
    `cancelled_at`             DATETIME       DEFAULT NULL COMMENT 'Cancelled at',
    `visited_at`               DATETIME       DEFAULT NULL COMMENT 'Visited at',
    `version`                  INT            NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration_order_no` (`order_no`),
    KEY `idx_registration_order_patient` (`patient_id`, `created_at`),
    KEY `idx_registration_order_session` (`session_id`, `order_status`),
    KEY `idx_registration_order_slot` (`slot_id`, `order_status`),
    CONSTRAINT `fk_registration_order_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_registration_order_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_registration_order_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `fk_registration_order_session` FOREIGN KEY (`session_id`) REFERENCES `clinic_session` (`id`),
    CONSTRAINT `fk_registration_order_slot` FOREIGN KEY (`slot_id`) REFERENCES `clinic_slot` (`id`),
    CONSTRAINT `fk_registration_order_slot_session` FOREIGN KEY (`slot_id`, `session_id`) REFERENCES `clinic_slot` (`id`, `session_id`),
    CONSTRAINT `fk_registration_order_hold` FOREIGN KEY (`hold_id`) REFERENCES `registration_hold` (`id`),
    CONSTRAINT `chk_registration_order_status` CHECK (`order_status` IN ('INIT', 'HELD', 'CONFIRMED', 'CANCELLED', 'VISITED', 'NO_SHOW', 'REFUNDED')),
    CONSTRAINT `chk_registration_order_fee` CHECK (`fee_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Registration order';

CREATE TABLE `slot_active_claim` (
    `slot_id`         BIGINT      NOT NULL COMMENT 'Slot ID',
    `hold_id`         BIGINT      DEFAULT NULL COMMENT 'Active hold ID',
    `order_id`        BIGINT      DEFAULT NULL COMMENT 'Active order ID',
    `claim_status`    VARCHAR(16) NOT NULL COMMENT 'HELD/BOOKED',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`slot_id`),
    UNIQUE KEY `uk_slot_active_claim_hold` (`hold_id`),
    UNIQUE KEY `uk_slot_active_claim_order` (`order_id`),
    CONSTRAINT `fk_slot_active_claim_slot` FOREIGN KEY (`slot_id`) REFERENCES `clinic_slot` (`id`),
    CONSTRAINT `fk_slot_active_claim_hold` FOREIGN KEY (`hold_id`) REFERENCES `registration_hold` (`id`),
    CONSTRAINT `fk_slot_active_claim_order` FOREIGN KEY (`order_id`) REFERENCES `registration_order` (`id`),
    CONSTRAINT `chk_slot_active_claim_status` CHECK (`claim_status` IN ('HELD', 'BOOKED')),
    CONSTRAINT `chk_slot_active_claim_holder` CHECK (
        (`claim_status` = 'HELD' AND `hold_id` IS NOT NULL AND `order_id` IS NULL)
        OR (`claim_status` = 'BOOKED' AND `hold_id` IS NULL AND `order_id` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Single active claim per slot';

CREATE TABLE `registration_status_log` (
    `id`            BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `order_id`      BIGINT       NOT NULL COMMENT 'Order ID',
    `from_status`   VARCHAR(16)  DEFAULT NULL COMMENT 'From status',
    `to_status`     VARCHAR(16)  NOT NULL COMMENT 'To status',
    `changed_by`    BIGINT       DEFAULT NULL COMMENT 'Changed by user ID',
    `change_reason` VARCHAR(255) DEFAULT NULL COMMENT 'Change reason',
    `occurred_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    PRIMARY KEY (`id`),
    KEY `idx_registration_status_log_order` (`order_id`, `occurred_at`),
    CONSTRAINT `fk_registration_status_log_order` FOREIGN KEY (`order_id`) REFERENCES `registration_order` (`id`),
    CONSTRAINT `fk_registration_status_log_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Registration order status log';

CREATE TABLE `slot_inventory_log` (
    `id`               BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `slot_id`          BIGINT       NOT NULL COMMENT 'Slot ID',
    `from_status`      VARCHAR(16)  DEFAULT NULL COMMENT 'From status',
    `to_status`        VARCHAR(16)  NOT NULL COMMENT 'To status',
    `related_order_id` BIGINT       DEFAULT NULL COMMENT 'Related order ID',
    `related_hold_id`  BIGINT       DEFAULT NULL COMMENT 'Related hold ID',
    `reason`           VARCHAR(255) DEFAULT NULL COMMENT 'Reason',
    `occurred_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    PRIMARY KEY (`id`),
    KEY `idx_slot_inventory_log_slot` (`slot_id`, `occurred_at`),
    CONSTRAINT `fk_slot_inventory_log_slot` FOREIGN KEY (`slot_id`) REFERENCES `clinic_slot` (`id`),
    CONSTRAINT `fk_slot_inventory_log_order` FOREIGN KEY (`related_order_id`) REFERENCES `registration_order` (`id`),
    CONSTRAINT `fk_slot_inventory_log_hold` FOREIGN KEY (`related_hold_id`) REFERENCES `registration_hold` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Slot inventory log';

CREATE TABLE `visit_encounter` (
    `id`                BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `order_id`          BIGINT      NOT NULL COMMENT 'Registration order ID',
    `patient_id`        BIGINT      NOT NULL COMMENT 'Patient user ID',
    `doctor_id`         BIGINT      NOT NULL COMMENT 'Doctor ID',
    `department_id`     BIGINT      NOT NULL COMMENT 'Department ID',
    `encounter_status`  VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/IN_PROGRESS/COMPLETED/CANCELLED',
    `check_in_at`       DATETIME    DEFAULT NULL COMMENT 'Check-in time',
    `called_at`         DATETIME    DEFAULT NULL COMMENT 'Called in time',
    `started_at`        DATETIME    DEFAULT NULL COMMENT 'Encounter start time',
    `finished_at`       DATETIME    DEFAULT NULL COMMENT 'Encounter finish time',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_visit_encounter_order` (`order_id`),
    KEY `idx_visit_encounter_patient` (`patient_id`, `created_at`),
    KEY `idx_visit_encounter_doctor` (`doctor_id`, `encounter_status`),
    CONSTRAINT `fk_visit_encounter_order` FOREIGN KEY (`order_id`) REFERENCES `registration_order` (`id`),
    CONSTRAINT `fk_visit_encounter_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_visit_encounter_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_visit_encounter_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_visit_encounter_status` CHECK (`encounter_status` IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Visit encounter';
