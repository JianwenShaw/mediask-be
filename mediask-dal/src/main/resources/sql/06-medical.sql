-- ============================================================
-- 06-medical.sql  —  病历与处方
-- ============================================================
-- 包含：medical_records (新增), medical_record_versions (新增),
--       drugs (新增), prescriptions (新增), prescription_items (新增)
--
-- 这些表为后续病历管理和处方功能预留，当前阶段可选创建。

-- ----- 病历表 -----
CREATE TABLE `medical_records` (
    `id`                BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `record_no`         VARCHAR(32)   NOT NULL                                              COMMENT '病历编号',
    `patient_id`        BIGINT        NOT NULL                                              COMMENT '患者ID',
    `doctor_id`         BIGINT        NOT NULL                                              COMMENT '主诊医生ID',
    `appointment_id`    BIGINT        DEFAULT NULL                                          COMMENT '关联预约ID',
    `conversation_id`   BIGINT        DEFAULT NULL                                          COMMENT '关联AI会话ID',
    `dept_id`           BIGINT        NOT NULL                                              COMMENT '就诊科室ID',
    `visit_date`        DATE          NOT NULL                                              COMMENT '就诊日期',
    `chief_complaint`   VARCHAR(500)  DEFAULT NULL                                          COMMENT '主诉',
    `present_illness`   TEXT          DEFAULT NULL                                          COMMENT '现病史',
    `past_history`      TEXT          DEFAULT NULL                                          COMMENT '既往史',
    `physical_exam`     TEXT          DEFAULT NULL                                          COMMENT '体格检查',
    `diagnosis`         VARCHAR(1000) DEFAULT NULL                                          COMMENT '诊断（JSON数组，支持多诊断）',
    `treatment_plan`    TEXT          DEFAULT NULL                                          COMMENT '治疗方案',
    `ai_suggestion`     TEXT          DEFAULT NULL                                          COMMENT 'AI辅助建议（来自问诊会话摘要）',
    `record_status`     TINYINT       NOT NULL DEFAULT 1                                    COMMENT '状态 1-草稿 2-已签署 3-已修订',
    `signed_at`         DATETIME      DEFAULT NULL                                          COMMENT '签署时间',
    `version`           INT           NOT NULL DEFAULT 0                                    COMMENT '乐观锁版本',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    KEY `idx_record_patient` (`patient_id`),
    KEY `idx_record_doctor` (`doctor_id`),
    KEY `idx_record_appt` (`appointment_id`),
    KEY `idx_record_dept_date` (`dept_id`, `visit_date`),
    KEY `idx_record_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历表';

-- ----- 病历版本表（审计追溯，记录每次修改） -----
CREATE TABLE `medical_record_versions` (
    `id`            BIGINT     NOT NULL                              COMMENT '雪花ID',
    `record_id`     BIGINT     NOT NULL                              COMMENT '病历ID',
    `version_no`    INT        NOT NULL                              COMMENT '版本号',
    `snapshot_json` JSON       NOT NULL                              COMMENT '病历内容快照（完整JSON）',
    `change_reason` VARCHAR(255) DEFAULT NULL                        COMMENT '修改原因',
    `changed_by`    BIGINT     NOT NULL                              COMMENT '修改人ID',
    `created_at`    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_version` (`record_id`, `version_no`),
    KEY `idx_record_ver_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历版本表';

-- ----- 药品字典表 -----
CREATE TABLE `drugs` (
    `id`                BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `drug_code`         VARCHAR(64)    NOT NULL                                              COMMENT '药品编码',
    `drug_name`         VARCHAR(128)   NOT NULL                                              COMMENT '药品名称',
    `generic_name`      VARCHAR(128)   DEFAULT NULL                                          COMMENT '通用名',
    `specification`     VARCHAR(128)   DEFAULT NULL                                          COMMENT '规格',
    `unit`              VARCHAR(20)    DEFAULT NULL                                          COMMENT '单位（片/ml/支等）',
    `manufacturer`      VARCHAR(128)   DEFAULT NULL                                          COMMENT '生产厂家',
    `unit_price`        DECIMAL(10, 2) DEFAULT NULL                                          COMMENT '单价',
    `drug_category`     VARCHAR(32)    DEFAULT NULL                                          COMMENT '药品分类（处方药/非处方药/中成药等）',
    `contraindication`  TEXT           DEFAULT NULL                                          COMMENT '禁忌说明',
    `status`            TINYINT        NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME       DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_drug_code` (`drug_code`),
    KEY `idx_drug_name` (`drug_name`),
    KEY `idx_drug_category` (`drug_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品字典表';

-- ----- 处方表 -----
CREATE TABLE `prescriptions` (
    `id`              BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `prescription_no` VARCHAR(32)    NOT NULL                                              COMMENT '处方编号',
    `record_id`       BIGINT         NOT NULL                                              COMMENT '关联病历ID',
    `patient_id`      BIGINT         NOT NULL                                              COMMENT '患者ID',
    `doctor_id`       BIGINT         NOT NULL                                              COMMENT '开方医生ID',
    `prescription_type` VARCHAR(20)  NOT NULL DEFAULT 'WESTERN'                             COMMENT '处方类型 WESTERN-西药 CHINESE-中药',
    `total_amount`    DECIMAL(10, 2) NOT NULL DEFAULT 0.00                                  COMMENT '处方总金额',
    `prescription_status` TINYINT    NOT NULL DEFAULT 1                                     COMMENT '状态 1-草稿 2-已签署 3-已发药 4-已作废',
    `signed_at`       DATETIME       DEFAULT NULL                                           COMMENT '签署时间',
    `dispensed_at`    DATETIME       DEFAULT NULL                                           COMMENT '发药时间',
    `notes`           VARCHAR(500)   DEFAULT NULL                                           COMMENT '医嘱备注',
    `version`         INT            NOT NULL DEFAULT 0                                     COMMENT '乐观锁版本',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME       DEFAULT NULL                                           COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prescription_no` (`prescription_no`),
    KEY `idx_prescription_record` (`record_id`),
    KEY `idx_prescription_patient` (`patient_id`),
    KEY `idx_prescription_doctor` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';

-- ----- 处方明细表 -----
CREATE TABLE `prescription_items` (
    `id`              BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `prescription_id` BIGINT         NOT NULL                                              COMMENT '处方ID',
    `drug_id`         BIGINT         NOT NULL                                              COMMENT '药品ID',
    `drug_name`       VARCHAR(128)   NOT NULL                                              COMMENT '药品名称（冗余，防止药品信息变更）',
    `specification`   VARCHAR(128)   DEFAULT NULL                                          COMMENT '规格（冗余）',
    `dosage`          VARCHAR(64)    DEFAULT NULL                                          COMMENT '用量（如：每次2片）',
    `frequency`       VARCHAR(64)    DEFAULT NULL                                          COMMENT '用药频次（如：每日3次）',
    `route`           VARCHAR(64)    DEFAULT NULL                                          COMMENT '给药途径（口服/静脉注射等）',
    `duration_days`   INT            DEFAULT NULL                                          COMMENT '用药天数',
    `quantity`        INT            NOT NULL DEFAULT 1                                    COMMENT '开药数量',
    `unit_price`      DECIMAL(10, 2) NOT NULL DEFAULT 0.00                                 COMMENT '单价（快照）',
    `subtotal`        DECIMAL(10, 2) NOT NULL DEFAULT 0.00                                 COMMENT '小计',
    `notes`           VARCHAR(255)   DEFAULT NULL                                          COMMENT '备注',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_prescription` (`prescription_id`),
    KEY `idx_item_drug` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表';
