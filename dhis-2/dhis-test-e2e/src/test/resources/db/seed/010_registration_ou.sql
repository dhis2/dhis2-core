-- REGISTRATION_OU fixture (DHIS2-21980): six entities, each with one enrollment and event.
-- A separate program keeps existing test counts unchanged. Different district counts
-- expose queries that mix up registration, enrollment and event org units:
--
-- Entities   Registration   Enrollment   Event
-- 001        Bo             Bombali      Kailahun
-- 002-003    Bombali        Kailahun     Bo
-- 004-006    Kailahun       Bo           Bombali
--
-- Clone demo rows to retain required defaults across schema versions.
-- Fixed 2022 dates keep the records eligible for analytics export.

-- Tracked entity type.

create temporary table seed_regou_tet as
select * from trackedentitytype where uid = 'UinS6TQnkUi';

update seed_regou_tet
set trackedentitytypeid = 9100001,
    uid = 'regOuTetp01',
    code = 'REGOU_TET',
    name = 'Registration OU test entity',
    shortname = 'RegOu TET',
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into trackedentitytype
select * from seed_regou_tet
where not exists (select 1 from program where uid = 'regOuProg01');

-- Program, based on Child Programme.

create temporary table seed_regou_program as
select * from program where uid = 'IpHINAT79UW';

update seed_regou_program
set programid = 9100002,
    uid = 'regOuProg01',
    code = 'REGOU_PRG',
    name = 'Registration OU test program',
    shortname = 'RegOu program',
    description = 'Fixture for REGISTRATION_OU analytics coverage',
    trackedentitytypeid = 9100001,
    version = 1,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into program
select * from seed_regou_program
where not exists (select 1 from program where uid = 'regOuProg01');

-- Program stage, based on Birth.

create temporary table seed_regou_stage as
select * from programstage where uid = 'A03MvHHogjR';

update seed_regou_stage
set programstageid = 9100003,
    uid = 'regOuStge01',
    code = 'REGOU_PS',
    name = 'Registration OU test stage',
    programid = 9100002,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into programstage
select * from seed_regou_stage
where not exists (select 1 from programstage where uid = 'regOuStge01');

-- Assign the program to the three facilities.
insert into program_organisationunits (programid, organisationunitid)
select 9100002, ou.organisationunitid
from organisationunit ou
where ou.uid in ('aBfyTU5Wgds', 'agEKP19IUKI', 'a1E6QWBTEwX')
  and not exists (select 1
                  from program_organisationunits po
                  where po.programid = 9100002
                    and po.organisationunitid = ou.organisationunitid);

-- Registration org units.

create temporary table seed_regou_te as
select t.*, v.new_id, v.new_uid, v.ou_uid
from (select * from trackedentity where uid = 'uhubxsfLanV') t
cross join (values (9100011, 'regOuTei001', 'aBfyTU5Wgds'),   -- registered in Bo
                   (9100012, 'regOuTei002', 'agEKP19IUKI'),   -- registered in Bombali
                   (9100013, 'regOuTei003', 'agEKP19IUKI'),
                   (9100014, 'regOuTei004', 'a1E6QWBTEwX'),   -- registered in Kailahun
                   (9100015, 'regOuTei005', 'a1E6QWBTEwX'),
                   (9100016, 'regOuTei006', 'a1E6QWBTEwX')) as v(new_id, new_uid, ou_uid);

update seed_regou_te s
set trackedentityid = s.new_id,
    uid = s.new_uid,
    code = s.new_uid,
    trackedentitytypeid = 9100001,
    organisationunitid = (select organisationunitid from organisationunit where uid = s.ou_uid),
    deleted = false,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

alter table seed_regou_te drop column new_id, drop column new_uid, drop column ou_uid;

insert into trackedentity
select * from seed_regou_te
where not exists (select 1 from trackedentity where uid = 'regOuTei001');

-- Enrollments in a different district from registration.

create temporary table seed_regou_enrollment as
select e.*, v.new_id, v.new_uid, v.te_id, v.ou_uid
from (select * from enrollment where uid = 'KxXkjF6buFN') e
cross join (values (9100021, 'regOuEnr001', 9100011, 'agEKP19IUKI'),   -- enrolled in Bombali
                   (9100022, 'regOuEnr002', 9100012, 'a1E6QWBTEwX'),   -- enrolled in Kailahun
                   (9100023, 'regOuEnr003', 9100013, 'a1E6QWBTEwX'),
                   (9100024, 'regOuEnr004', 9100014, 'aBfyTU5Wgds'),   -- enrolled in Bo
                   (9100025, 'regOuEnr005', 9100015, 'aBfyTU5Wgds'),
                   (9100026, 'regOuEnr006', 9100016, 'aBfyTU5Wgds')) as v(new_id, new_uid, te_id, ou_uid);

update seed_regou_enrollment s
set enrollmentid = s.new_id,
    uid = s.new_uid,
    programid = 9100002,
    trackedentityid = s.te_id,
    organisationunitid = (select organisationunitid from organisationunit where uid = s.ou_uid),
    enrollmentdate = timestamp '2022-03-01 00:00:00',
    occurreddate = timestamp '2022-03-01 00:00:00',
    status = 'ACTIVE',
    deleted = false,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

alter table seed_regou_enrollment
  drop column new_id, drop column new_uid, drop column te_id, drop column ou_uid;

insert into enrollment
select * from seed_regou_enrollment
where not exists (select 1 from enrollment where uid = 'regOuEnr001');

-- Events in the third district.

create temporary table seed_regou_event as
select e.*, v.new_id, v.new_uid, v.en_id, v.ou_uid
from (select * from trackerevent where uid = 'MyWQlBkftni') e
cross join (values (9100031, 'regOuEvt001', 9100021, 'a1E6QWBTEwX'),   -- served in Kailahun
                   (9100032, 'regOuEvt002', 9100022, 'aBfyTU5Wgds'),   -- served in Bo
                   (9100033, 'regOuEvt003', 9100023, 'aBfyTU5Wgds'),
                   (9100034, 'regOuEvt004', 9100024, 'agEKP19IUKI'),   -- served in Bombali
                   (9100035, 'regOuEvt005', 9100025, 'agEKP19IUKI'),
                   (9100036, 'regOuEvt006', 9100026, 'agEKP19IUKI')) as v(new_id, new_uid, en_id, ou_uid);

update seed_regou_event s
set eventid = s.new_id,
    uid = s.new_uid,
    enrollmentid = s.en_id,
    programstageid = 9100003,
    organisationunitid = (select organisationunitid from organisationunit where uid = s.ou_uid),
    occurreddate = timestamp '2022-06-15 00:00:00',
    status = 'COMPLETED',
    deleted = false,
    eventdatavalues = '{}'::jsonb,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

alter table seed_regou_event
  drop column new_id, drop column new_uid, drop column en_id, drop column ou_uid;

insert into trackerevent
select * from seed_regou_event
where not exists (select 1 from trackerevent where uid = 'regOuEvt001');

-- Reserve the fixture IDs and remove temporary tables.

select setval('hibernate_sequence',
              greatest((select last_value from hibernate_sequence), 9200000));

drop table seed_regou_tet;
drop table seed_regou_program;
drop table seed_regou_stage;
drop table seed_regou_te;
drop table seed_regou_enrollment;
drop table seed_regou_event;
