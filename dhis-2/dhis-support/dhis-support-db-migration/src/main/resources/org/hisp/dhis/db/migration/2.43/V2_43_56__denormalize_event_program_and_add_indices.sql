-- Add indices to support the new default order (created desc with PK desc as
-- tie-breaker). The default order changed from PK desc since clients mostly
-- request createdAt:desc (capture and android both default to it).
--
-- All indices are non-partial so they work regardless of the deleted filter.

-- drop redundant/replaced indices
drop index if exists in_trackedentityinstance_created;
drop index if exists in_trackedentityinstance_deleted;
drop index if exists in_programinstance_programid;
drop index if exists in_programinstance_deleted;

-- 1. trackedentity: leading column lets PG jump to the right type partition when
-- data is unevenly distributed across tracked entity types.
drop index if exists in_trackedentity_type_created;
create index in_trackedentity_type_created
    on trackedentity (trackedentitytypeid, created desc, trackedentityid desc);

-- 2. enrollment: leading column lets PG jump to the right program partition when
-- data is unevenly distributed across programs.
drop index if exists in_enrollment_program_created;
create index in_enrollment_program_created
    on enrollment (programid, created desc, enrollmentid desc);
drop index if exists in_enrollment_program_enrollmentdate;
create index in_enrollment_program_enrollmentdate
    on enrollment (programid, enrollmentdate desc, enrollmentid desc);

-- 3. trackerevent: Denormalize programid to eliminate the join to enrollment
-- for program context. This fixes the 'Join Trap' where the planner fails to
-- use indices for ordering when filtering by program via a join.
--
-- The programid prefix is superior to programstageid because it allows a
-- single-pass ordered scan for any number of program stages.

-- add programid column if not exists
alter table trackerevent add column if not exists programid bigint;

-- idempotent backfill: join to programstage (metadata) is significantly faster
-- than joining to the enrollment table.
update trackerevent ev
set programid = ps.programid
from programstage ps
where ev.programstageid = ps.programstageid
  and ev.programid is null;

-- add FK and NOT NULL constraint
alter table trackerevent alter column programid set not null;
alter table trackerevent
    add constraint fk_trackerevent_programid foreign key (programid)
    references program (programid);

-- drop unused/redundant event indices
drop index if exists in_trackerevent_status_occurreddate; -- replaced by program-scoped index
drop index if exists in_trackerevent_programstageid_created; -- replaced by program-scoped index
drop index if exists in_trackerevent_occurreddate; -- replaced by program-scoped index
drop index if exists in_trackerevent_deleted_assigneduserid; -- replaced by program-scoped index

-- add new composite indices with programid prefix for optimal filtering + ordering
drop index if exists in_trackerevent_program_created;
create index in_trackerevent_program_created
    on trackerevent (programid, created desc, eventid desc);

-- support fast order=occurredAt:desc within a program
drop index if exists in_trackerevent_program_occurreddate;
create index in_trackerevent_program_occurreddate
    on trackerevent (programid, occurreddate desc, eventid desc);

-- support assigned user filtering within a program.
drop index if exists in_trackerevent_program_assigneduser;
create index in_trackerevent_program_assigneduser
    on trackerevent (programid, assigneduserid);

-- 4. singleevent: the existing in_singleevent_programstageid_occurreddate
-- (V2_43_50) covers order=occurredDate and stays as-is.
drop index if exists in_singleevent_programstageid_created;
create index in_singleevent_programstageid_created
    on singleevent (programstageid, created desc, eventid desc);
