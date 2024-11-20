DO $$
DECLARE dummyOrgUnitId bigint;
DECLARE dummyOrgUnitUid varchar(11);
BEGIN
    select coalesce((select max(organisationunitid) + 1 from organisationunit), 1) into dummyOrgUnitId;
    select generate_uid() into dummyOrgUnitUid;

    while (select count(*) from organisationunit where uid = dummyOrgUnitUid) > 0 loop
        select generate_uid() into dummyOrgUnitUid;
    end loop;

    insert into organisationunit
    (organisationunitid, name, code, parentid, shortname, openingdate, created, lastupdated, uid, hierarchylevel)
    values
        (dummyOrgUnitId,
         'DUMMY OU',
         'DUMMY_OU_CODE',
         null,
         'DUMMY OU',
         '1970-01-01',
         now(),
         now(),
         dummyOrgUnitUid,
         0);

    -- Update null organisation unit of enrollments to dummy organisation unit
    update enrollment en set organisationunitid = dummyOrgUnitId
    where en.organisationunitid is null;

    alter table enrollment alter column organisationunitid set not null;

    -- Update null organisation unit of event to dummy organisation unit
    update event ev set organisationunitid = dummyOrgUnitId
    where ev.organisationunitid is null;

    alter table event alter column organisationunitid set not null;
END $$;