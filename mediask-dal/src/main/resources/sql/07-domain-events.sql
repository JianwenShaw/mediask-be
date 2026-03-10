-- ============================================================
-- 07-domain-events.sql  --  Audit, access log, outbox, domain events (V3)
-- ============================================================

CREATE TABLE `audit_event` (
    `id`                  BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `trace_id`            VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    `operator_user_id`    BIGINT      DEFAULT NULL COMMENT 'Operator user ID',
    `operator_role_code`  VARCHAR(64) DEFAULT NULL COMMENT 'Operator role code',
    `action_code`         VARCHAR(50) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/LOGIN/EXPORT/AI_REVIEW',
    `resource_type`       VARCHAR(50) DEFAULT NULL COMMENT 'Resource type',
    `resource_id`         VARCHAR(64) DEFAULT NULL COMMENT 'Resource ID',
    `client_ip`           VARCHAR(45) DEFAULT NULL COMMENT 'Client IP',
    `success_flag`        TINYINT     NOT NULL DEFAULT 1 COMMENT 'Success flag',
    `occurred_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    `created_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_audit_event_trace` (`trace_id`),
    KEY `idx_audit_event_user_time` (`operator_user_id`, `occurred_at`),
    KEY `idx_audit_event_resource` (`resource_type`, `resource_id`),
    CONSTRAINT `fk_audit_event_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit event index';

CREATE TABLE `audit_payload` (
    `id`                         BIGINT     NOT NULL COMMENT 'Snowflake ID',
    `audit_event_id`             BIGINT     NOT NULL COMMENT 'Audit event ID',
    `request_payload_encrypted`  MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted request payload',
    `before_payload_encrypted`   MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted before payload',
    `after_payload_encrypted`    MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted after payload',
    `request_payload_masked`     TEXT       DEFAULT NULL COMMENT 'Masked request payload',
    `before_payload_masked`      TEXT       DEFAULT NULL COMMENT 'Masked before payload',
    `after_payload_masked`       TEXT       DEFAULT NULL COMMENT 'Masked after payload',
    `created_at`                 DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_payload_event` (`audit_event_id`),
    CONSTRAINT `fk_audit_payload_event` FOREIGN KEY (`audit_event_id`) REFERENCES `audit_event` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit encrypted payload';

CREATE TABLE `data_access_log` (
    `id`                BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `trace_id`          VARCHAR(64)  DEFAULT NULL COMMENT 'Trace ID',
    `operator_user_id`  BIGINT       NOT NULL COMMENT 'Operator user ID',
    `operator_role_code` VARCHAR(64) DEFAULT NULL COMMENT 'Operator role code',
    `access_purpose`    VARCHAR(64)  NOT NULL COMMENT 'VIEW/EXPORT/PRINT/DEBUG/REVIEW',
    `resource_type`     VARCHAR(64)  NOT NULL COMMENT 'EMR/PRESCRIPTION/AI_CONTENT/AUDIT',
    `resource_id`       VARCHAR(64)  NOT NULL COMMENT 'Resource ID',
    `patient_user_id`   BIGINT       DEFAULT NULL COMMENT 'Related patient user ID',
    `access_result`     VARCHAR(16)  NOT NULL DEFAULT 'ALLOWED' COMMENT 'ALLOWED/DENIED',
    `deny_reason`       VARCHAR(255) DEFAULT NULL COMMENT 'Deny reason',
    `occurred_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    PRIMARY KEY (`id`),
    KEY `idx_data_access_log_operator` (`operator_user_id`, `occurred_at`),
    KEY `idx_data_access_log_resource` (`resource_type`, `resource_id`, `occurred_at`),
    KEY `idx_data_access_log_patient` (`patient_user_id`, `occurred_at`),
    CONSTRAINT `fk_data_access_log_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_data_access_log_patient` FOREIGN KEY (`patient_user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_data_access_log_purpose` CHECK (`access_purpose` IN ('VIEW', 'EXPORT', 'PRINT', 'DEBUG', 'REVIEW')),
    CONSTRAINT `chk_data_access_log_result` CHECK (`access_result` IN ('ALLOWED', 'DENIED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sensitive data access log';

CREATE TABLE `domain_event_stream` (
    `id`              BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `event_key`       VARCHAR(64) NOT NULL COMMENT 'Event key',
    `aggregate_type`  VARCHAR(32) NOT NULL COMMENT 'Aggregate type',
    `aggregate_id`    BIGINT      NOT NULL COMMENT 'Aggregate ID',
    `event_type`      VARCHAR(64) NOT NULL COMMENT 'Event type',
    `trace_id`        VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
    `event_payload_json` JSON     DEFAULT NULL COMMENT 'Minimal masked payload json',
    `event_payload_encrypted` MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted sensitive payload',
    `payload_hash`    VARCHAR(64) DEFAULT NULL COMMENT 'Payload hash',
    `occurred_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_domain_event_stream_key` (`event_key`),
    KEY `idx_domain_event_stream_aggregate` (`aggregate_type`, `aggregate_id`, `occurred_at`),
    KEY `idx_domain_event_stream_type` (`event_type`, `occurred_at`),
    CONSTRAINT `chk_domain_event_stream_payload` CHECK (`event_payload_json` IS NOT NULL OR `event_payload_encrypted` IS NOT NULL OR `payload_hash` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Domain event stream';

CREATE TABLE `outbox_event` (
    `id`               BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `event_key`        VARCHAR(64) NOT NULL COMMENT 'Event key',
    `aggregate_type`   VARCHAR(32) NOT NULL COMMENT 'Aggregate type',
    `aggregate_id`     BIGINT      NOT NULL COMMENT 'Aggregate ID',
    `event_type`       VARCHAR(64) NOT NULL COMMENT 'Event type',
    `trace_id`         VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
    `payload_json`     JSON        DEFAULT NULL COMMENT 'Minimal masked outbox payload json',
    `payload_encrypted` MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted sensitive outbox payload',
    `payload_hash`     VARCHAR(64) DEFAULT NULL COMMENT 'Payload hash',
    `publish_status`   VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHED/FAILED/ARCHIVED',
    `retry_count`      INT         NOT NULL DEFAULT 0 COMMENT 'Retry count',
    `next_retry_at`    DATETIME    DEFAULT NULL COMMENT 'Next retry at',
    `published_at`     DATETIME    DEFAULT NULL COMMENT 'Published at',
    `occurred_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_key` (`event_key`),
    KEY `idx_outbox_event_status` (`publish_status`, `next_retry_at`),
    CONSTRAINT `chk_outbox_event_status` CHECK (`publish_status` IN ('PENDING', 'PUBLISHED', 'FAILED', 'ARCHIVED')),
    CONSTRAINT `chk_outbox_event_retry` CHECK (`retry_count` >= 0),
    CONSTRAINT `chk_outbox_event_payload` CHECK (`payload_json` IS NOT NULL OR `payload_encrypted` IS NOT NULL OR `payload_hash` IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Reliable outbox event';

CREATE TABLE `integration_event_archive` (
    `id`              BIGINT   NOT NULL COMMENT 'Snowflake ID',
    `outbox_event_id` BIGINT   NOT NULL COMMENT 'Outbox event ID',
    `archived_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Archived at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_integration_event_archive_outbox` (`outbox_event_id`),
    CONSTRAINT `fk_integration_event_archive_outbox` FOREIGN KEY (`outbox_event_id`) REFERENCES `outbox_event` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Published integration event archive';
