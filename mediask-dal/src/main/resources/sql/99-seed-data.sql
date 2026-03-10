-- ============================================================
-- 99-seed-data.sql  --  Minimal local demo seed for V3
-- ============================================================

INSERT INTO `users` (`id`, `username`, `password_hash`, `user_type`, `account_status`) VALUES
(100000000001, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 'ACTIVE'),
(100000000002, 'doctor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'DOCTOR', 'ACTIVE'),
(100000000003, 'patient1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'PATIENT', 'ACTIVE');

INSERT INTO `user_pii_profile` (`id`, `user_id`, `real_name_encrypted`, `phone_masked`, `gender`) VALUES
(100000000011, 100000000001, 'ENC_ADMIN', '137****0000', 1),
(100000000012, 100000000002, 'ENC_DOCTOR', '138****0001', 1),
(100000000013, 100000000003, 'ENC_PATIENT', '139****0001', 2);

INSERT INTO `patient_profile` (`id`, `user_id`, `patient_no`, `allergy_summary`, `profile_status`) VALUES
(100000000021, 100000000003, 'P000001', '青霉素过敏', 'ACTIVE');

INSERT INTO `roles` (`id`, `role_code`, `role_name`, `level`, `is_system`) VALUES
(100000000031, 'super_admin', '超级管理员', 100, 1),
(100000000032, 'doctor', '医生', 50, 1),
(100000000033, 'patient', '患者', 10, 1);

INSERT INTO `permissions` (`id`, `perm_code`, `perm_name`, `perm_type`, `path`, `method`, `sort_order`) VALUES
(100000000041, 'schedule:publish', '发布门诊', 'API', '/api/v1/scheduling/publish', 'POST', 1),
(100000000042, 'registration:create', '创建挂号', 'API', '/api/v1/registrations', 'POST', 2),
(100000000043, 'ai:review', 'AI复核', 'API', '/api/v1/ai/reviews', 'POST', 3),
(100000000044, 'audit:view', '查看审计', 'API', '/api/v1/audit/events', 'GET', 4);

INSERT INTO `user_roles` (`id`, `user_id`, `role_id`, `grant_reason`) VALUES
(100000000051, 100000000001, 100000000031, '初始化'),
(100000000052, 100000000002, 100000000032, '初始化'),
(100000000053, 100000000003, 100000000033, '初始化');

INSERT INTO `role_permissions` (`id`, `role_id`, `permission_id`) VALUES
(100000000061, 100000000031, 100000000041),
(100000000062, 100000000031, 100000000042),
(100000000063, 100000000031, 100000000043),
(100000000064, 100000000031, 100000000044),
(100000000065, 100000000032, 100000000043),
(100000000066, 100000000033, 100000000042);

INSERT INTO `data_scope_rules` (`id`, `role_id`, `resource_type`, `scope_type`) VALUES
(100000000071, 100000000031, 'EMR_RECORD', 'ALL'),
(100000000072, 100000000032, 'EMR_RECORD', 'DEPARTMENT'),
(100000000073, 100000000033, 'REGISTRATION_ORDER', 'SELF');

INSERT INTO `hospitals` (`id`, `hospital_name`, `hospital_code`, `hospital_level`, `address`, `contact_phone`, `status`) VALUES
(100000000101, 'MediAsk附属医院', 'MDA001', '三级甲等', '示例路100号', '010-88886666', 'ACTIVE');

INSERT INTO `departments` (`id`, `hospital_id`, `dept_code`, `dept_name`, `dept_type`, `display_order`, `status`) VALUES
(100000000111, 100000000101, 'CARD', '心内科', 'OUTPATIENT', 1, 'ACTIVE'),
(100000000112, 100000000101, 'GEN', '全科医学', 'OUTPATIENT', 2, 'ACTIVE');

INSERT INTO `doctors` (`id`, `user_id`, `hospital_id`, `doctor_code`, `title`, `specialty_summary`, `default_consultation_fee`, `doctor_status`) VALUES
(100000000121, 100000000002, 100000000101, 'D0001', '主治医师', '心血管与慢病管理', 50.00, 'ACTIVE');

INSERT INTO `doctor_department_rel` (`id`, `doctor_id`, `department_id`, `is_primary`, `can_schedule`, `valid_from`, `status`) VALUES
(100000000131, 100000000121, 100000000111, 1, 1, CURDATE(), 'ACTIVE');

INSERT INTO `schedule_ruleset` (`id`, `department_id`, `ruleset_code`, `ruleset_name`, `version_no`, `ruleset_status`, `created_by`) VALUES
(100000000201, 100000000111, 'CARD-BASE', '心内科基础规则', 1, 'PUBLISHED', 100000000001);

INSERT INTO `schedule_ruleset_item` (`id`, `ruleset_id`, `rule_code`, `rule_type`, `rule_scope`, `is_hard_constraint`, `priority`, `weight`, `rule_expr_json`, `status`) VALUES
(100000000211, 100000000201, 'MAX_WEEKLY_SESSIONS', 'MAX_WEEKLY_SESSIONS', 'DOCTOR', 1, 100, 1.00, JSON_OBJECT('max', 8), 'ACTIVE'),
(100000000212, 100000000201, 'REQUIRE_SENIOR', 'REQUIRE_SENIOR_COUNT', 'DEPARTMENT', 0, 50, 2.00, JSON_OBJECT('minimum', 1), 'ACTIVE');

INSERT INTO `doctor_availability_rule` (`id`, `doctor_id`, `department_id`, `weekday`, `period_code`, `clinic_type`, `is_available`, `priority`, `effective_from`, `status`) VALUES
(100000000221, 100000000121, 100000000111, 1, 1, 'GENERAL', 1, 10, CURDATE(), 'ACTIVE'),
(100000000222, 100000000121, 100000000111, 3, 1, 'GENERAL', 1, 8, CURDATE(), 'ACTIVE');

INSERT INTO `calendar_day` (`id`, `calendar_date`, `region_code`, `day_type`, `is_holiday`, `is_makeup_workday`, `holiday_name`, `status`) VALUES
(100000000231, '2026-01-01', 'CN-NATIONAL', 'HOLIDAY', 1, 0, '元旦', 'ACTIVE');

INSERT INTO `schedule_demand_template` (`id`, `department_id`, `weekday`, `period_code`, `clinic_type`, `required_doctor_count`, `required_senior_count`, `suggested_slot_count`, `min_slot_interval_minutes`, `effective_from`, `status`) VALUES
(100000000241, 100000000111, 1, 1, 'GENERAL', 1, 0, 8, 15, CURDATE(), 'ACTIVE'),
(100000000242, 100000000111, 3, 1, 'GENERAL', 1, 0, 8, 15, CURDATE(), 'ACTIVE');

INSERT INTO `schedule_generation_job` (`id`, `department_id`, `ruleset_id`, `job_type`, `start_date`, `end_date`, `solver_strategy`, `job_status`, `submitted_by`) VALUES
(100000000251, 100000000111, 100000000201, 'FORMAL', DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'RULE_GREEDY', 'SUCCEEDED', 100000000001);

INSERT INTO `schedule_generation_result` (`id`, `job_id`, `result_no`, `result_status`, `score`, `hard_violation_count`, `warning_count`, `is_selected`) VALUES
(100000000261, 100000000251, 1, 'PUBLISHED', 95.50, 0, 0, 1);

INSERT INTO `schedule_generation_assignment` (`id`, `result_id`, `doctor_id`, `department_id`, `schedule_date`, `period_code`, `clinic_type`, `suggested_start_time`, `suggested_end_time`, `suggested_fee`, `suggested_capacity`, `assignment_status`) VALUES
(100000000271, 100000000261, 100000000121, 100000000111, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 'GENERAL', '09:00:00', '11:00:00', 50.00, 4, 'PUBLISHED');

INSERT INTO `clinic_session` (`id`, `department_id`, `doctor_id`, `source_assignment_id`, `session_date`, `period_code`, `clinic_type`, `start_time`, `end_time`, `fee`, `capacity`, `remaining_count`, `session_status`, `allow_registration`, `published_at`) VALUES
(100000000301, 100000000111, 100000000121, 100000000271, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 'GENERAL', '09:00:00', '11:00:00', 50.00, 4, 4, 'OPEN', 1, NOW());

INSERT INTO `clinic_slot` (`id`, `session_id`, `slot_seq`, `slot_start_time`, `slot_end_time`, `slot_status`) VALUES
(100000000311, 100000000301, 1, '09:00:00', '09:30:00', 'FREE'),
(100000000312, 100000000301, 2, '09:30:00', '10:00:00', 'FREE'),
(100000000313, 100000000301, 3, '10:00:00', '10:30:00', 'FREE'),
(100000000314, 100000000301, 4, '10:30:00', '11:00:00', 'FREE');

INSERT INTO `knowledge_base` (`id`, `base_code`, `base_name`, `owner_type`, `embedding_model`, `vector_backend`, `status`) VALUES
(100000000401, 'SYS-MED', '系统医学知识库', 'SYSTEM', 'text-embedding-v4', 'MILVUS', 'ACTIVE');

INSERT INTO `knowledge_document` (`id`, `knowledge_base_id`, `document_uuid`, `title`, `source_uri`, `doc_type`, `category`, `ingest_status`) VALUES
(100000000411, 100000000401, 'doc-demo-001', '高血压基础宣教', 'docs/hypertension.md', 'MARKDOWN', '慢病', 'READY');

INSERT INTO `knowledge_chunk` (`id`, `document_id`, `chunk_index`, `content`, `section`, `token_count`, `vector_ref_id`) VALUES
(100000000421, 100000000411, 0, '高血压患者应规律监测血压，减少高盐饮食，遵循医生指导服药。', '生活方式管理', 32, 'vec-demo-001');

INSERT INTO `ai_session` (`id`, `session_uuid`, `patient_id`, `department_id`, `scene_type`, `session_status`, `entrypoint`, `chief_complaint_summary`, `summary`) VALUES
(100000000431, 'ai-session-demo-001', 100000000003, 100000000111, 'PRE_DIAGNOSIS', 'ACTIVE', 'JAVA', '胸闷三天', '患者主诉胸闷三天，无明确放射痛，建议进一步线下就诊。');

INSERT INTO `ai_turn` (`id`, `session_id`, `turn_no`, `turn_status`, `input_hash`) VALUES
(100000000441, 100000000431, 1, 'COMPLETED', 'input-hash-demo-001');

INSERT INTO `ai_turn_content` (`id`, `turn_id`, `content_role`, `content_encrypted`, `content_masked`, `content_hash`) VALUES
(100000000451, 100000000441, 'USER', 'ENC_USER_MESSAGE', '我最近胸闷三天', 'content-hash-demo-001');

INSERT INTO `ai_model_run` (`id`, `turn_id`, `provider_run_id`, `provider_name`, `model_name`, `trace_id`, `rag_enabled`, `retrieval_provider`, `tokens_input`, `tokens_output`, `latency_ms`, `run_status`) VALUES
(100000000461, 100000000441, 'py-run-001', 'PYTHON_AI', 'deepseek-chat', 'trace-ai-demo-001', 1, 'MILVUS', 120, 260, 980, 'SUCCEEDED');

INSERT INTO `ai_run_artifact` (`id`, `run_id`, `artifact_type`, `artifact_json`) VALUES
(100000000471, 100000000461, 'CITATION', JSON_ARRAY(JSON_OBJECT('document_uuid', 'doc-demo-001', 'section', '生活方式管理', 'score', 0.86)));

INSERT INTO `ai_guardrail_event` (`id`, `run_id`, `risk_level`, `action_taken`, `matched_rule_codes`, `input_hash`, `output_hash`) VALUES
(100000000481, 100000000461, 'MEDIUM', 'CAUTION', JSON_ARRAY('diagnosis_request'), 'input-hash-demo-001', 'output-hash-demo-001');

INSERT INTO `drug_catalog` (`id`, `drug_code`, `drug_name`, `generic_name`, `specification`, `unit`, `manufacturer`, `unit_price`, `status`) VALUES
(100000000501, 'DRUG001', '阿司匹林肠溶片', '阿司匹林', '100mg*30片', '盒', '示例药业', 18.50, 'ACTIVE');
