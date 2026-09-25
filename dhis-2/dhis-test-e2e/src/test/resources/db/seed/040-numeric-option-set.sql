-- DHIS2-22038: a NUMBER data element whose option set carries numeric codes.
-- The analytics column of a decimal data element is a double, so the option code "1" is
-- read back as "1.0" and matches no option in the response metadata. Reproducing that needs
-- an option set whose codes parse as numbers; every such set in the demo dump is text coded.
--
-- Reuses the six events from 010_registration_ou.sql, whose eventdatavalues are empty.
-- Codes are spread so each aggregates to a different count:
--
--   code 1   events 001-003   count 3
--   code 2   events 004-005   count 2
--   code 3   event  006       count 1
--
-- Clone demo rows to retain required defaults across schema versions.

-- Option set with numeric codes.

create temporary table seed_numopt_optionset as
select * from optionset where uid = 'SokRAajDrRz';

update seed_numopt_optionset
set optionsetid = 9100041,
    uid = 'numOptSet01',
    code = 'NUMOPT_SET',
    name = 'DHIS2-22038 numeric option set',
    valuetype = 'NUMBER',
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into optionset
select * from seed_numopt_optionset
where not exists (select 1 from optionset where uid = 'numOptSet01');

create temporary table seed_numopt_option as
select o.*, v.new_id, v.new_uid, v.new_code, v.new_name, v.new_sort
from (select * from optionvalue where optionsetid = 1151439 limit 1) o
cross join (values (9100042, 'numOptOne01', '1', 'One', 1),
                   (9100043, 'numOptTwo01', '2', 'Two', 2),
                   (9100044, 'numOptThr01', '3', 'Three', 3))
    as v(new_id, new_uid, new_code, new_name, new_sort);

update seed_numopt_option s
set optionvalueid = s.new_id,
    uid = s.new_uid,
    code = s.new_code,
    name = s.new_name,
    sort_order = s.new_sort,
    optionsetid = 9100041,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

alter table seed_numopt_option
  drop column new_id, drop column new_uid, drop column new_code,
  drop column new_name, drop column new_sort;

insert into optionvalue
select * from seed_numopt_option
where not exists (select 1 from optionvalue where uid = 'numOptOne01');

-- Data element bound to that option set.

create temporary table seed_numopt_de as
select * from dataelement where uid = 'UXz7xuGCEhU';

update seed_numopt_de
set dataelementid = 9100045,
    uid = 'numOptDe001',
    code = 'NUMOPT_DE',
    name = 'DHIS2-22038 numeric option set element',
    shortname = 'DHIS2-22038 numeric option set element',
    formname = null,
    valuetype = 'NUMBER',
    domaintype = 'TRACKER',
    aggregationtype = 'SUM',
    optionsetid = 9100041,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into dataelement
select * from seed_numopt_de
where not exists (select 1 from dataelement where uid = 'numOptDe001');

-- Attach it to the registration OU fixture stage so it reaches the analytics tables.

create temporary table seed_numopt_psde as
select * from programstagedataelement
order by programstagedataelementid
limit 1;

update seed_numopt_psde
set programstagedataelementid = 9100046,
    uid = 'numOptPsd01',
    code = 'NUMOPT_PSDE',
    programstageid = 9100003,
    dataelementid = 9100045,
    compulsory = false,
    sort_order = 1,
    skipanalytics = false,
    skipsynchronization = false,
    created = timestamp '2022-07-01 00:00:00',
    lastupdated = timestamp '2022-07-01 00:00:00';

insert into programstagedataelement
select * from seed_numopt_psde
where not exists (select 1 from programstagedataelement where uid = 'numOptPsd01');

-- Option codes on the six fixture events.

update trackerevent e
set eventdatavalues = e.eventdatavalues || jsonb_build_object(
        'numOptDe001',
        jsonb_build_object(
            'value', v.code,
            'created', '2022-07-01T00:00:00',
            'lastUpdated', '2022-07-01T00:00:00',
            'storedBy', null,
            'providedElsewhere', false)),
    lastupdated = timestamp '2022-07-01 00:00:00'
from (values ('regOuEvt001', '1'),
             ('regOuEvt002', '1'),
             ('regOuEvt003', '1'),
             ('regOuEvt004', '2'),
             ('regOuEvt005', '2'),
             ('regOuEvt006', '3')) as v(event_uid, code)
where e.uid = v.event_uid
  and not e.eventdatavalues ? 'numOptDe001';

drop table seed_numopt_optionset;
drop table seed_numopt_option;
drop table seed_numopt_de;
drop table seed_numopt_psde;
