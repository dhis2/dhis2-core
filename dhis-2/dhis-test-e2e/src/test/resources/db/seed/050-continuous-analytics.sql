-- Isolated DHIS2-22183 fixtures for ContinuousAnalyticsTest. The test writes data values for this
-- data element after the full analytics export and then runs a continuous (lastYears=0) update, so
-- it uses its own data element and data set: no other analytics test queries them, and their data
-- does not change any existing analytics snapshot. The data element has no data values in the dump.
-- Clone metadata defaults from the Sierra Leone test dump. Safe to apply repeatedly.
insert into dataelement
select (jsonb_populate_record(null::dataelement, to_jsonb(d) || jsonb_build_object(
    'dataelementid', nextval('hibernate_sequence'), 'uid', 'caE2eDe0001', 'code', null,
    'name', 'Continuous analytics e2e value', 'shortname', 'Continuous analytics e2e',
    'formname', null, 'valuetype', 'INTEGER', 'domaintype', 'AGGREGATE',
    'aggregationtype', 'SUM', 'categorycomboid', cc.categorycomboid, 'optionsetid', null,
    'commentoptionsetid', null, 'zeroissignificant', false,
    'created', timestamp '2022-01-01', 'lastupdated', timestamp '2022-01-01'))).*
from dataelement d cross join categorycombo cc
where d.uid = 'fbfJHSPpUQD' and cc.uid = 'bjDvmb4bfuf'
on conflict (uid) do nothing;

insert into dataset
select (jsonb_populate_record(null::dataset, to_jsonb(s) || jsonb_build_object(
    'datasetid', nextval('hibernate_sequence'), 'uid', 'caE2eDs0001', 'code', null,
    'name', 'Continuous analytics e2e', 'shortname', 'Continuous analytics e2e',
    'categorycomboid', cc.categorycomboid, 'dataentryform', null, 'workflowid', null,
    'created', timestamp '2022-01-01', 'lastupdated', timestamp '2022-01-01'))).*
from dataset s cross join categorycombo cc
where s.uid = 'BfMAe6Itzgt' and cc.uid = 'bjDvmb4bfuf'
on conflict (uid) do nothing;

insert into datasetelement (datasetelementid, datasetid, dataelementid)
select nextval('hibernate_sequence'), s.datasetid, d.dataelementid
from dataset s cross join dataelement d
where s.uid = 'caE2eDs0001' and d.uid = 'caE2eDe0001'
  and not exists (
    select 1 from datasetelement e
    where e.datasetid = s.datasetid and e.dataelementid = d.dataelementid);

insert into datasetsource (datasetid, sourceid)
select s.datasetid, o.organisationunitid
from dataset s cross join organisationunit o
where s.uid = 'caE2eDs0001' and o.uid = 'DiszpKrYNg8'
on conflict do nothing;
