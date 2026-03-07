-- ============================================================
-- MediAsk 开发环境数据库初始化脚本（orchestrator）
-- ============================================================
-- 用法：
--   方式1（MySQL命令行）: source mediask-dal/src/main/resources/sql/init-dev.sql
--   方式2（按需执行子文件）: 手动按顺序 source 各子文件
--
-- 本脚本按依赖顺序加载所有子模块 SQL 文件，完成从零建库。
-- 执行前请确保已创建并选择了目标数据库（CREATE DATABASE IF NOT EXISTS mediask_dev）。
--
-- 文件清单（32张表）：
--   00-drop-all.sql       清除所有表（含旧表兼容清理）
--   01-base-auth.sql      认证授权（8表）: test_connections, users, roles, permissions,
--                          user_roles, role_permissions, data_scope_rules, audit_logs
--   02-hospital-org.sql   医院组织（3表）: hospitals, departments, doctors
--   03-scheduling.sql     排班管理（9表）: doctor_availability_rules, doctor_time_off,
--                          department_schedule_demand, calendar_day, doctor_schedules,
--                          appointment_slots, schedule_plan, schedule_plan_items,
--                          schedule_rule_profile
--   04-appointment.sql    预约挂号（1表）: appointments
--   05-ai.sql             AI问诊（5表）:  ai_conversations, ai_messages,
--                          ai_feedback_reviews, knowledge_documents, knowledge_chunks
--   06-medical.sql        病历处方（5表）: medical_records, medical_record_versions,
--                          drugs, prescriptions, prescription_items
--   07-domain-events.sql  领域事件（1表）: domain_events
--   99-seed-data.sql      测试种子数据

SOURCE mediask-dal/src/main/resources/sql/00-drop-all.sql;
SOURCE mediask-dal/src/main/resources/sql/01-base-auth.sql;
SOURCE mediask-dal/src/main/resources/sql/02-hospital-org.sql;
SOURCE mediask-dal/src/main/resources/sql/03-scheduling.sql;
SOURCE mediask-dal/src/main/resources/sql/04-appointment.sql;
SOURCE mediask-dal/src/main/resources/sql/05-ai.sql;
SOURCE mediask-dal/src/main/resources/sql/06-medical.sql;
SOURCE mediask-dal/src/main/resources/sql/07-domain-events.sql;
SOURCE mediask-dal/src/main/resources/sql/99-seed-data.sql;
