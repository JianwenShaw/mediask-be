-- ============================================================
-- 05-ai.sql  --  AI session, model run, knowledge (V3)
-- ============================================================
-- Core assumption:
--   Python service owns LLM/RAG execution.
--   Java system owns business identity, access control, durable indexes,
--   review workflow, and minimal traceable run metadata.

CREATE TABLE `ai_session` (
    `id`                      BIGINT        NOT NULL COMMENT 'Snowflake ID',
    `session_uuid`            VARCHAR(64)   NOT NULL COMMENT 'Business session UUID',
    `patient_id`              BIGINT        NOT NULL COMMENT 'Patient user ID',
    `department_id`           BIGINT        DEFAULT NULL COMMENT 'Related department ID',
    `related_order_id`        BIGINT        DEFAULT NULL COMMENT 'Related registration order ID',
    `scene_type`              VARCHAR(32)   NOT NULL COMMENT 'PRE_DIAGNOSIS/HEALTH_CONSULT/FOLLOW_UP',
    `session_status`          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/ABORTED',
    `entrypoint`              VARCHAR(16)   NOT NULL DEFAULT 'JAVA' COMMENT 'JAVA/PYTHON',
    `chief_complaint_summary` VARCHAR(500)  DEFAULT NULL COMMENT 'Chief complaint summary',
    `summary`                 VARCHAR(2000) DEFAULT NULL COMMENT 'Conversation summary',
    `started_at`              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Started at',
    `ended_at`                DATETIME      DEFAULT NULL COMMENT 'Ended at',
    `created_at`              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_session_uuid` (`session_uuid`),
    KEY `idx_ai_session_patient` (`patient_id`, `started_at`),
    KEY `idx_ai_session_department` (`department_id`, `started_at`),
    CONSTRAINT `fk_ai_session_patient` FOREIGN KEY (`patient_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_ai_session_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `fk_ai_session_order` FOREIGN KEY (`related_order_id`) REFERENCES `registration_order` (`id`),
    CONSTRAINT `chk_ai_session_scene` CHECK (`scene_type` IN ('PRE_DIAGNOSIS', 'HEALTH_CONSULT', 'FOLLOW_UP')),
    CONSTRAINT `chk_ai_session_status` CHECK (`session_status` IN ('ACTIVE', 'CLOSED', 'ABORTED')),
    CONSTRAINT `chk_ai_session_entrypoint` CHECK (`entrypoint` IN ('JAVA', 'PYTHON'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI session header';

CREATE TABLE `ai_turn` (
    `id`               BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `session_id`       BIGINT      NOT NULL COMMENT 'AI session ID',
    `turn_no`          INT         NOT NULL COMMENT 'Turn number',
    `turn_status`      VARCHAR(16) NOT NULL DEFAULT 'COMPLETED' COMMENT 'PENDING/COMPLETED/FAILED',
    `input_hash`       VARCHAR(64) DEFAULT NULL COMMENT 'Masked input hash',
    `output_hash`      VARCHAR(64) DEFAULT NULL COMMENT 'Masked output hash',
    `started_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Started at',
    `finished_at`      DATETIME    DEFAULT NULL COMMENT 'Finished at',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_turn_session_no` (`session_id`, `turn_no`),
    KEY `idx_ai_turn_status` (`session_id`, `turn_status`),
    CONSTRAINT `fk_ai_turn_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`),
    CONSTRAINT `chk_ai_turn_status` CHECK (`turn_status` IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT `chk_ai_turn_no` CHECK (`turn_no` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI dialog turn';

CREATE TABLE `ai_turn_content` (
    `id`                 BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `turn_id`            BIGINT      NOT NULL COMMENT 'Turn ID',
    `content_role`       VARCHAR(16) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
    `content_encrypted`  MEDIUMTEXT  NOT NULL COMMENT 'Encrypted raw content',
    `content_masked`     TEXT        DEFAULT NULL COMMENT 'Masked content for preview',
    `content_hash`       VARCHAR(64) DEFAULT NULL COMMENT 'Content hash',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_ai_turn_content_turn` (`turn_id`, `content_role`),
    CONSTRAINT `fk_ai_turn_content_turn` FOREIGN KEY (`turn_id`) REFERENCES `ai_turn` (`id`),
    CONSTRAINT `chk_ai_turn_content_role` CHECK (`content_role` IN ('USER', 'ASSISTANT', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI encrypted content payload';

CREATE TABLE `ai_model_run` (
    `id`                     BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `turn_id`                BIGINT      NOT NULL COMMENT 'Turn ID',
    `provider_run_id`        VARCHAR(128) DEFAULT NULL COMMENT 'Python service run ID',
    `provider_name`          VARCHAR(32) NOT NULL COMMENT 'PYTHON_AI/DEEPSEEK/OPENAI_COMPATIBLE',
    `model_name`             VARCHAR(64) DEFAULT NULL COMMENT 'Model name',
    `trace_id`               VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    `rag_enabled`            TINYINT     NOT NULL DEFAULT 0 COMMENT 'Whether RAG enabled',
    `retrieval_provider`     VARCHAR(32) DEFAULT NULL COMMENT 'MILVUS/PYTHON_AI/NONE',
    `tokens_input`           INT         DEFAULT NULL COMMENT 'Input tokens',
    `tokens_output`          INT         DEFAULT NULL COMMENT 'Output tokens',
    `latency_ms`             INT         DEFAULT NULL COMMENT 'Latency ms',
    `run_status`             VARCHAR(16) NOT NULL DEFAULT 'SUCCEEDED' COMMENT 'RUNNING/SUCCEEDED/FAILED/DEGRADED',
    `is_degraded`            TINYINT     NOT NULL DEFAULT 0 COMMENT 'Degraded response flag',
    `request_payload_hash`   VARCHAR(64) DEFAULT NULL COMMENT 'Request payload hash',
    `response_payload_hash`  VARCHAR(64) DEFAULT NULL COMMENT 'Response payload hash',
    `started_at`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Started at',
    `finished_at`            DATETIME    DEFAULT NULL COMMENT 'Finished at',
    `created_at`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_model_run_provider` (`provider_name`, `provider_run_id`),
    KEY `idx_ai_model_run_turn` (`turn_id`, `started_at`),
    KEY `idx_ai_model_run_trace` (`trace_id`),
    CONSTRAINT `fk_ai_model_run_turn` FOREIGN KEY (`turn_id`) REFERENCES `ai_turn` (`id`),
    CONSTRAINT `chk_ai_model_run_provider` CHECK (`provider_name` IN ('PYTHON_AI', 'DEEPSEEK', 'OPENAI_COMPATIBLE')),
    CONSTRAINT `chk_ai_model_run_retrieval_provider` CHECK (`retrieval_provider` IS NULL OR `retrieval_provider` IN ('MILVUS', 'PYTHON_AI', 'NONE')),
    CONSTRAINT `chk_ai_model_run_status` CHECK (`run_status` IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'DEGRADED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI model run metadata';

CREATE TABLE `ai_run_artifact` (
    `id`                BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `run_id`            BIGINT      NOT NULL COMMENT 'Model run ID',
    `artifact_type`     VARCHAR(32) NOT NULL COMMENT 'SUMMARY/CITATION/ROUTING/RAG_CONTEXT/PROMPT_DEBUG',
    `artifact_json`     JSON        DEFAULT NULL COMMENT 'Masked or low-sensitivity artifact payload',
    `artifact_encrypted` MEDIUMTEXT DEFAULT NULL COMMENT 'Encrypted high-sensitivity artifact payload',
    `retention_until`   DATETIME    DEFAULT NULL COMMENT 'Retention deadline for sensitive payload',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_ai_run_artifact_run` (`run_id`, `artifact_type`),
    CONSTRAINT `fk_ai_run_artifact_run` FOREIGN KEY (`run_id`) REFERENCES `ai_model_run` (`id`),
    CONSTRAINT `chk_ai_run_artifact_type` CHECK (`artifact_type` IN ('SUMMARY', 'CITATION', 'ROUTING', 'RAG_CONTEXT', 'PROMPT_DEBUG')),
    CONSTRAINT `chk_ai_run_artifact_payload` CHECK (`artifact_json` IS NOT NULL OR `artifact_encrypted` IS NOT NULL),
    CONSTRAINT `chk_ai_run_artifact_retention` CHECK (
        (`artifact_type` IN ('RAG_CONTEXT', 'PROMPT_DEBUG') AND `retention_until` IS NOT NULL)
        OR (`artifact_type` IN ('SUMMARY', 'CITATION', 'ROUTING'))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI run artifact';

CREATE TABLE `ai_guardrail_event` (
    `id`                 BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `run_id`             BIGINT      NOT NULL COMMENT 'Model run ID',
    `risk_level`         VARCHAR(16) NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
    `action_taken`       VARCHAR(16) NOT NULL COMMENT 'PASS/CAUTION/REFUSE',
    `matched_rule_codes` JSON        DEFAULT NULL COMMENT 'Matched rule codes',
    `input_hash`         VARCHAR(64) DEFAULT NULL COMMENT 'Masked input hash',
    `output_hash`        VARCHAR(64) DEFAULT NULL COMMENT 'Masked output hash',
    `event_detail_json`  JSON        DEFAULT NULL COMMENT 'Guardrail detail payload',
    `occurred_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
    PRIMARY KEY (`id`),
    KEY `idx_ai_guardrail_event_run` (`run_id`, `occurred_at`),
    KEY `idx_ai_guardrail_event_level` (`risk_level`, `occurred_at`),
    CONSTRAINT `fk_ai_guardrail_event_run` FOREIGN KEY (`run_id`) REFERENCES `ai_model_run` (`id`),
    CONSTRAINT `chk_ai_guardrail_event_level` CHECK (`risk_level` IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT `chk_ai_guardrail_event_action` CHECK (`action_taken` IN ('PASS', 'CAUTION', 'REFUSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI guardrail event';

CREATE TABLE `ai_feedback_task` (
    `id`                 BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `session_id`         BIGINT      NOT NULL COMMENT 'AI session ID',
    `turn_id`            BIGINT      DEFAULT NULL COMMENT 'AI turn ID',
    `task_type`          VARCHAR(20) NOT NULL COMMENT 'REVIEW/CORRECTION/THUMBS',
    `task_status`        VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ASSIGNED/CLOSED/CANCELLED',
    `assigned_doctor_id` BIGINT      DEFAULT NULL COMMENT 'Assigned doctor ID',
    `created_by`         BIGINT      DEFAULT NULL COMMENT 'Created by user ID',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_ai_feedback_task_session` (`session_id`, `task_status`),
    KEY `idx_ai_feedback_task_doctor` (`assigned_doctor_id`, `task_status`),
    CONSTRAINT `fk_ai_feedback_task_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`),
    CONSTRAINT `fk_ai_feedback_task_turn` FOREIGN KEY (`turn_id`) REFERENCES `ai_turn` (`id`),
    CONSTRAINT `fk_ai_feedback_task_doctor` FOREIGN KEY (`assigned_doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `fk_ai_feedback_task_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_ai_feedback_task_type` CHECK (`task_type` IN ('REVIEW', 'CORRECTION', 'THUMBS')),
    CONSTRAINT `chk_ai_feedback_task_status` CHECK (`task_status` IN ('OPEN', 'ASSIGNED', 'CLOSED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI feedback task';

CREATE TABLE `ai_feedback_review` (
    `id`                 BIGINT        NOT NULL COMMENT 'Snowflake ID',
    `task_id`            BIGINT        NOT NULL COMMENT 'Feedback task ID',
    `reviewer_doctor_id` BIGINT        NOT NULL COMMENT 'Reviewer doctor ID',
    `review_result`      VARCHAR(16)   NOT NULL COMMENT 'APPROVED/REJECTED/CORRECTED',
    `review_score`       TINYINT       DEFAULT NULL COMMENT 'Review score 1-5',
    `correction_summary` VARCHAR(2000) DEFAULT NULL COMMENT 'Correction summary',
    `review_comment`     VARCHAR(1000) DEFAULT NULL COMMENT 'Review comment',
    `reviewed_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Reviewed at',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_feedback_review_task` (`task_id`),
    KEY `idx_ai_feedback_review_doctor` (`reviewer_doctor_id`, `reviewed_at`),
    CONSTRAINT `fk_ai_feedback_review_task` FOREIGN KEY (`task_id`) REFERENCES `ai_feedback_task` (`id`),
    CONSTRAINT `fk_ai_feedback_review_doctor` FOREIGN KEY (`reviewer_doctor_id`) REFERENCES `doctors` (`id`),
    CONSTRAINT `chk_ai_feedback_review_result` CHECK (`review_result` IN ('APPROVED', 'REJECTED', 'CORRECTED')),
    CONSTRAINT `chk_ai_feedback_review_score` CHECK (`review_score` IS NULL OR (`review_score` BETWEEN 1 AND 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI feedback review';

CREATE TABLE `knowledge_base` (
    `id`              BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `base_code`       VARCHAR(64)  NOT NULL COMMENT 'Knowledge base code',
    `base_name`       VARCHAR(128) NOT NULL COMMENT 'Knowledge base name',
    `owner_type`      VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/DEPARTMENT',
    `owner_dept_id`   BIGINT       DEFAULT NULL COMMENT 'Owner department ID',
    `embedding_model` VARCHAR(64)  DEFAULT NULL COMMENT 'Embedding model',
    `vector_backend`  VARCHAR(32)  NOT NULL DEFAULT 'MILVUS' COMMENT 'MILVUS/FAISS/NONE',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_base_code` (`base_code`),
    CONSTRAINT `fk_knowledge_base_owner_dept` FOREIGN KEY (`owner_dept_id`) REFERENCES `departments` (`id`),
    CONSTRAINT `chk_knowledge_base_owner_type` CHECK (`owner_type` IN ('SYSTEM', 'DEPARTMENT')),
    CONSTRAINT `chk_knowledge_base_backend` CHECK (`vector_backend` IN ('MILVUS', 'FAISS', 'NONE')),
    CONSTRAINT `chk_knowledge_base_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge base';

CREATE TABLE `knowledge_document` (
    `id`                  BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `knowledge_base_id`   BIGINT       NOT NULL COMMENT 'Knowledge base ID',
    `document_uuid`       VARCHAR(64)  NOT NULL COMMENT 'Document UUID',
    `title`               VARCHAR(255) NOT NULL COMMENT 'Document title',
    `source_uri`          VARCHAR(500) DEFAULT NULL COMMENT 'Source URI',
    `doc_type`            VARCHAR(16)  NOT NULL DEFAULT 'MARKDOWN' COMMENT 'MARKDOWN/PDF/TEXT',
    `category`            VARCHAR(64)  DEFAULT NULL COMMENT 'Document category',
    `content_hash`        VARCHAR(64)  DEFAULT NULL COMMENT 'Content hash',
    `ingest_status`       VARCHAR(16)  NOT NULL DEFAULT 'READY' COMMENT 'READY/INGESTING/FAILED/OFFLINE',
    `ingested_by_service` VARCHAR(32)  NOT NULL DEFAULT 'PYTHON_AI' COMMENT 'Producer service',
    `ingested_at`         DATETIME     DEFAULT NULL COMMENT 'Ingested at',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_document_uuid` (`document_uuid`),
    KEY `idx_knowledge_document_base` (`knowledge_base_id`, `ingest_status`),
    CONSTRAINT `fk_knowledge_document_base` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`),
    CONSTRAINT `chk_knowledge_document_type` CHECK (`doc_type` IN ('MARKDOWN', 'PDF', 'TEXT')),
    CONSTRAINT `chk_knowledge_document_status` CHECK (`ingest_status` IN ('READY', 'INGESTING', 'FAILED', 'OFFLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge document metadata';

CREATE TABLE `knowledge_chunk` (
    `id`             BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `document_id`    BIGINT       NOT NULL COMMENT 'Document ID',
    `chunk_index`    INT          NOT NULL COMMENT 'Chunk index',
    `content`        MEDIUMTEXT   NOT NULL COMMENT 'Chunk text',
    `section`        VARCHAR(255) DEFAULT NULL COMMENT 'Section title',
    `page_no`        INT          DEFAULT NULL COMMENT 'Page number',
    `token_count`    INT          DEFAULT NULL COMMENT 'Estimated token count',
    `vector_ref_id`  VARCHAR(128) DEFAULT NULL COMMENT 'Vector storage ID',
    `metadata_json`  JSON         DEFAULT NULL COMMENT 'Extra metadata',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_knowledge_chunk_doc_idx` (`document_id`, `chunk_index`),
    KEY `idx_knowledge_chunk_vector` (`vector_ref_id`),
    CONSTRAINT `fk_knowledge_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`id`),
    CONSTRAINT `chk_knowledge_chunk_index` CHECK (`chunk_index` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge chunk';
