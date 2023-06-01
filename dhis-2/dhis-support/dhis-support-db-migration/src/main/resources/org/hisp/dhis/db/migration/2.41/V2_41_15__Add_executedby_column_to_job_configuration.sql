/* optional user UID of the user that executes the job */
alter table jobconfiguration add column if not exists executedby character varying(11);