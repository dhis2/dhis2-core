-- Replace single-column indexes on singleevent with composite indexes
-- that have programstageid as leading column, since single event queries
-- always filter by program stage.

-- Drop indexes that are never used or are superseded by the new composites
drop index if exists in_singleevent_status_occurreddate;
drop index if exists in_singleevent_occurreddate;
drop index if exists in_singleevent_attributeoptioncomboid;
drop index if exists in_singleevent_programstageid;
drop index if exists in_singleevent_organisationunitid;
drop index if exists in_singleevent_deleted_assigneduserid;

-- Composite: serves date range filters, ORDER BY occurreddate, and program-only queries
create index if not exists in_singleevent_programstageid_occurreddate
    on singleevent using btree (programstageid, occurreddate);

-- Composite: serves org unit selected/children/descendants queries
create index if not exists in_singleevent_programstageid_organisationunitid
    on singleevent using btree (programstageid, organisationunitid);

-- Composite: serves assigned user queries
create index if not exists in_singleevent_programstageid_assigneduserid
    on singleevent using btree (programstageid, assigneduserid);
