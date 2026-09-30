-- https://dhis2.atlassian.net/browse/DHIS2-9525
-- Add new columns.

alter table externalmaplayer add column if not exists description varchar(1024);
alter table externalmaplayer add column if not exists descriptionurl varchar(255);
alter table externalmaplayer add column if not exists image varchar(2097152); -- 2mb
