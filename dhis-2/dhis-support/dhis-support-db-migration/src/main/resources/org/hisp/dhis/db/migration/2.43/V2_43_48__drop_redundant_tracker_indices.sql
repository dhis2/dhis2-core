-- drop redundant single-column index on enrollment.programid; already covered by
-- index_programinstance_programid_trackedentityid
drop index if exists index_programinstance;

-- replace uk_tei_program and the redundant 3-column unique index with a single covering
-- unique index that includes organisationunitid so queries joining
-- trackedentityprogramowner to organisationunit can do index-only scans
drop index if exists in_unique_trackedentityprogramowner_teiid_programid_ouid;
alter table trackedentityprogramowner drop constraint if exists uk_tei_program;
create unique index if not exists trackedentityprogramowner_trackedentityid_programid_key
    on trackedentityprogramowner (trackedentityid, programid)
    include (organisationunitid);
