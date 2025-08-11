update dashboarditem set translations = '[]'::jsonb where translations is null;
alter table dashboarditem alter column translations set default '[]'::jsonb;