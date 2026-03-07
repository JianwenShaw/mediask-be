-- ============================================================
-- 02-hospital-org.sql  —  医院组织与医生档案
-- ============================================================
-- 包含：hospitals, departments, doctors

-- ----- 医院表 -----
CREATE TABLE `hospitals` (
    `id`             BIGINT       NOT NULL                                              COMMENT '雪花ID',
    `hospital_name`  VARCHAR(128) NOT NULL                                              COMMENT '医院名称',
    `hospital_code`  VARCHAR(64)  NOT NULL                                              COMMENT '医院编码',
    `hospital_level` VARCHAR(32)  DEFAULT NULL                                          COMMENT '医院等级（三级甲等等）',
    `address`        VARCHAR(255) DEFAULT NULL                                          COMMENT '地址',
    `contact_phone`  VARCHAR(20)  DEFAULT NULL                                          COMMENT '联系电话',
    `status`         TINYINT      NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     DATETIME     DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hospital_code` (`hospital_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院表';

-- ----- 科室表 -----
CREATE TABLE `departments` (
    `id`            BIGINT        NOT NULL                                              COMMENT '雪花ID',
    `hospital_id`   BIGINT        NOT NULL                                              COMMENT '医院ID',
    `dept_code`     VARCHAR(64)   NOT NULL                                              COMMENT '科室编码',
    `dept_name`     VARCHAR(128)  NOT NULL                                              COMMENT '科室名称',
    `dept_intro`    VARCHAR(1000) DEFAULT NULL                                          COMMENT '科室简介',
    `display_order` INT           NOT NULL DEFAULT 0                                    COMMENT '显示顺序',
    `status`        TINYINT       NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME      DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hospital_dept_code` (`hospital_id`, `dept_code`),
    KEY `idx_department_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- ----- 医生档案表 -----
CREATE TABLE `doctors` (
    `id`               BIGINT         NOT NULL                                              COMMENT '雪花ID',
    `user_id`          BIGINT         NOT NULL                                              COMMENT '关联用户ID',
    `hospital_id`      BIGINT         NOT NULL                                              COMMENT '所属医院ID',
    `dept_id`          BIGINT         NOT NULL                                              COMMENT '所属科室ID',
    `doctor_code`      VARCHAR(64)    NOT NULL                                              COMMENT '医生编码',
    `title`            VARCHAR(64)    DEFAULT NULL                                          COMMENT '职称',
    `specialty`        VARCHAR(1000)  DEFAULT NULL                                          COMMENT '擅长领域(JSON数组)',
    `introduction`     VARCHAR(2000)  DEFAULT NULL                                          COMMENT '个人简介',
    `consultation_fee` DECIMAL(10, 2) NOT NULL DEFAULT 0.00                                 COMMENT '默认诊疗费用',
    `license_number`   VARCHAR(128)   DEFAULT NULL                                          COMMENT '执业证书号',
    `status`           TINYINT        NOT NULL DEFAULT 1                                    COMMENT '状态 0-停用 1-启用',
    `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       DATETIME       DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctor_user` (`user_id`),
    UNIQUE KEY `uk_doctor_code` (`doctor_code`),
    KEY `idx_doctor_dept` (`dept_id`),
    KEY `idx_doctor_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生档案表';
