-- DHIS2-15106
-- Remove name and shortname uniqueness for the expressiondimensionitem table.

alter table expressiondimensionitem drop constraint if exists expressiondimensionitem_shortname_key;
alter table expressiondimensionitem drop constraint if exists expressiondimensionitem_name_key;
