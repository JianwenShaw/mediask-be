-- ============================================================
-- 01-base-auth.sql  —  基础认证、授权与审计
-- ============================================================
-- 包含：test_connections, users, roles, permissions,
--       user_roles, role_permissions, data_scope_rules, audit_logs

-- 连接测试表
CREATE TABLE `test_connections` (
    `id`         BIGINT       NOT NULL                              COMMENT '雪花ID',
    `message`    VARCHAR(255) DEFAULT NULL                          COMMENT '测试消息',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接测试表';

-- ----- 用户表 -----
CREATE TABLE `users` (
    `id`                BIGINT       NOT NULL                                              COMMENT '雪花ID',
    `username`          VARCHAR(64)  NOT NULL                                              COMMENT '用户名',
    `phone`             VARCHAR(20)  DEFAULT NULL                                          COMMENT '手机号',
    `id_card_encrypted` VARCHAR(255) DEFAULT NULL                                          COMMENT 'AES加密身份证号(Base64)',
    `password`          VARCHAR(255) NOT NULL                                              COMMENT 'BCrypt加密密码',
    `user_type`         TINYINT      NOT NULL                                              COMMENT '用户类型 1-患者 2-医生 3-管理员',
    `real_name`         VARCHAR(64)  DEFAULT NULL                                          COMMENT '真实姓名',
    `gender`            TINYINT      DEFAULT 0                                             COMMENT '性别 0-未知 1-男 2-女',
    `birth_date`        DATE         DEFAULT NULL                                          COMMENT '出生日期',
    `avatar_url`        VARCHAR(255) DEFAULT NULL                                          COMMENT '头像URL',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME     DEFAULT NULL                                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----- 角色表（增强：支持继承与系统角色标记） -----
CREATE TABLE `roles` (
    `id`          BIGINT       NOT NULL                              COMMENT '雪花ID',
    `role_code`   VARCHAR(64)  NOT NULL                              COMMENT '角色编码',
    `role_name`   VARCHAR(64)  NOT NULL                              COMMENT '角色名称',
    `parent_id`   BIGINT       DEFAULT NULL                          COMMENT '父角色ID（支持继承）',
    `level`       INT          NOT NULL DEFAULT 0                    COMMENT '角色等级（数值越大权限越高）',
    `is_system`   TINYINT      NOT NULL DEFAULT 0                    COMMENT '是否系统内置角色 0-否 1-是',
    `description` VARCHAR(255) DEFAULT NULL                          COMMENT '描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME     DEFAULT NULL                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roles_code` (`role_code`),
    KEY `idx_roles_parent` (`parent_id`),
    KEY `idx_roles_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----- 权限表（增强：树形结构，支持菜单/按钮/API） -----
CREATE TABLE `permissions` (
    `id`          BIGINT        NOT NULL                              COMMENT '雪花ID',
    `perm_code`   VARCHAR(100)  NOT NULL                              COMMENT '权限编码',
    `perm_name`   VARCHAR(64)   NOT NULL                              COMMENT '权限名称',
    `parent_id`   BIGINT        NOT NULL DEFAULT 0                    COMMENT '父权限ID（0=顶级）',
    `perm_type`   VARCHAR(20)   NOT NULL DEFAULT 'API'                COMMENT '类型 MENU/BUTTON/API',
    `path`        VARCHAR(255)  DEFAULT NULL                          COMMENT 'API路径或前端路由',
    `method`      VARCHAR(10)   DEFAULT NULL                          COMMENT 'HTTP方法 GET/POST/PUT/DELETE',
    `sort_order`  INT           NOT NULL DEFAULT 0                    COMMENT '排序（同级内）',
    `icon`        VARCHAR(50)   DEFAULT NULL                          COMMENT '菜单图标',
    `description` VARCHAR(255)  DEFAULT NULL                          COMMENT '描述',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME      DEFAULT NULL                          COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permissions_code` (`perm_code`),
    KEY `idx_permissions_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----- 用户角色关联表（增强：支持有效期、授权追溯） -----
CREATE TABLE `user_roles` (
    `id`           BIGINT       NOT NULL                              COMMENT '雪花ID',
    `user_id`      BIGINT       NOT NULL                              COMMENT '用户ID',
    `role_id`      BIGINT       NOT NULL                              COMMENT '角色ID',
    `valid_from`   DATETIME     DEFAULT NULL                          COMMENT '生效时间（NULL=立即）',
    `valid_until`  DATETIME     DEFAULT NULL                          COMMENT '失效时间（NULL=永久）',
    `grant_reason` VARCHAR(255) DEFAULT NULL                          COMMENT '授权原因',
    `grantor_id`   BIGINT       DEFAULT NULL                          COMMENT '授权人ID',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_roles_user_id` (`user_id`),
    KEY `idx_user_roles_role_id` (`role_id`),
    KEY `idx_user_roles_valid` (`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----- 角色权限关联表（保持不变） -----
CREATE TABLE `role_permissions` (
    `id`            BIGINT   NOT NULL                              COMMENT '雪花ID',
    `role_id`       BIGINT   NOT NULL                              COMMENT '角色ID',
    `permission_id` BIGINT   NOT NULL                              COMMENT '权限ID',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_permissions_role_id` (`role_id`),
    KEY `idx_role_permissions_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ----- 数据权限范围表（新增） -----
CREATE TABLE `data_scope_rules` (
    `id`               BIGINT      NOT NULL                              COMMENT '雪花ID',
    `role_id`          BIGINT      NOT NULL                              COMMENT '角色ID',
    `resource_type`    VARCHAR(50) NOT NULL                              COMMENT '资源类型 APPOINTMENT/SCHEDULE/MEDICAL_RECORD等',
    `scope_type`       VARCHAR(20) NOT NULL                              COMMENT '范围类型 ALL/DEPARTMENT/SELF/CUSTOM',
    `custom_condition` JSON        DEFAULT NULL                          COMMENT '自定义条件（JSON，scope_type=CUSTOM时使用）',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`role_id`, `resource_type`),
    KEY `idx_data_scope_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限规则表';

-- ----- 审计日志表（新增） -----
CREATE TABLE `audit_logs` (
    `id`             BIGINT       NOT NULL                              COMMENT '雪花ID',
    `user_id`        BIGINT       DEFAULT NULL                          COMMENT '操作用户ID',
    `username`       VARCHAR(64)  DEFAULT NULL                          COMMENT '用户名（冗余，便于查询）',
    `user_role`      VARCHAR(64)  DEFAULT NULL                          COMMENT '操作时角色编码',
    `action`         VARCHAR(50)  NOT NULL                              COMMENT '操作类型 CREATE/UPDATE/DELETE/LOGIN/EXPORT等',
    `resource_type`  VARCHAR(50)  DEFAULT NULL                          COMMENT '资源类型',
    `resource_id`    VARCHAR(64)  DEFAULT NULL                          COMMENT '资源ID',
    `resource_name`  VARCHAR(255) DEFAULT NULL                          COMMENT '资源名称',
    `client_ip`      VARCHAR(45)  DEFAULT NULL                          COMMENT '客户端IP（支持IPv6）',
    `user_agent`     VARCHAR(500) DEFAULT NULL                          COMMENT 'User-Agent',
    `trace_id`       VARCHAR(64)  DEFAULT NULL                          COMMENT '链路追踪ID',
    `old_value`      JSON         DEFAULT NULL                          COMMENT '变更前值（JSON，脱敏后）',
    `new_value`      JSON         DEFAULT NULL                          COMMENT '变更后值（JSON，脱敏后）',
    `request_params` JSON         DEFAULT NULL                          COMMENT '请求参数（JSON，脱敏后）',
    `success`        TINYINT      NOT NULL DEFAULT 1                    COMMENT '是否成功 0-失败 1-成功',
    `fail_reason`    VARCHAR(500) DEFAULT NULL                          COMMENT '失败原因',
    `occurred_at`    DATETIME     NOT NULL                              COMMENT '操作发生时间',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_user_time` (`user_id`, `occurred_at`),
    KEY `idx_audit_resource` (`resource_type`, `resource_id`),
    KEY `idx_audit_action` (`action`),
    KEY `idx_audit_occurred` (`occurred_at`),
    KEY `idx_audit_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
