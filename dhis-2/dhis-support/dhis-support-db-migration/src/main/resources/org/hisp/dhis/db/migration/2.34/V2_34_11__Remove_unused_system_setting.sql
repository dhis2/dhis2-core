-- Removes unused SystemSetting which cause failure of Tracker app.
-- KEY_SCHED_TASKS("keySchedTasks")

delete from systemsetting where name = 'keySchedTasks';