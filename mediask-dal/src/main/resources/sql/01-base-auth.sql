-- ============================================================
-- 01-base-auth.sql  --  Auth and identity (V3)
-- ============================================================

CREATE TABLE `users` (
    `id`             BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `username`       VARCHAR(64)  NOT NULL COMMENT 'Login name',
    `password_hash`  VARCHAR(255) NOT NULL COMMENT 'BCrypt password hash',
    `user_type`      VARCHAR(16)  NOT NULL COMMENT 'PATIENT/DOCTOR/ADMIN',
    `account_status` VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LOCKED/DISABLED',
    `last_login_at`  DATETIME     DEFAULT NULL COMMENT 'Last login time',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    CONSTRAINT `chk_users_type` CHECK (`user_type` IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    CONSTRAINT `chk_users_status` CHECK (`account_status` IN ('ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User account table';

CREATE TABLE `user_pii_profile` (
    `id`                         BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `user_id`                    BIGINT       NOT NULL COMMENT 'User ID',
    `real_name_encrypted`        VARCHAR(255) DEFAULT NULL COMMENT 'Encrypted real name',
    `phone_encrypted`            VARCHAR(255) DEFAULT NULL COMMENT 'Encrypted phone',
    `phone_masked`               VARCHAR(32)  DEFAULT NULL COMMENT 'Masked phone for display',
    `id_card_encrypted`          VARCHAR(255) DEFAULT NULL COMMENT 'Encrypted id card number',
    `gender`                     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 unknown 1 male 2 female',
    `birth_date`                 DATE         DEFAULT NULL COMMENT 'Birth date',
    `emergency_contact_name`     VARCHAR(128) DEFAULT NULL COMMENT 'Emergency contact name',
    `emergency_contact_encrypted` VARCHAR(255) DEFAULT NULL COMMENT 'Encrypted emergency contact phone',
    `created_at`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pii_user` (`user_id`),
    CONSTRAINT `fk_user_pii_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_user_pii_gender` CHECK (`gender` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sensitive profile table';

CREATE TABLE `patient_profile` (
    `id`                 BIGINT        NOT NULL COMMENT 'Snowflake ID',
    `user_id`            BIGINT        NOT NULL COMMENT 'User ID',
    `patient_no`         VARCHAR(32)   NOT NULL COMMENT 'Patient number',
    `allergy_summary`    VARCHAR(1000) DEFAULT NULL COMMENT 'Allergy summary',
    `blood_type`         VARCHAR(8)    DEFAULT NULL COMMENT 'Blood type',
    `default_dept_id`    BIGINT        DEFAULT NULL COMMENT 'Preferred department',
    `profile_status`     VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_patient_profile_user` (`user_id`),
    UNIQUE KEY `uk_patient_profile_no` (`patient_no`),
    KEY `idx_patient_default_dept` (`default_dept_id`),
    CONSTRAINT `fk_patient_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_patient_profile_status` CHECK (`profile_status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Patient business profile';

CREATE TABLE `roles` (
    `id`          BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `role_code`   VARCHAR(64)  NOT NULL COMMENT 'Role code',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT 'Role name',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT 'Parent role ID',
    `level`       INT          NOT NULL DEFAULT 0 COMMENT 'Role level',
    `is_system`   TINYINT      NOT NULL DEFAULT 0 COMMENT 'System builtin role',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roles_code` (`role_code`),
    KEY `idx_roles_parent` (`parent_id`),
    CONSTRAINT `fk_roles_parent` FOREIGN KEY (`parent_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role table';

CREATE TABLE `permissions` (
    `id`          BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `perm_code`   VARCHAR(100) NOT NULL COMMENT 'Permission code',
    `perm_name`   VARCHAR(64)  NOT NULL COMMENT 'Permission name',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT 'Parent permission ID',
    `perm_type`   VARCHAR(20)  NOT NULL COMMENT 'MENU/BUTTON/API',
    `path`        VARCHAR(255) DEFAULT NULL COMMENT 'Route or API path',
    `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP method',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT 'Sort order',
    `icon`        VARCHAR(50)  DEFAULT NULL COMMENT 'Icon',
    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permissions_code` (`perm_code`),
    KEY `idx_permissions_parent` (`parent_id`),
    CONSTRAINT `fk_permissions_parent` FOREIGN KEY (`parent_id`) REFERENCES `permissions` (`id`),
    CONSTRAINT `chk_permissions_type` CHECK (`perm_type` IN ('MENU', 'BUTTON', 'API'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Permission table';

CREATE TABLE `user_roles` (
    `id`           BIGINT       NOT NULL COMMENT 'Snowflake ID',
    `user_id`      BIGINT       NOT NULL COMMENT 'User ID',
    `role_id`      BIGINT       NOT NULL COMMENT 'Role ID',
    `valid_from`   DATETIME     DEFAULT NULL COMMENT 'Valid from',
    `valid_until`  DATETIME     DEFAULT NULL COMMENT 'Valid until',
    `grant_reason` VARCHAR(255) DEFAULT NULL COMMENT 'Grant reason',
    `grantor_id`   BIGINT       DEFAULT NULL COMMENT 'Grantor user ID',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_roles_user_role` (`user_id`, `role_id`),
    KEY `idx_user_roles_role` (`role_id`),
    KEY `idx_user_roles_grantor` (`grantor_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `fk_user_roles_grantor` FOREIGN KEY (`grantor_id`) REFERENCES `users` (`id`),
    CONSTRAINT `chk_user_roles_time` CHECK (`valid_until` IS NULL OR `valid_from` IS NULL OR `valid_until` >= `valid_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User role relation';

CREATE TABLE `role_permissions` (
    `id`            BIGINT   NOT NULL COMMENT 'Snowflake ID',
    `role_id`       BIGINT   NOT NULL COMMENT 'Role ID',
    `permission_id` BIGINT   NOT NULL COMMENT 'Permission ID',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permissions_role_perm` (`role_id`, `permission_id`),
    KEY `idx_role_permissions_perm` (`permission_id`),
    CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role permission relation';

CREATE TABLE `data_scope_rules` (
    `id`               BIGINT      NOT NULL COMMENT 'Snowflake ID',
    `role_id`          BIGINT      NOT NULL COMMENT 'Role ID',
    `resource_type`    VARCHAR(50) NOT NULL COMMENT 'Resource type',
    `scope_type`       VARCHAR(20) NOT NULL COMMENT 'ALL/DEPARTMENT/SELF/CUSTOM',
    `custom_condition` JSON        DEFAULT NULL COMMENT 'Custom rule json',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_scope_role_resource` (`role_id`, `resource_type`),
    CONSTRAINT `fk_data_scope_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
    CONSTRAINT `chk_data_scope_type` CHECK (`scope_type` IN ('ALL', 'DEPARTMENT', 'SELF', 'CUSTOM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Data scope rule';
