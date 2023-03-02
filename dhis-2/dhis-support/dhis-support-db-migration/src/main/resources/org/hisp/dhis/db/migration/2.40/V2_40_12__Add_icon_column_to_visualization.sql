-- DHIS2-14691
alter table visualization drop column if exists icons;
alter table visualization add column if not exists icons jsonb default '[]'::jsonb;
