-- POC: Denormalize ownership org unit onto trackerevent for orgUnitMode=SELECTED performance.
--
-- Background: For tracker programs, the effective org unit for access control is stored in
-- trackedentityprogramowner (TPO), not trackerevent.organisationunitid. This forces every
-- event query to join enrollment -> trackedentityprogramowner -> organisationunit before any
-- org unit filter can be applied, preventing index use on the event table itself.
--
-- This migration adds ownerorganisationunitid directly on trackerevent, backfills it from TPO,
-- and installs two triggers to keep it in sync:
--   1. tpo_trackerevent_owner_sync   -- fires on TPO INSERT/UPDATE (ownership transfers)
--   2. trackerevent_set_owner_on_insert -- fires on trackerevent INSERT (new events)
--
-- With this column in place, orgUnitMode=SELECTED can be evaluated with a simple equality
-- predicate on trackerevent, enabling index scans without the join chain.

-- Step 1: Add the column (nullable during backfill)
alter table trackerevent add column if not exists ownerorganisationunitid bigint;

-- Step 2: Backfill from trackedentityprogramowner via enrollment.
-- Falls back to organisationunitid when no TPO record exists.
update trackerevent ev
set ownerorganisationunitid = coalesce(po.organisationunitid, ev.organisationunitid)
from enrollment en
left join trackedentityprogramowner po
    on po.trackedentityid = en.trackedentityid
    and po.programid = en.programid
where ev.enrollmentid = en.enrollmentid;

-- Step 3: Make non-nullable now that all rows are populated
alter table trackerevent alter column ownerorganisationunitid set not null;

-- Step 4: Add FK for referential integrity (consistent with organisationunitid on the same table)
alter table trackerevent
    add constraint fk_trackerevent_ownerorganisationunitid
    foreign key (ownerorganisationunitid)
    references organisationunit (organisationunitid);

-- Step 5: Index for orgUnitMode=SELECTED queries
create index if not exists in_trackerevent_owner_organisationunitid
    on trackerevent (ownerorganisationunitid);

-- Step 6: Trigger function for TPO INSERT/UPDATE (ownership transfers).
-- When a tracked entity's ownership is transferred to a new org unit, update all events
-- in their enrollments for that program.
create or replace function sync_trackerevent_owner_from_tpo() returns trigger as $$
begin
    update trackerevent ev
    set ownerorganisationunitid = new.organisationunitid
    from enrollment en
    where ev.enrollmentid = en.enrollmentid
      and en.trackedentityid = new.trackedentityid
      and en.programid = new.programid;
    return new;
end;
$$ language plpgsql;

create trigger tpo_trackerevent_owner_sync
    after insert or update on trackedentityprogramowner
    for each row execute function sync_trackerevent_owner_from_tpo();

-- Step 7: Trigger function for trackerevent INSERT (new events).
-- Populates ownerorganisationunitid at insert time from TPO, falling back to the event's own
-- organisationunitid when no TPO record exists.
create or replace function set_trackerevent_owner_on_insert() returns trigger as $$
begin
    -- Only resolve from TPO if the application layer did not already supply the value.
    -- The application layer (TrackerObjectsMapper) is the primary source; this trigger
    -- acts as a safety net for direct SQL inserts.
    if new.ownerorganisationunitid is null then
        select coalesce(po.organisationunitid, new.organisationunitid)
        into new.ownerorganisationunitid
        from enrollment en
        left join trackedentityprogramowner po
            on po.trackedentityid = en.trackedentityid
            and po.programid = en.programid
        where en.enrollmentid = new.enrollmentid;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger trackerevent_set_owner_on_insert
    before insert on trackerevent
    for each row execute function set_trackerevent_owner_on_insert();