-- Denormalize ownership org unit onto trackerevent for orgUnitMode=SELECTED performance.
--
-- Background: For tracker programs, the effective org unit for access control is stored in
-- trackedentityprogramowner (TPO), not trackerevent.organisationunitid. This forces every
-- event query to join enrollment -> trackedentityprogramowner -> organisationunit before any
-- org unit filter can be applied, preventing index use on the event table itself.
--
-- This migration adds ownerorganisationunitid directly on trackerevent and backfills it from TPO.
-- The application layer (TrackerObjectsMapper) sets this column on every new event insert, and
-- DefaultTrackedEntityProgramOwnerService.updateTrackedEntityProgramOwner syncs it on ownership
-- transfers. No DB triggers are used.
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

