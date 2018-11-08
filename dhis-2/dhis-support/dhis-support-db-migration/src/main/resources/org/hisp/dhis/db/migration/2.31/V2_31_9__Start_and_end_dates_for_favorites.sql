
-- Add start and end date to reporttable
alter table reporttable add column startdate timestamp;
alter table reporttable add column enddate timestamp;

-- Add start and end date to chart
alter table chart add column startdate timestamp;
alter table chart add column enddate timestamp;
