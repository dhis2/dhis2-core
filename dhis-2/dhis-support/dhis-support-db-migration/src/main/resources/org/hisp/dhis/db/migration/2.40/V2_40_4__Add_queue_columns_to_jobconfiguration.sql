alter table jobconfiguration add column if not exists queuename character varying(50);
alter table jobconfiguration add column if not exists queueposition integer;