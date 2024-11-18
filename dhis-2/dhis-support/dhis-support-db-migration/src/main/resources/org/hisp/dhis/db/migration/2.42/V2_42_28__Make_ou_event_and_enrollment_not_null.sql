-- Delete invalid events linked to invalid enrollments.
delete from event e where e.enrollmentid in (
        select en.enrollmentid
        from enrollment en join program p on en.programid = p.programid
        where p.type = 'WITH_REGISTRATION'
        and en.trackedentityid is null
    );

-- Delete invalid enrollments that are part of a tracker program and
-- have null tracked entity.
delete from enrollment en where en.programid in (
        select p.programid
        from program p
        where p.type = 'WITH_REGISTRATION'
)
and en.trackedentityid is null;

-- Update null organisation unit of enrollments to organisation unit of one of its events
update enrollment en set organisationunitid =
         (select distinct ev.organisationunitid
                  from event ev
                  where en.enrollmentid = ev.enrollmentid
                    and ev.organisationunitid is not null
                  limit 1)
where en.organisationunitid is null;

-- If organisationunitid column is still null for any placeholder enrollment,
-- update organisation unit to root organisation unit.
-- Placeholder enrollments do not have a tracked entity, so we need to use one well-know
-- organisation unit.
-- Organisation unit for this enrollments is never used anyway but we still need to fill in a value.
update enrollment en set organisationunitid = (
        select distinct organisationunitid
        from organisationunit
        where parentid is null
        limit 1
    )
where en.organisationunitid is null
and (select type from program p where en.programid = p.programid) = 'WITHOUT_REGISTRATION';

-- If organisationunitid column is still null for any enrollment that is not a placeholder,
-- use the tracked entity organisation unit.
-- Tracked entity organisation unit is guaranteed to be not null.
update enrollment en set organisationunitid =
         (select te.organisationunitid
          from trackedentity te
          where en.trackedentityid = te.trackedentityid)
where en.organisationunitid is null
and (select type from program p where en.programid = p.programid) = 'WITH_REGISTRATION';

alter table enrollment alter column organisationunitid set not null;

-- Update null organisation unit of event to organisation unit the enrollment
-- that at this point is guaranteed to be not null.
update event ev set organisationunitid = (
        select organisationunitid
        from enrollment en
        where en.enrollmentid = ev.enrollmentid)
where ev.organisationunitid is null;

alter table event alter column organisationunitid set not null;