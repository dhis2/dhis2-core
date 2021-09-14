alter table visualization add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table map add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table eventreport add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table eventchart add column if not exists attributevalues jsonb default '{}'::jsonb;

alter table attribute add column if not exists visualizationAttribute boolean not null default false;
alter table attribute add column if not exists mapAttribute boolean not null default false;
alter table attribute add column if not exists eventReportAttribute boolean not null default false;
alter table attribute add column if not exists eventChartAttribute boolean not null default false;
