-- ============================================================
-- 07-domain-events.sql  —  统一领域事件表
-- ============================================================
-- 合并原 appointment_events + schedule_events 为统一事件表。
-- 通过 aggregate_type + aggregate_id 区分不同聚合根。

CREATE TABLE `domain_events` (
    `id`              BIGINT       NOT NULL                              COMMENT '雪花ID',
    `aggregate_type`  VARCHAR(32)  NOT NULL                              COMMENT '聚合根类型 APPOINTMENT/SCHEDULE/PLAN/CONVERSATION/MEDICAL_RECORD',
    `aggregate_id`    BIGINT       NOT NULL                              COMMENT '聚合根ID',
    `event_type`      VARCHAR(64)  NOT NULL                              COMMENT '事件类型（如 APPT_CREATED/SCHEDULE_PUBLISHED/PLAN_ARCHIVED）',
    `from_status`     VARCHAR(20)  DEFAULT NULL                          COMMENT '原状态',
    `to_status`       VARCHAR(20)  DEFAULT NULL                          COMMENT '新状态',
    `operator_type`   VARCHAR(16)  DEFAULT NULL                          COMMENT '操作者类型 SYSTEM/ADMIN/DOCTOR/PATIENT',
    `operator_id`     BIGINT       DEFAULT NULL                          COMMENT '操作者ID',
    `payload_json`    JSON         DEFAULT NULL                          COMMENT '事件载荷（JSON，包含变更详情）',
    `trace_id`        VARCHAR(64)  DEFAULT NULL                          COMMENT '链路追踪ID',
    `occurred_at`     DATETIME     NOT NULL                              COMMENT '事件发生时间',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_event_aggregate` (`aggregate_type`, `aggregate_id`, `occurred_at`),
    KEY `idx_event_type` (`event_type`, `occurred_at`),
    KEY `idx_event_occurred` (`occurred_at`),
    KEY `idx_event_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一领域事件表';
