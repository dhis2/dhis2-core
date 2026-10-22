-- Makes eventvisualization.uid unique.
-- If duplicates still exist, raise a warning and leave the table untouched instead of failing the migration.
-- Admins can fix the data and then create the index manually:
--   create unique index uk_eventvisualization_uid on eventvisualization (uid);

do $$

declare
    duplicate_count integer;
begin
    -- index already present, nothing to do
    if exists (
        select 1
        from pg_indexes
        where tablename = 'eventvisualization'
          and indexname = 'uk_eventvisualization_uid'
    ) then
        return;
    end if;

    select count(*)
    into duplicate_count
    from (
        select uid
        from eventvisualization
        where uid is not null
        group by uid
        having count(*) > 1
    ) duplicates;

    if duplicate_count > 0 then
        raise warning 'Column uid of table eventvisualization was not made unique: % uid(s) are used by more than one row. Find them with: select uid, count(*) from eventvisualization group by uid having count(*) > 1;', duplicate_count;
        return;
    end if;

    create unique index uk_eventvisualization_uid on eventvisualization (uid);
end $$;
