-- https://dhis2.atlassian.net/browse/DHIS2-15689
-- Adds a new column "skiprounding" into EventVisualization table.

-- Our standard approach is to always default boolean types to "true" or "false" to avoid conversion problems.
alter table eventvisualization add column if not exists "skiprounding" boolean not null default false;
