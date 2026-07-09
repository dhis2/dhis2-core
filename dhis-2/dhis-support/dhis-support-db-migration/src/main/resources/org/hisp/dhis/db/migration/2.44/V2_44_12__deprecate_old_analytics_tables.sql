-- It relates to https://dhis2.atlassian.net/browse/DHIS2-21273
-- Rename/deprecated old analytics resource tables, so it fails fast if someone is still pointing to them.

do $$
declare
tbl text;
    new_name text;
begin
for tbl in
    	-- List of possible analytics resource tables.
select unnest(array[
                  '_categoryoptioncomboname',
              '_categorystructure',
              '_dataelementcategoryoptioncombo',
              '_dataelementgroupsetstructure',
              '_dataelementstructure',
              '_datasetorganisationunitcategory',
              '_dateperiodstructure',
              '_indicatorgroupsetstructure',
              '_organisationunitgroupsetstructure',
              '_orgunitstructure',
              '_periodstructure',
              '_relationship'
                  ])
           loop
       -- Build the new name by replacing the leading underscore with "_deprecated".
    new_name := '_deprecated_' || substr(tbl, 2);

-- Only rename/deprecate if the table actually exists.
if exists (
            select 1
            from pg_tables
            where schemaname = 'public'
              and tablename = tbl
        ) then
            execute format('alter table public.%I rename to %I;', tbl, new_name);
end if;
end loop;
end $$;
