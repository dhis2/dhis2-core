alter table visualization add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table map add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table eventreport add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table eventchart add column if not exists attributevalues jsonb default '{}'::jsonb;

alter table attribute add column if not exists visualizationAttribute boolean;
alter table attribute add column if not exists mapAttribute boolean;
alter table attribute add column if not exists eventReportAttribute boolean;
alter table attribute add column if not exists eventChartAttribute boolean;
