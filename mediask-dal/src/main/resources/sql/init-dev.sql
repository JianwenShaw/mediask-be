-- ============================================================
-- MediAsk V3 development database bootstrap
-- ============================================================
-- Usage:
--   source mediask-dal/src/main/resources/sql/init-dev.sql
--
-- File layout (57 tables):
--   00-drop-all.sql       Drop V3 and legacy V2 tables
--   01-base-auth.sql      Auth and identity (8)
--   02-hospital-org.sql   Organization and doctor roster (4)
--   03-scheduling.sql     Scheduling planning (10)
--   04-appointment.sql    Clinic sessions and registration (8)
--   05-ai.sql             AI session and knowledge (11)
--   06-medical.sql        EMR and prescription (10)
--   07-domain-events.sql  Audit, access log, outbox, events (6)
--   99-seed-data.sql      Optional local seed data

SOURCE mediask-dal/src/main/resources/sql/00-drop-all.sql;
SOURCE mediask-dal/src/main/resources/sql/01-base-auth.sql;
SOURCE mediask-dal/src/main/resources/sql/02-hospital-org.sql;
SOURCE mediask-dal/src/main/resources/sql/03-scheduling.sql;
SOURCE mediask-dal/src/main/resources/sql/04-appointment.sql;
SOURCE mediask-dal/src/main/resources/sql/05-ai.sql;
SOURCE mediask-dal/src/main/resources/sql/06-medical.sql;
SOURCE mediask-dal/src/main/resources/sql/07-domain-events.sql;
SOURCE mediask-dal/src/main/resources/sql/99-seed-data.sql;
