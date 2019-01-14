------------------------------------------------------------------------------------------------------------------
---------- SQL commands converting trackedentitydatavalue into new json column in programstageinstance  ----------
------------------------------------------------------------------------------------------------------------------

alter table programstageinstance add column eventdatavalues jsonb;

-- Fixes invalid timestamps in DB (problems found in Demo DB, but can be run on all DBs)
update trackedentitydatavalue set created = substring( created::text, 1, 23 )::timestamp where created::text ~ '.*\.\d{6}$';
update trackedentitydatavalue set lastupdated = substring( lastupdated::text, 1, 23 )::timestamp where lastupdated::text ~ '.*\.\d{6}$';

-- Fetch data from trackedentitydatavalue table, process them, creates a jsonb representation of them and stores into eventdatavalues column
-- in programstageinstance table.
-- Various PostgreSql functions are used to get the required format of the JSON string. The string is then cast to jsonb type.
-- Takes care of empty values. When there is no trackedentitydatavalue for given programstageinstance a null is saved. This can be changed
-- and specified JSON string can be saved instead (change NULL for the given JSON String).
update programstageinstance psi set eventdatavalues =
  to_jsonb(coalesce(nullif(replace(array_to_string(array(
    select jsonb_build_object(de.uid, jsonb_build_object('created', tedv.created, 'lastUpdated', tedv.lastupdated, 'value', tedv.value,
                                                         'providedElsewhere', tedv.providedelsewhere, 'storedBy', tedv.storedby))
    from trackedentitydatavalue tedv join dataelement de on tedv.dataelementid = de.dataelementid
    where tedv.programstageinstanceid = psi.programstageinstanceid), ','), '}},{', '},'), ''), NULL)::json);

-- NULL will make a mess, Get rid of it and replace with an empty json object '{}'
update programstageinstance set eventdatavalues = '{}'::jsonb where eventdatavalues is null;

-- Add default value, so NULL is not entered accidentally
alter table programstageinstance alter column eventdatavalues set default '{}'::jsonb;

-- Add NOT NULL constraint to not allow entering NULL at all
alter table programstageinstance alter column eventdatavalues set not null;
