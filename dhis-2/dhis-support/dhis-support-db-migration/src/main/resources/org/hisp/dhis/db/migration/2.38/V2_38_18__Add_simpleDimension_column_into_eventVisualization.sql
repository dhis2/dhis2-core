-- This script relates to the issue https://jira.dhis2.org/browse/TECH-795
-- Adds a new column "simpledimensions" into the EventReport and EventVisualization tables


alter table eventvisualization add column if not exists "simpledimensions" jsonb;
alter table eventreport add column if not exists "simpledimensions" jsonb;
