DO $$
BEGIN
    -- Set null organisation unit of dummy enrollments to root organisation unit
    update enrollment en
    set organisationunitid = (select organisationunitid from organisationunit order by hierarchylevel limit 1)
    where en.organisationunitid is null
      and en.programid in (select programid from program where type = 'WITHOUT_REGISTRATION');

    alter table if exists enrollment alter column organisationunitid set not null;

    alter table if exists event alter column organisationunitid set not null;

EXCEPTION
  WHEN not_null_violation THEN
    RAISE EXCEPTION 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.42/migration-notes.md#null-organisation-unit to have more information on the issue and to find ways to fix it. Detailed error message: %', SQLERRM;

END;
$$;