-- Split event table into trackerevent and singleevent tables
alter table event drop code;

-- Copy single events into singleevent table without any constraint
create table singleevent as
select * from event
where eventid in (
    select ev.eventid
    from event ev
             join programstage ps on ev.programstageid = ps.programstageid
             join program p on ps.programid = p.programid
    where p.type = 'WITHOUT_REGISTRATION'
);

-- Delete not used columns
alter table singleevent drop column scheduleddate;
alter table singleevent drop column enrollmentid;

-- Create primary key
alter table singleevent add primary key (eventid);
-- Make uid column unique
alter table singleevent add constraint unique_singleevent_uid unique (uid);
-- Set not null columns
alter table singleevent alter column created set not null;
alter table singleevent alter column occurreddate set not null;
alter table singleevent alter column lastupdated set not null;
alter table singleevent alter column lastsynchronized set default to_timestamp((0)::double precision);
alter table singleevent alter column lastsynchronized set not null;
alter table singleevent alter column programstageid set not null;
alter table singleevent alter column attributeoptioncomboid set not null;
alter table singleevent alter column deleted set not null;
alter table singleevent alter column organisationunitid set not null;
alter table singleevent alter column status set not null;
alter table singleevent alter column eventdatavalues set default '{}'::jsonb;
alter table singleevent alter column eventdatavalues set not null;

-- Recreate indexes
create index if not exists in_singleevent_status_occurreddate on singleevent using btree (status,occurreddate);
create index if not exists in_singleevent_deleted_assigneduserid on singleevent using btree (deleted,assigneduserid);
create index if not exists in_singleevent_occurreddate on singleevent using btree (occurreddate);
create index if not exists in_singleevent_attributeoptioncomboid on singleevent using btree (attributeoptioncomboid);
create index if not exists in_singleevent_organisationunitid on singleevent using btree (organisationunitid);
create index if not exists in_singleevent_programstageid on singleevent using btree (programstageid);

create sequence if not exists singleevent_sequence;
select setval('singleevent_sequence', max(eventid)) from singleevent;

-- Recreate foreign keys
alter table if exists singleevent
    add constraint fk_singleevent_assigneduserid foreign key (assigneduserid) references userinfo (userinfoid),
    add constraint fk_singleevent_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid),
    add constraint fk_singleevent_organisationunitid foreign key (organisationunitid) references organisationunit (organisationunitid),
    add constraint fk_singleevent_programstageid foreign key (programstageid) references programstage (programstageid);

-- Copy tracker events into trackerevent table without any constraint
create table trackerevent as
select * from event
where eventid in (
    select ev.eventid
    from event ev
             join programstage ps on ev.programstageid = ps.programstageid
             join program p on ps.programid = p.programid
    where p.type = 'WITH_REGISTRATION'
);

-- Create primary key
alter table trackerevent add primary key (eventid);
-- Make uid column unique
alter table trackerevent add constraint unique_trackerevent_uid unique (uid);
-- Set not null columns
alter table trackerevent alter column created set not null;
alter table trackerevent alter column lastupdated set not null;
alter table trackerevent alter column lastsynchronized set default to_timestamp((0)::double precision);
alter table trackerevent alter column lastsynchronized set not null;
alter table trackerevent alter column enrollmentid set not null;
alter table trackerevent alter column programstageid set not null;
alter table trackerevent alter column attributeoptioncomboid set not null;
alter table trackerevent alter column deleted set not null;
alter table trackerevent alter column organisationunitid set not null;
alter table trackerevent alter column status set not null;
alter table trackerevent alter column eventdatavalues set default '{}'::jsonb;
alter table trackerevent alter column eventdatavalues set not null;

-- Recreate indexes
create index if not exists in_trackerevent_status_occurreddate on trackerevent using btree (status,occurreddate);
create index if not exists in_trackerevent_deleted_assigneduserid on trackerevent using btree (deleted,assigneduserid);
create index if not exists in_trackerevent_occurreddate on trackerevent using btree (occurreddate);
create index if not exists in_trackerevent_attributeoptioncomboid on trackerevent using btree (attributeoptioncomboid);
create index if not exists in_trackerevent_organisationunitid on trackerevent using btree (organisationunitid);
create index if not exists in_trackerevent_enrollmentid on trackerevent using btree (enrollmentid);

create sequence if not exists trackerevent_sequence;
select setval('trackerevent_sequence', max(eventid)) from trackerevent;

