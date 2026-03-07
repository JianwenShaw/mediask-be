-- ============================================================
-- 00-drop-all.sql  —  按依赖逆序清除所有业务表
-- ============================================================
-- 每次执行初始化时先 SOURCE 本文件，确保干净重建。

SET FOREIGN_KEY_CHECKS = 0;

-- 领域事件
DROP TABLE IF EXISTS `domain_events`;

-- 病历与处方
DROP TABLE IF EXISTS `prescription_items`;
DROP TABLE IF EXISTS `prescriptions`;
DROP TABLE IF EXISTS `drugs`;
DROP TABLE IF EXISTS `medical_record_versions`;
DROP TABLE IF EXISTS `medical_records`;

-- AI 问诊
DROP TABLE IF EXISTS `knowledge_chunks`;
DROP TABLE IF EXISTS `knowledge_documents`;
DROP TABLE IF EXISTS `ai_feedback_reviews`;
DROP TABLE IF EXISTS `ai_messages`;
DROP TABLE IF EXISTS `ai_conversations`;

-- 预约挂号
DROP TABLE IF EXISTS `appointments`;

-- 排班管理
DROP TABLE IF EXISTS `schedule_rule_profile`;
DROP TABLE IF EXISTS `schedule_plan_items`;
DROP TABLE IF EXISTS `schedule_plan`;
DROP TABLE IF EXISTS `appointment_slots`;
DROP TABLE IF EXISTS `doctor_schedules`;
DROP TABLE IF EXISTS `calendar_day`;
DROP TABLE IF EXISTS `department_schedule_demand`;
DROP TABLE IF EXISTS `doctor_time_off`;
DROP TABLE IF EXISTS `doctor_availability_rules`;

-- 医院组织
DROP TABLE IF EXISTS `doctors`;
DROP TABLE IF EXISTS `departments`;
DROP TABLE IF EXISTS `hospitals`;

-- 权限与审计
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `data_scope_rules`;
DROP TABLE IF EXISTS `role_permissions`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `permissions`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `test_connections`;

-- 旧表清理（本次重构已删除，兼容旧库升级）
DROP TABLE IF EXISTS `schedule_events`;
DROP TABLE IF EXISTS `appointment_events`;
DROP TABLE IF EXISTS `schedule_plan_constraint_snapshot`;
DROP TABLE IF EXISTS `schedule_exceptions`;
DROP TABLE IF EXISTS `schedule_template_rules`;
DROP TABLE IF EXISTS `schedule_templates`;
DROP TABLE IF EXISTS `ai_metrics_daily`;
DROP TABLE IF EXISTS `ai_metrics_dept_daily`;

SET FOREIGN_KEY_CHECKS = 1;
