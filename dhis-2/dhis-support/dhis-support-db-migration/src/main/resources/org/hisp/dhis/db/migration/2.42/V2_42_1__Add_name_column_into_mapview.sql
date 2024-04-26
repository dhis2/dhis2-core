-- DHIS2-16088: Adds a "name" column to the table mapview.
alter table mapview add column if not exists name varchar(230) null;
