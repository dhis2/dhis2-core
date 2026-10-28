-- DHIS2-22268: isolated first/last stage-date aggregation fixture.
-- Both facilities are descendants of Zoy23SSHCPs. Fixed dates keep snapshots stable.
-- February 2022 expectations:
-- FIRST = 1000 + 2000 = 3000; FIRST_AVERAGE_ORG_UNIT = 1500;
-- FIRST_FIRST_ORG_UNIT = 1000 (earliest Birth across the requested OU group).
-- LAST = 3180 + 3575 = 6755; LAST_AVERAGE_ORG_UNIT = 3377.5;
-- LAST_LAST_ORG_UNIT = 3180 (latest Birth across the requested OU group).
-- Follow-ups precede and follow Birth events, with the same weight element.
-- Sindadu's latest Birth is in January; Koardu's is late on February's final day.
-- A null weight, a post-cutoff Birth and a pre-lookback Birth must be ignored.

insert into trackedentitytype
select (jsonb_populate_record(null::trackedentitytype, to_jsonb(t) || jsonb_build_object(
    'trackedentitytypeid', nextval('hibernate_sequence'), 'uid', 'flTet000001',
    'code', null, 'name', 'First last stage period entity', 'shortname', 'First last entity',
    'created', timestamp '2021-01-01', 'lastupdated', timestamp '2022-01-01'))).*
from trackedentitytype t where t.uid = 'UinS6TQnkUi'
on conflict (uid) do nothing;

insert into program
select (jsonb_populate_record(null::program, to_jsonb(p) || jsonb_build_object(
    'programid', nextval('hibernate_sequence'), 'uid', 'flProg00001', 'code', null,
    'name', 'First last stage period program', 'shortname', 'First last program',
    'trackedentitytypeid', t.trackedentitytypeid, 'onlyenrollonce', false,
    'dataentryformid', null, 'relatedprogramid', null))).*
from program p cross join trackedentitytype t
where p.uid = 'IpHINAT79UW' and t.uid = 'flTet000001'
on conflict (uid) do nothing;

insert into programstage
select (jsonb_populate_record(null::programstage, to_jsonb(s) || jsonb_build_object(
    'programstageid', nextval('hibernate_sequence'), 'uid', v.uid, 'code', null,
    'name', v.name, 'programid', p.programid, 'repeatable', true,
    'autogenerateevent', false, 'dataentryformid', null, 'dataentryform', null,
    'nextscheduledateid', null, 'sort_order', v.sort_order))).*
from programstage s cross join program p cross join
    (values ('flBirth0001', 'First last Birth', 1), ('flFollow001', 'First last Follow-up', 2))
    v(uid, name, sort_order)
where s.uid = 'A03MvHHogjR' and p.uid = 'flProg00001'
on conflict (uid) do nothing;

insert into dataelement
select (jsonb_populate_record(null::dataelement, to_jsonb(d) || jsonb_build_object(
    'dataelementid', nextval('hibernate_sequence'), 'uid', 'flWeight001', 'code', null,
    'name', 'First last regression weight', 'shortname', 'First last weight',
    'valuetype', 'NUMBER', 'aggregationtype', 'AVERAGE', 'optionsetid', null,
    'commentoptionsetid', null))).*
from dataelement d where d.uid = 'UXz7xuGCEhU'
on conflict (uid) do nothing;

insert into programstagedataelement
    (programstagedataelementid, uid, created, lastupdated, programstageid, dataelementid,
     compulsory, sort_order, displayinreports, skipsynchronization, skipanalytics)
select nextval('hibernate_sequence'), v.uid, timestamp '2021-01-01', timestamp '2022-01-01',
    s.programstageid, d.dataelementid, false, 1, true, false, false
from (values ('flBirth0001', 'flPde000001'), ('flFollow001', 'flPde000002')) v(stage, uid)
join programstage s on s.uid = v.stage cross join dataelement d
where d.uid = 'flWeight001'
on conflict (uid) do nothing;

insert into program_organisationunits (programid, organisationunitid)
select p.programid, o.organisationunitid from program p cross join organisationunit o
where p.uid = 'flProg00001' and o.uid in ('PwoQgMJNWbR', 'nurO6U9bOLi')
on conflict do nothing;

insert into trackedentity
    (trackedentityid, uid, organisationunitid, trackedentitytypeid, created, lastupdated,
     createdatclient, lastupdatedatclient, inactive, deleted, featuretype)
