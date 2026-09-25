-- https://dhis2.atlassian.net/browse/DHIS2-9525
-- Add new columns.

alter table externalmaplayer add column if not exists description text;
alter table externalmaplayer add column if not exists descriptionurl text;
