-- Isolated DHIS2-21967 fixtures. Clone metadata defaults from the Sierra Leone test dump,
-- but use a dedicated TET/program/DE so existing analytics snapshots keep their data.
-- Existing org units are referenced without modification. Safe to apply repeatedly.
insert into trackedentitytype
select (jsonb_populate_record(null::trackedentitytype, to_jsonb(t) || jsonb_build_object(
    'trackedentitytypeid', nextval('hibernate_sequence'), 'uid', 'r21967Tet01',
    'code', null, 'name', 'TE aggregate scoped regression', 'shortname', 'TE aggregate regression',
    'created', timestamp '2021-01-01', 'lastupdated', timestamp '2021-01-01'))).*
from trackedentitytype t where t.uid = 'nEenWmSyUEp'
on conflict (uid) do nothing;

insert into program
select (jsonb_populate_record(null::program, to_jsonb(p) || jsonb_build_object(
    'programid', nextval('hibernate_sequence'), 'uid', 'r21967Prg01', 'code', null,
    'name', 'TE aggregate scoped regression', 'shortname', 'TE aggregate regression',
    'trackedentitytypeid', t.trackedentitytypeid, 'onlyenrollonce', false,
    'dataentryformid', null, 'relatedprogramid', null))).*
from program p cross join trackedentitytype t
where p.uid = 'IpHINAT79UW' and t.uid = 'r21967Tet01'
on conflict (uid) do nothing;

insert into programstage
select (jsonb_populate_record(null::programstage, to_jsonb(s) || jsonb_build_object(
    'programstageid', nextval('hibernate_sequence'), 'uid', v.uid, 'code', null,
    'name', v.name, 'programid', p.programid, 'repeatable', true,
    'autogenerateevent', false, 'dataentryformid', null, 'dataentryform', null,
    'nextscheduledateid', null, 'sort_order', v.sort_order))).*
from programstage s cross join program p cross join
    (values ('r21967Stg01', 'Grouped visits', 1), ('r21967Stg02', 'Independent visits', 2))
    v(uid, name, sort_order)
where s.uid = 'A03MvHHogjR' and p.uid = 'r21967Prg01'
on conflict (uid) do nothing;

insert into dataelement
select (jsonb_populate_record(null::dataelement, to_jsonb(d) || jsonb_build_object(
    'dataelementid', nextval('hibernate_sequence'), 'uid', 'r21967De001', 'code', null,
    'name', 'TE aggregate regression weight', 'shortname', 'TE regression weight',
    'valuetype', 'NUMBER', 'optionsetid', null, 'commentoptionsetid', null))).*
from dataelement d where d.uid = 'UXz7xuGCEhU'
on conflict (uid) do nothing;

insert into programstagedataelement
    (programstagedataelementid, uid, created, lastupdated, programstageid, dataelementid,
     compulsory, sort_order, displayinreports, skipsynchronization, skipanalytics)
select nextval('hibernate_sequence'), v.uid, timestamp '2021-01-01', timestamp '2021-01-01',
    s.programstageid, d.dataelementid, false, 1, true, false, false
from (values ('r21967Stg01', 'r21967Pde01'), ('r21967Stg02', 'r21967Pde02')) v(stage, uid)
join programstage s on s.uid = v.stage cross join dataelement d
where d.uid = 'r21967De001'
on conflict (uid) do nothing;

insert into program_organisationunits (programid, organisationunitid)
select p.programid, o.organisationunitid from program p cross join organisationunit o
where p.uid = 'r21967Prg01' and o.uid in ('QII5GqfDfO3', 'DiszpKrYNg8')
on conflict do nothing;

-- TE 1 has two enrollments and moves from OU A to OU B; its latest event has no weight.
-- TEs 2/3 provide separate timestamps on the same day, TE 4/5 test the filter boundaries.
-- TE 6 has no events. TE 7 has two events tied on timestamp with different coordinates.
insert into trackedentity
    (trackedentityid, uid, organisationunitid, trackedentitytypeid, created, lastupdated,
     createdatclient, lastupdatedatclient, inactive, deleted, featuretype)
select nextval('trackedentity_sequence'), v.uid, o.organisationunitid, t.trackedentitytypeid,
    timestamp '2021-01-01', timestamp '2022-01-01', timestamp '2021-01-01',
    timestamp '2022-01-01', false, false, 'NONE'
