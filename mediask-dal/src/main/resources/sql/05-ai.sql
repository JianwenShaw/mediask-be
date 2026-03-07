-- ============================================================
-- 05-ai.sql  —  AI 问诊会话与知识库
-- ============================================================
-- 包含：ai_conversations (增强), ai_messages (增强),
--       ai_feedback_reviews (增强),
--       knowledge_documents (新增), knowledge_chunks (新增)
--
-- 删除表：ai_metrics_daily, ai_metrics_dept_daily（按需从明细表实时聚合）

-- ----- AI会话表（增强：+dept_id, chief_complaint, model, total_tokens） -----
CREATE TABLE `ai_conversations` (
    `id`                BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `conversation_uuid` VARCHAR(64)   NOT NULL                                              COMMENT '业务会话UUID',
    `user_id`           BIGINT        NOT NULL                                              COMMENT '用户ID',
    `dept_id`           BIGINT        DEFAULT NULL                                          COMMENT '关联科室ID（问诊导诊场景）',
    `scene_type`        VARCHAR(32)   NOT NULL                                              COMMENT '场景类型 pre_diagnosis/health_consult/follow_up',
    `chief_complaint`   VARCHAR(500)  DEFAULT NULL                                          COMMENT '主诉摘要（AI提取）',
    `summary`           VARCHAR(2000) DEFAULT NULL                                          COMMENT '会话摘要',
    `model`             VARCHAR(64)   DEFAULT NULL                                          COMMENT '主要使用的LLM模型',
    `total_tokens`      INT           DEFAULT 0                                             COMMENT '会话累计Token消耗',
    `status`            TINYINT       NOT NULL DEFAULT 1                                    COMMENT '状态 1-进行中 2-已结束 3-异常终止',
    `started_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '开始时间',
    `ended_at`          DATETIME      DEFAULT NULL                                          COMMENT '结束时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_uuid` (`conversation_uuid`),
    KEY `idx_ai_conv_user` (`user_id`),
    KEY `idx_ai_conv_dept` (`dept_id`),
    KEY `idx_ai_conv_started` (`started_at`),
    KEY `idx_ai_conv_status` (`status`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

-- ----- AI消息表（增强：+审计追踪字段，支持护栏方案） -----
CREATE TABLE `ai_messages` (
    `id`              BIGINT      NOT NULL                                              COMMENT '雪花ID',
    `conversation_id` BIGINT      NOT NULL                                              COMMENT '会话ID',
    `role`            TINYINT     NOT NULL                                              COMMENT '消息角色 1-user 2-assistant 3-system',
    `content`         TEXT        NOT NULL                                              COMMENT '消息内容',
    `context`         JSON        DEFAULT NULL                                          COMMENT 'RAG检索上下文（JSON）',
    `citations_json`  JSON        DEFAULT NULL                                          COMMENT '引用来源（doc_id/page/section/score）',
    `tokens_used`     INT         DEFAULT NULL                                          COMMENT '本条消息Token消耗',
    -- 审计与安全字段（对应 AI_GUARDRAILS_PLAN）
    `trace_id`        VARCHAR(64) DEFAULT NULL                                          COMMENT '链路追踪ID',
    `risk_level`      VARCHAR(10) DEFAULT NULL                                          COMMENT '风险等级 LOW/MEDIUM/HIGH',
    `model`           VARCHAR(64) DEFAULT NULL                                          COMMENT '使用的LLM模型',
    `latency_ms`      INT         DEFAULT NULL                                          COMMENT '响应延迟（毫秒）',
    `is_degraded`     TINYINT     NOT NULL DEFAULT 0                                    COMMENT '是否降级响应 0-否 1-是',
    `guardrail_action` VARCHAR(20) DEFAULT NULL                                         COMMENT '护栏动作 PASS/CAUTION/REFUSE',
    `matched_rules`   JSON        DEFAULT NULL                                          COMMENT '命中的护栏规则ID列表',
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `deleted_at`      DATETIME    DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_msg_conv` (`conversation_id`),
    KEY `idx_ai_msg_created` (`created_at`),
    KEY `idx_ai_msg_trace` (`trace_id`),
    KEY `idx_ai_msg_risk` (`risk_level`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

-- ----- AI复核记录表（增强：+message_id, feedback_type, correction_content, -department_id冗余） -----
CREATE TABLE `ai_feedback_reviews` (
    `id`                  BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `conversation_id`     BIGINT        NOT NULL                                              COMMENT '会话ID',
    `message_id`          BIGINT        DEFAULT NULL                                          COMMENT '针对的消息ID（精确到条）',
    `doctor_id`           BIGINT        NOT NULL                                              COMMENT '复核医生ID',
    `feedback_type`       VARCHAR(20)   NOT NULL DEFAULT 'REVIEW'                             COMMENT '反馈类型 REVIEW-专业复核 THUMBS-点赞点踩 CORRECTION-纠错',
    `review_score`        TINYINT       DEFAULT NULL                                          COMMENT '复核评分 1-5（REVIEW类型必填）',
    `is_adopted`          TINYINT       DEFAULT NULL                                          COMMENT '是否采纳AI建议 0-否 1-是',
    `correction_content`  VARCHAR(2000) DEFAULT NULL                                          COMMENT '纠正内容（CORRECTION类型）',
    `review_comment`      VARCHAR(1000) DEFAULT NULL                                          COMMENT '复核意见',
    `reviewed_at`         DATETIME      NOT NULL                                              COMMENT '复核时间',
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`          DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_review_conv` (`conversation_id`),
    KEY `idx_ai_review_msg` (`message_id`),
    KEY `idx_ai_review_doctor` (`doctor_id`, `reviewed_at`),
    KEY `idx_ai_review_date` (`reviewed_at`),
    KEY `idx_ai_review_type` (`feedback_type`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI复核记录表';

-- ----- 知识文档表（新增，对应 RAG 入库流程） -----
CREATE TABLE `knowledge_documents` (
    `id`              BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `doc_uuid`        VARCHAR(64)   NOT NULL                                              COMMENT '文档UUID',
    `title`           VARCHAR(255)  NOT NULL                                              COMMENT '文档标题',
    `source`          VARCHAR(255)  DEFAULT NULL                                          COMMENT '来源（文件路径/URL）',
    `doc_type`        VARCHAR(20)   NOT NULL DEFAULT 'MARKDOWN'                           COMMENT '文档类型 MARKDOWN/PDF/TEXT',
    `category`        VARCHAR(64)   DEFAULT NULL                                          COMMENT '分类（科室/病种/药品等）',
    `content_hash`    VARCHAR(64)   DEFAULT NULL                                          COMMENT '内容SHA256（去重用）',
    `chunk_count`     INT           NOT NULL DEFAULT 0                                    COMMENT '分块数量',
    `status`          TINYINT       NOT NULL DEFAULT 1                                    COMMENT '状态 0-已下线 1-已入库 2-入库中 3-入库失败',
    `ingested_at`     DATETIME      DEFAULT NULL                                          COMMENT '入库完成时间',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_uuid` (`doc_uuid`),
    KEY `idx_doc_category` (`category`),
    KEY `idx_doc_status` (`status`),
    KEY `idx_doc_content_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档表';

-- ----- 知识分块表（新增，对应 RAG 分块 + embedding） -----
-- 注意：向量数据本身存储在 Milvus，本表仅存文本与元数据，用于追溯和引用展示
CREATE TABLE `knowledge_chunks` (
    `id`           BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `document_id`  BIGINT        NOT NULL                                              COMMENT '所属文档ID',
    `chunk_index`  INT           NOT NULL                                              COMMENT '分块序号（从0开始）',
    `content`      TEXT          NOT NULL                                              COMMENT '分块文本内容',
    `section`      VARCHAR(255)  DEFAULT NULL                                          COMMENT '所属章节标题',
    `page`         INT           DEFAULT NULL                                          COMMENT '所在页码（PDF适用）',
    `token_count`  INT           DEFAULT NULL                                          COMMENT 'Token数估算',
    `vector_id`    VARCHAR(128)  DEFAULT NULL                                          COMMENT 'Milvus向量ID（关联用）',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_chunk` (`document_id`, `chunk_index`),
    KEY `idx_chunk_document` (`document_id`),
    KEY `idx_chunk_vector` (`vector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分块表';
