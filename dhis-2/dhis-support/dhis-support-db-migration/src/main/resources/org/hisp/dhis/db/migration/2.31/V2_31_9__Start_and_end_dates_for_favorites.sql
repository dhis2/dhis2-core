
-- Add start and end date to reporttable
alter table reporttable add column if not exists startdate timestamp;
alter table reporttable add column if not exists enddate timestamp;

-- Add start and end date to chart
alter table chart add column if not exists startdate timestamp;
alter table chart add column if not exists enddate timestamp;
