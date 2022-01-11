-- This script relates to the issue https://jira.dhis2.org/browse/TECH-891
-- Adds a new column "eventrepetitions" into EventVisualization table


alter table eventvisualization add column if not exists "eventrepetitions" jsonb;
