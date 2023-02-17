-- DHIS2-14305: Adds a new column to the table mapview.
alter table mapview add column if not exists labeltemplate varchar(50) null;
