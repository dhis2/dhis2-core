alter table programnotificationtemplate add column if not exists translations jsonb default '[]'::jsonb;
alter table datasetnotificationtemplate add column if not exists translations jsonb default '[]'::jsonb;
alter table validationnotificationtemplate add column if not exists translations jsonb default '[]'::jsonb;