from (values ('r21967Te001'), ('r21967Te002'), ('r21967Te003'), ('r21967Te004'),
             ('r21967Te005'), ('r21967Te006'), ('r21967Te007')) v(uid)
cross join organisationunit o cross join trackedentitytype t
where o.uid = 'QII5GqfDfO3' and t.uid = 'r21967Tet01'
on conflict (uid) do nothing;

insert into enrollment
    (enrollmentid, uid, enrollmentdate, occurreddate, programid, status, followup,
     created, lastupdated, trackedentityid, organisationunitid, deleted, attributeoptioncomboid)
select nextval('enrollment_sequence'), v.uid, v.enrollmentdate::timestamp,
    v.enrollmentdate::timestamp, p.programid, 'ACTIVE', false,
    timestamp '2021-01-01', timestamp '2022-01-01', t.trackedentityid, o.organisationunitid,
    false, c.categoryoptioncomboid
from (values
    ('r21967En001', 'r21967Te001', '2021-01-01'),
    ('r21967En002', 'r21967Te001', '2021-06-01'),
    ('r21967En003', 'r21967Te002', '2021-01-01'),
    ('r21967En004', 'r21967Te003', '2021-01-01'),
    ('r21967En005', 'r21967Te004', '2021-01-01'),
    ('r21967En006', 'r21967Te005', '2021-01-01'),
    ('r21967En007', 'r21967Te006', '2021-01-01'),
    ('r21967En008', 'r21967Te007', '2021-01-01')) v(uid, te, enrollmentdate)
join trackedentity t on t.uid = v.te
cross join program p cross join organisationunit o cross join categoryoptioncombo c
where p.uid = 'r21967Prg01' and o.uid = 'QII5GqfDfO3' and c.uid = 'HllvX50cXC0'
on conflict (uid) do nothing;

insert into trackerevent
    (eventid, uid, enrollmentid, programstageid, scheduleddate, occurreddate,
     organisationunitid, status, created, lastupdated, deleted, attributeoptioncomboid,
     eventdatavalues)
select nextval('trackerevent_sequence'), v.uid, e.enrollmentid, s.programstageid,
    v.occurreddate::timestamp, v.occurreddate::timestamp, o.organisationunitid, v.status,
    timestamp '2021-01-01', timestamp '2022-01-01', false, c.categoryoptioncomboid,
    case when v.weight is null then '{}'::jsonb else jsonb_build_object('r21967De001',
        jsonb_build_object('value', v.weight, 'created', '2021-01-01T00:00:00',
            'lastUpdated', '2022-01-01T00:00:00', 'providedElsewhere', false)) end
from (values
    ('r21967Ev001', 'r21967En001', 'r21967Stg01', '2021-02-01', 'QII5GqfDfO3', 'COMPLETED', '10'),
    ('r21967Ev002', 'r21967En002', 'r21967Stg01', '2021-07-02', 'DiszpKrYNg8', 'ACTIVE', null),
    ('r21967Ev003', 'r21967En003', 'r21967Stg01', '2021-07-01 10:00:00', 'QII5GqfDfO3', 'ACTIVE', '20'),
    ('r21967Ev004', 'r21967En004', 'r21967Stg01', '2021-07-01 18:00:00', 'QII5GqfDfO3', 'ACTIVE', '30'),
    ('r21967Ev005', 'r21967En005', 'r21967Stg01', '2021-06-30', 'QII5GqfDfO3', 'ACTIVE', '40'),
    ('r21967Ev006', 'r21967En006', 'r21967Stg01', '2022-01-01', 'QII5GqfDfO3', 'ACTIVE', '50'),
    ('r21967Ev007', 'r21967En002', 'r21967Stg02', '2021-07-03', 'QII5GqfDfO3', 'ACTIVE', '99'),
    ('r21967Ev008', 'r21967En008', 'r21967Stg01', '2020-01-01', 'QII5GqfDfO3', 'COMPLETED', '60'),
    ('r21967Ev009', 'r21967En008', 'r21967Stg01', '2020-01-01', 'DiszpKrYNg8', 'ACTIVE', '70'))
    v(uid, enrollment, stage, occurreddate, ou, status, weight)
join enrollment e on e.uid = v.enrollment
join programstage s on s.uid = v.stage
join organisationunit o on o.uid = v.ou
cross join categoryoptioncombo c where c.uid = 'HllvX50cXC0'
on conflict (uid) do nothing;
