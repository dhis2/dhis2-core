-- DHIS2-15010
-- Make shortname mandatory/unique for the expressiondimensionitem table.

-- In expressiondimensionitem "name" and "shortname" always hold the same value. This is the current rule on the client app.
update expressiondimensionitem set shortname = name;
alter table expressiondimensionitem alter column shortname set not null;

-- Constraints. Delete and create to make them idempotent.
alter table expressiondimensionitem drop constraint if exists expressiondimensionitem_shortname_key;
alter table expressiondimensionitem drop constraint if exists expressiondimensionitem_name_key;

alter table expressiondimensionitem add constraint expressiondimensionitem_shortname_key unique (shortname);
alter table expressiondimensionitem add constraint expressiondimensionitem_name_key unique (name);
