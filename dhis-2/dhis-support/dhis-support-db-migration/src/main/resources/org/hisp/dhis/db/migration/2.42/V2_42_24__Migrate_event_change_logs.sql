-- DHIS2-18117: migrate tracker event change logs from trackedentitydatavalueaudit to eventchangelogs

create sequence if not exists eventchangelog_sequence;

-- Create new table and add constraints
create table if not exists eventchangelog (
    eventchangelogid int8 not null DEFAULT nextval('eventchangelog_sequence'),
    eventid int8 not null,
    dataelementid int8 null,
    eventproperty varchar(100) null,
    currentvalue varchar(50000) null,
    previousvalue varchar(50000) null,
    changelogtype varchar(100) not null,
    created timestamp not null,
    createdby varchar(255) not null,
    constraint eventchangelog_pkey primary key (eventchangelogid)
);

select setval('eventchangelog_sequence', max(eventchangelogid)) from eventchangelog;

/*create sequence if not exists eventchangelog_sequence;
select setval('eventchangelog_sequence', max(eventchangelogid)) FROM eventchangelog;*/

create index index_eventchangelog_programstageinstanceid on eventchangelog using btree (eventid);
alter table eventchangelog add constraint fk_eventchangelog_dataelementid foreign key (dataelementid) references dataelement(dataelementid);
alter table eventchangelog add constraint fk_eventchangelog_eventid foreign key (eventid) references "event"(eventid);

-- Migrate data into new table
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
where cl.audittype in ('CREATE', 'UPDATE', 'DELETE');