-- Recreate foreign keys
alter table if exists trackerevent
    add constraint fk_trackerevent_assigneduserid foreign key (assigneduserid) references userinfo (userinfoid),
    add constraint fk_trackerevent_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid),
    add constraint fk_trackerevent_organisationunitid foreign key (organisationunitid) references organisationunit (organisationunitid),
    add constraint fk_trackerevent_enrollmentid foreign key (enrollmentid) references enrollment (enrollmentid),
    add constraint fk_trackerevent_programstageid foreign key (programstageid) references programstage (programstageid);

-- Check if any event was not moved to singleevent or trackerevent table
do $$
    begin
        create table inconsistentevent as
        select * from event
        where eventid not in (
            select eventid
            from singleevent
            union
            select eventid
            from trackerevent
        );
        if ((select count(*) from inconsistentevent) > 0)
        then raise warning 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.43/migration-notes.md#inconsistent-events to have more information on the issue and to find ways to fix it.';
        end if;
        if ((select count(*) from inconsistentevent) = 0)
        then drop table inconsistentevent;
        end if;
    end;
$$;

-- Split eventid column in relationshipitem table
alter table if exists relationshipitem
    add column if not exists trackereventid bigint,
    add column if not exists singleeventid bigint;
alter table if exists relationshipitem
    drop constraint if exists fk_relationshipitem_programstageinstanceid;
alter table if exists relationshipitem
    add constraint fk_relationshipitem_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_relationshipitem_singleeventid foreign key (singleeventid) references singleevent (eventid);

update relationshipitem
set singleeventid = eventid
where eventid in (select eventid from singleevent);

update relationshipitem
set trackereventid = eventid
where eventid in (select eventid from trackerevent);

alter table if exists relationshipitem drop column if exists eventid;

-- Split eventchangelog table into trackereventchangelog and singleeventchangelog
alter table if exists eventchangelog
    drop constraint if exists fk_eventchangelog_eventid;

create table trackereventchangelog (like eventchangelog including all);
create table singleeventchangelog (like eventchangelog including all);

alter table if exists trackereventchangelog
    add constraint fk_trackereventchangelog_eventid foreign key (eventid) references trackerevent (eventid);
alter table if exists singleeventchangelog
    add constraint fk_singleeventchangelog_eventid foreign key (eventid) references singleevent (eventid);

insert into trackereventchangelog
select *
from eventchangelog
where eventid in (select eventid from trackerevent);

insert into singleeventchangelog
select *
from eventchangelog
where eventid in (select eventid from singleevent);

drop table eventchangelog;

-- Split event_notes table into trackerevent_notes and singleevent_notes
alter table if exists event_notes
    drop constraint if exists fk_programstageinstancecomments_programstageinstanceid;

create table trackerevent_notes (like event_notes including all);
create table singleevent_notes (like event_notes including all);

alter table if exists trackerevent_notes
    add constraint fk_trackerevent_notes_eventid foreign key (eventid) references trackerevent (eventid);
alter table if exists singleevent_notes
    add constraint fk_singleevent_notes_eventid foreign key (eventid) references singleevent (eventid);

insert into trackerevent_notes
select *
from event_notes
where eventid in (select eventid from trackerevent);

insert into singleevent_notes
select *
from event_notes
where eventid in (select eventid from singleevent);

drop table event_notes;

-- Split eventid column in programmessage table
alter table if exists programmessage
    add column if not exists trackereventid bigint,
    add column if not exists singleeventid bigint;

alter table if exists programmessage
    drop constraint if exists fk_programmessage_programstageinstanceid;
alter table if exists programmessage
    add constraint fk_programmessage_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_programmessage_singleeventid foreign key (singleeventid) references singleevent (eventid);

update programmessage
set singleeventid = eventid
where eventid in (select eventid from singleevent);
update programmessage
set trackereventid = eventid
where eventid in (select eventid from trackerevent);

alter table if exists programmessage drop column if exists eventid;

-- Split eventid column in programnotificationinstance table
alter table if exists programnotificationinstance
    add column if not exists trackereventid bigint,
    add column if not exists singleeventid bigint;

alter table if exists programnotificationinstance
    drop constraint if exists fk_programstagenotification_psi;
alter table if exists programnotificationinstance
    add constraint fk_programnotificationinstance_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_programnotificationinstance_singleeventid foreign key (singleeventid) references singleevent (eventid);

update programnotificationinstance
set singleeventid = eventid
where eventid in (select eventid from singleevent);

update programnotificationinstance
set trackereventid = eventid
where eventid in (select eventid from trackerevent);

alter table if exists programnotificationinstance drop column if exists eventid;

-- Drop event table
drop table event;

-- Delete dummy enrollments from DB
delete from enrollment where enrollmentid in (
    select en.enrollmentid
    from enrollment en
             join program p on en.programid = p.programid
    where p.type = 'WITHOUT_REGISTRATION'
);

alter table enrollment alter column trackedentityid set not null;