select nextval('trackedentity_sequence'), v.uid, o.organisationunitid, t.trackedentitytypeid,
    timestamp '2021-01-01', timestamp '2022-01-01', timestamp '2021-01-01',
    timestamp '2022-01-01', false, false, 'NONE'
from (values ('flTe0000001', 'PwoQgMJNWbR'), ('flTe0000002', 'nurO6U9bOLi')) v(uid, ou)
join organisationunit o on o.uid = v.ou cross join trackedentitytype t
where t.uid = 'flTet000001'
on conflict (uid) do nothing;

insert into enrollment
    (enrollmentid, uid, enrollmentdate, occurreddate, programid, status, followup,
     created, lastupdated, trackedentityid, organisationunitid, deleted, attributeoptioncomboid)
select nextval('enrollment_sequence'), v.uid, timestamp '2021-01-01',
    timestamp '2021-01-01', p.programid, 'ACTIVE', false,
    timestamp '2021-01-01', timestamp '2022-01-01', t.trackedentityid, o.organisationunitid,
    false, c.categoryoptioncomboid
from (values ('flEn0000001', 'flTe0000001', 'PwoQgMJNWbR'),
             ('flEn0000002', 'flTe0000002', 'nurO6U9bOLi')) v(uid, te, ou)
join trackedentity t on t.uid = v.te join organisationunit o on o.uid = v.ou
cross join program p cross join categoryoptioncombo c
where p.uid = 'flProg00001' and c.uid = 'HllvX50cXC0'
on conflict (uid) do nothing;

insert into trackerevent
    (eventid, uid, enrollmentid, programstageid, scheduleddate, occurreddate,
     organisationunitid, status, created, lastupdated, deleted, attributeoptioncomboid,
     eventdatavalues)
select nextval('trackerevent_sequence'), v.uid, e.enrollmentid, s.programstageid,
    v.eventdate::timestamp, v.eventdate::timestamp, o.organisationunitid, 'COMPLETED',
    v.eventdate::timestamp, timestamp '2022-03-01', false, c.categoryoptioncomboid,
    case when v.weight is null then '{}'::jsonb else jsonb_build_object('flWeight001',
        jsonb_build_object('value', v.weight, 'created', v.eventdate,
            'lastUpdated', '2022-03-01T00:00:00', 'providedElsewhere', false)) end
from (values
    ('flEv0000001', 'flEn0000001', 'flBirth0001', '2010-01-01', 'PwoQgMJNWbR', '500'),
    ('flEv0000002', 'flEn0000001', 'flFollow001', '2020-12-01', 'PwoQgMJNWbR', '9000'),
    ('flEv0000003', 'flEn0000002', 'flFollow001', '2020-12-02', 'nurO6U9bOLi', '8000'),
    ('flEv0000004', 'flEn0000001', 'flBirth0001', '2021-01-10', 'PwoQgMJNWbR', '1000'),
    ('flEv0000005', 'flEn0000002', 'flBirth0001', '2021-01-15', 'nurO6U9bOLi', '2000'),
    ('flEv0000006', 'flEn0000001', 'flBirth0001', '2022-01-20', 'PwoQgMJNWbR', '3100'),
    ('flEv0000007', 'flEn0000002', 'flBirth0001', '2022-01-25', 'nurO6U9bOLi', '3575'),
    ('flEv0000008', 'flEn0000002', 'flFollow001', '2022-02-10', 'nurO6U9bOLi', '8000'),
    ('flEv0000009', 'flEn0000001', 'flBirth0001', '2022-02-28 23:59:00', 'PwoQgMJNWbR', '3180'),
    ('flEv0000010', 'flEn0000001', 'flFollow001', '2022-02-28 23:59:50', 'PwoQgMJNWbR', '9000'),
    ('flEv0000011', 'flEn0000001', 'flBirth0001', '2022-02-28 23:59:59', 'PwoQgMJNWbR', null),
    ('flEv0000012', 'flEn0000001', 'flBirth0001', '2022-03-01', 'PwoQgMJNWbR', '9500'))
    v(uid, enrollment, stage, eventdate, ou, weight)
join enrollment e on e.uid = v.enrollment join programstage s on s.uid = v.stage
join organisationunit o on o.uid = v.ou cross join categoryoptioncombo c
where c.uid = 'HllvX50cXC0'
on conflict (uid) do nothing;
