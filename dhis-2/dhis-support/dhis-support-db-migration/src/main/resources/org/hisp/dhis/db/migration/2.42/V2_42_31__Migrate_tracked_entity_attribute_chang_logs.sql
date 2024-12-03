-- DHIS2-18474: migrate tracker attribute change logs from trackedentityattributevalueaudit to trackedentitychangelog

-- Create new table and add constraints
create sequence if not exists trackedentitychangelog_sequence;

create table if not exists trackedentitychangelog (
    trackedentitychangelogid int8 not null default nextval('trackedentitychangelog_sequence'),
    trackedentityid int8 not null,
    trackedentityattributeid int8 not null,
    currentvalue varchar(50000) null,
    previousvalue varchar(50000) null,
    changelogtype varchar(100) not null,
    created timestamp not null,
    createdby varchar(255) not null,
    constraint trackedentitychangelog_pkey primary key (trackedentitychangelogid),
    constraint fk_trackedentitychangelog_trackedentityattributeid foreign key (trackedentityattributeid) references trackedentityattribute(trackedentityattributeid),
    constraint fk_trackedentitychangelog_trackedentityid foreign key (trackedentityid) references trackedentity(trackedentityid)
);

create index if not exists trackedentitychangelog_trackedentityid_created_idx on trackedentitychangelog (trackedentityid, created desc);

-- Migrate
insert into trackedentitychangelog (trackedentitychangelogid, trackedentityid, trackedentityattributeid, currentvalue, previousvalue, changelogtype, created, createdby)
select
    cl.trackedentityattributevalueauditid,
    cl.trackedentityid,
    cl.trackedentityattributeid,
    case
        when cl.audittype = 'CREATE' then cl.currentchangelogvalue
        when cl.audittype = 'UPDATE' then cl.currentchangelogvalue
        end as currentvalue,
    case
        when cl.audittype = 'DELETE' then cl.currentchangelogvalue
        when cl.audittype = 'UPDATE' then cl.previouschangelogvalue
        end as previousvalue,
    cl.audittype,
    coalesce(cl.created, '1970-01-01 00:00:00'),
    coalesce (cl.modifiedby, '--')
from (
         select audit.trackedentityattributevalueauditid, audit.trackedentityid, audit.created, tea.trackedentityattributeid, audit.audittype, audit.modifiedby,
                lead (audit.value) over (partition by audit.trackedentityid, audit.trackedentityattributeid order by audit.created desc) as previouschangelogvalue,
                audit.value as currentchangelogvalue
         from trackedentityattributevalueaudit audit
                  join trackedentity t using(trackedentityid)
                  join trackedentityattribute tea using(trackedentityattributeid)
         where audit.audittype in ('CREATE', 'UPDATE', 'DELETE')) cl
where not exists (
    select 1 from trackedentitychangelog tecl where tecl.trackedentitychangelogid = cl.trackedentityattributevalueauditid
);

-- Set sequence to highest value
select setval('trackedentitychangelog_sequence', max(trackedentitychangelogid)) from trackedentitychangelog;

-- Delete the migrated data from the old table, keeping the unmigrated data.
delete from trackedentityattributevalueaudit
where trackedentityattributevalueauditid in (select trackedentitychangelogid from trackedentitychangelog);