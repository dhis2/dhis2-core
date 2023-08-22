-- https://dhis2.atlassian.net/browse/DHIS2-15689
-- Adds a new column "skiprounding" into EventVisualization table.

alter table eventvisualization add column if not exists "skiprounding" boolean;
