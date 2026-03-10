-- ============================================================
-- 00-drop-all.sql  --  V3 all business tables drop script
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Audit, access log, domain events
DROP TABLE IF EXISTS `integration_event_archive`;
DROP TABLE IF EXISTS `outbox_event`;
DROP TABLE IF EXISTS `domain_event_stream`;
DROP TABLE IF EXISTS `data_access_log`;
DROP TABLE IF EXISTS `audit_payload`;
DROP TABLE IF EXISTS `audit_event`;

-- EMR and prescription
DROP TABLE IF EXISTS `medication_dispense`;
DROP TABLE IF EXISTS `prescription_snapshot`;
DROP TABLE IF EXISTS `prescription_item`;
DROP TABLE IF EXISTS `prescription_order`;
DROP TABLE IF EXISTS `emr_observation`;
DROP TABLE IF EXISTS `emr_diagnosis`;
DROP TABLE IF EXISTS `emr_record_revision`;
DROP TABLE IF EXISTS `emr_record_content`;
DROP TABLE IF EXISTS `emr_record`;
DROP TABLE IF EXISTS `drug_catalog`;

-- AI and knowledge
DROP TABLE IF EXISTS `knowledge_chunk`;
DROP TABLE IF EXISTS `knowledge_document`;
DROP TABLE IF EXISTS `knowledge_base`;
DROP TABLE IF EXISTS `ai_feedback_review`;
DROP TABLE IF EXISTS `ai_feedback_task`;
DROP TABLE IF EXISTS `ai_guardrail_event`;
DROP TABLE IF EXISTS `ai_run_artifact`;
DROP TABLE IF EXISTS `ai_model_run`;
DROP TABLE IF EXISTS `ai_turn_content`;
DROP TABLE IF EXISTS `ai_turn`;
DROP TABLE IF EXISTS `ai_session`;

-- Clinic registration
DROP TABLE IF EXISTS `visit_encounter`;
DROP TABLE IF EXISTS `slot_inventory_log`;
DROP TABLE IF EXISTS `registration_status_log`;
DROP TABLE IF EXISTS `slot_active_claim`;
DROP TABLE IF EXISTS `registration_order`;
DROP TABLE IF EXISTS `registration_hold`;
DROP TABLE IF EXISTS `clinic_slot`;
DROP TABLE IF EXISTS `clinic_session`;

-- Scheduling planning
DROP TABLE IF EXISTS `schedule_generation_assignment`;
DROP TABLE IF EXISTS `schedule_generation_result`;
DROP TABLE IF EXISTS `schedule_generation_job`;
DROP TABLE IF EXISTS `schedule_demand_override`;
DROP TABLE IF EXISTS `schedule_demand_template`;
DROP TABLE IF EXISTS `calendar_day`;
DROP TABLE IF EXISTS `doctor_unavailability`;
DROP TABLE IF EXISTS `doctor_availability_rule`;
DROP TABLE IF EXISTS `schedule_ruleset_item`;
DROP TABLE IF EXISTS `schedule_ruleset`;

-- Organization
DROP TABLE IF EXISTS `doctor_department_rel`;
DROP TABLE IF EXISTS `doctors`;
DROP TABLE IF EXISTS `departments`;
DROP TABLE IF EXISTS `hospitals`;

-- Auth and identity
DROP TABLE IF EXISTS `patient_profile`;
DROP TABLE IF EXISTS `user_pii_profile`;
DROP TABLE IF EXISTS `data_scope_rules`;
DROP TABLE IF EXISTS `role_permissions`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `permissions`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `users`;

-- V2 compatibility cleanup
DROP TABLE IF EXISTS `domain_events`;
DROP TABLE IF EXISTS `prescription_items`;
DROP TABLE IF EXISTS `prescriptions`;
DROP TABLE IF EXISTS `drugs`;
DROP TABLE IF EXISTS `medical_record_versions`;
DROP TABLE IF EXISTS `medical_records`;
DROP TABLE IF EXISTS `knowledge_chunks`;
DROP TABLE IF EXISTS `knowledge_documents`;
DROP TABLE IF EXISTS `ai_feedback_reviews`;
DROP TABLE IF EXISTS `ai_messages`;
DROP TABLE IF EXISTS `ai_conversations`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `schedule_rule_profile`;
DROP TABLE IF EXISTS `schedule_plan_items`;
DROP TABLE IF EXISTS `schedule_plan`;
DROP TABLE IF EXISTS `appointment_slots`;
DROP TABLE IF EXISTS `doctor_schedules`;
DROP TABLE IF EXISTS `department_schedule_demand`;
DROP TABLE IF EXISTS `doctor_time_off`;
DROP TABLE IF EXISTS `doctor_availability_rules`;
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `test_connections`;
DROP TABLE IF EXISTS `schedule_events`;
DROP TABLE IF EXISTS `appointment_events`;
DROP TABLE IF EXISTS `schedule_plan_constraint_snapshot`;
DROP TABLE IF EXISTS `schedule_exceptions`;
DROP TABLE IF EXISTS `schedule_template_rules`;
DROP TABLE IF EXISTS `schedule_templates`;
DROP TABLE IF EXISTS `ai_metrics_daily`;
DROP TABLE IF EXISTS `ai_metrics_dept_daily`;

SET FOREIGN_KEY_CHECKS = 1;
