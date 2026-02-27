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

-- trackedentity: leading column lets PG jump to the right type partition when
-- data is unevenly distributed across tracked entity types.
create index in_trackedentity_type_created
    on trackedentity (trackedentitytypeid, created desc, trackedentityid desc);

-- enrollment: leading column lets PG jump to the right program partition when
-- data is unevenly distributed across programs.
create index in_enrollment_program_created
    on enrollment (programid, created desc, enrollmentid desc);

-- enrollment: order=enrolledAt is the second most common order from clients.
create index in_enrollment_program_enrollmentdate
    on enrollment (programid, enrollmentdate desc, enrollmentid desc);

-- singleevent: supports the new default order (created desc, eventid desc).
-- The existing in_singleevent_programstageid_occurreddate (V2_43_50)
-- covers order=occurredDate and stays as-is.
create index in_singleevent_programstageid_created
    on singleevent (programstageid, created desc, eventid desc);
