-- DHIS2-22070: reuse the six enrollments from 010_registration_ou.sql.
-- All six enrollments are ACTIVE and have one COMPLETED event in June 2022.
-- Each indicator below must therefore return 6 for 2022 at the national org unit.
-- Only new indicator metadata is added; existing fixture data is unchanged.

insert into programindicator (
    programindicatorid, uid, code, created, lastupdated, name, shortname,
    programid, expression, filter, aggregationtype, decimals, analyticstype,
    categorycomboid, attributecomboid, publicaccess, sharing)
select nextval('hibernate_sequence'), v.uid, v.uid,
       timestamp '2022-07-01 00:00:00', timestamp '2022-07-01 00:00:00',
       v.name, v.name, p.programid, '1', v.filter, 'SUM', 0, 'ENROLLMENT',
       cc.categorycomboid, cc.categorycomboid, 'r-------', '{"public":"r-------"}'::jsonb
from program p
cross join categorycombo cc
cross join (values
    ('pi22070And1', 'DHIS2-22070 conjunctive filter',
     $$V{event_status} == 'COMPLETED' && V{enrollment_status} == 'ACTIVE'$$),
    ('pi22070Or01', 'DHIS2-22070 disjunctive filter',
     $$V{event_status} == 'ACTIVE' || V{event_status} == 'COMPLETED'$$),
    ('pi22070Not1', 'DHIS2-22070 negated filter',
     $$!(V{event_status} == 'ACTIVE')$$)
) as v(uid, name, filter)
where p.uid = 'regOuProg01'
  and cc.name = 'default'
  and not exists (select 1 from programindicator pi where pi.uid = v.uid);

insert into periodboundary (
    periodboundaryid, uid, created, lastupdated, boundarytarget,
    analyticsperiodboundarytype, programindicatorid)
select nextval('hibernate_sequence'), v.uid,
       timestamp '2022-07-01 00:00:00', timestamp '2022-07-01 00:00:00',
       'EVENT_DATE', v.boundary_type, pi.programindicatorid
from programindicator pi
join (values
    ('pi22070And1', 'pb22070And1', 'AFTER_START_OF_REPORTING_PERIOD'),
    ('pi22070And1', 'pb22070And2', 'BEFORE_END_OF_REPORTING_PERIOD'),
    ('pi22070Or01', 'pb22070Or01', 'AFTER_START_OF_REPORTING_PERIOD'),
    ('pi22070Or01', 'pb22070Or02', 'BEFORE_END_OF_REPORTING_PERIOD'),
    ('pi22070Not1', 'pb22070Not1', 'AFTER_START_OF_REPORTING_PERIOD'),
    ('pi22070Not1', 'pb22070Not2', 'BEFORE_END_OF_REPORTING_PERIOD')
) as v(pi_uid, uid, boundary_type) on pi.uid = v.pi_uid
where not exists (select 1 from periodboundary pb where pb.uid = v.uid);
