-- https://dhis2.atlassian.net/browse/DHIS2-16369
-- Adds a new column "sorting" into Visualization table.

alter table visualization add column if not exists "sorting" jsonb default '[]'::jsonb;
