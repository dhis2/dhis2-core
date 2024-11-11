-- DHIS2-18117: migrate tracker event change logs from trackedentitydatavalueaudit to eventchangelogs

-- Create new table and add constraints
create sequence if not exists eventchangelog_sequence;

create table if not exists eventchangelog (
    eventchangelogid int8 not null default nextval('eventchangelog_sequence'),
    eventid int8 not null,
    dataelementid int8 null,
    eventproperty varchar(100) null,
    currentvalue varchar(50000) null,
    previousvalue varchar(50000) null,
    changelogtype varchar(100) not null,
    created timestamp not null,
    createdby varchar(255) not null,
    constraint eventchangelog_pkey primary key (eventchangelogid),
    constraint fk_eventchangelog_dataelementid foreign key (dataelementid) references dataelement(dataelementid),
    constraint fk_eventchangelog_eventid foreign key (eventid) references event(eventid)
);

create index if not exists eventchangelog_eventid_created_idx on eventchangelog (eventid, created desc);

-- Migrate data from trackedentitydatavalueaudit to eventchangelog
insert into eventchangelog (eventchangelogid, eventid, dataelementid, currentvalue, previousvalue, created, createdby, changelogtype)
select
    cl.trackedentitydatavalueauditid,
    cl.eventid,
    cl.dataelementid,
    case
        when cl.audittype = 'CREATE' then cl.previouschangelogvalue
        when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is null then cl.currentvalue
        when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is not null then cl.currentchangelogvalue
        end,
    case
        when cl.audittype = 'DELETE' then cl.previouschangelogvalue
        when cl.audittype = 'UPDATE' then cl.previouschangelogvalue
        end,
    coalesce(cl.created, '1970-01-01 00:00:00'),
    coalesce (cl.modifiedby, '--'),
    cl.audittype
from
    (select t.trackedentitydatavalueauditid, t.eventid, t.dataelementid, t.created, t.audittype, t.modifiedby,
            lag (t.value) over (partition by t.eventid, t.dataelementid order by t.created desc) as currentchangelogvalue,
            t.value AS previouschangelogvalue,
            e.eventdatavalues -> d.uid  ->> 'value' as currentvalue
     from trackedentitydatavalueaudit t
              join event e using (eventid)
              join dataelement d using (dataelementid)
     order by t.trackedentitydatavalueauditid) cl
where cl.audittype in ('CREATE', 'UPDATE', 'DELETE')
and not exists (
    select 1 from eventchangelog ecl where ecl.eventchangelogid = cl.trackedentitydatavalueauditid
);

-- Set sequence to highest value
select setval('eventchangelog_sequence', max(eventchangelogid)) from eventchangelog;

-- Delete the migrated data from the old table, keeping the unmigrated data.
delete from trackedentitydatavalueaudit
where trackedentitydatavalueauditid in (select eventchangelogid from eventchangelog);