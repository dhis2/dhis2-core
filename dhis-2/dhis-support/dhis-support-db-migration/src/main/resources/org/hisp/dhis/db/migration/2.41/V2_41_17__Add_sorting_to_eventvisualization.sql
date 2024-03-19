-- https://dhis2.atlassian.net/browse/DHIS2-14956
-- Adds a new column "sorting" into EventVisualization table.

alter table eventvisualization add column if not exists "sorting" jsonb default '[]'::jsonb;
