alter table if exists event
    drop constraint if exists fk_programstageinstance_assigneduserid,
    drop constraint if exists fk_programstageinstance_attributeoptioncomboid,
    drop constraint if exists fk_programstageinstance_organisationunitid,
    drop constraint if exists fk_programstageinstance_programinstanceid,
    drop constraint if exists fk_programstageinstance_programstageid;

create table trackerevent
(
    like event including all
);
create table singleevent
(
    like event including all
);

insert into trackerevent
select *
from event
where eventid in (select ev.eventid
                  from event ev
                           join programstage ps on ev.programstageid = ps.programstageid
                           join program p on ps.programid = p.programid
                  where p.type = 'WITH_REGISTRATION');

insert into singleevent
select *
from event
where eventid in (select ev.eventid
                  from event ev
                           join programstage ps on ev.programstageid = ps.programstageid
                           join program p on ps.programid = p.programid
                  where p.type = 'WITHOUT_REGISTRATION');

alter table if exists trackerevent
    add constraint fk_trackerevent_assigneduserid foreign key (assigneduserid) references userinfo (userinfoid),
    add constraint fk_trackerevent_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid),
    add constraint fk_trackerevent_organisationunitid foreign key (organisationunitid) references organisationunit (organisationunitid),
    add constraint fk_trackerevent_enrollmentid foreign key (enrollmentid) references enrollment (enrollmentid),
    add constraint fk_trackerevent_programstageid foreign key (programstageid) references programstage (programstageid);

alter table if exists singleevent
    add constraint fk_singleevent_assigneduserid foreign key (assigneduserid) references userinfo (userinfoid),
    add constraint fk_singleevent_attributeoptioncomboid foreign key (attributeoptioncomboid) references categoryoptioncombo (categoryoptioncomboid),
    add constraint fk_singleevent_organisationunitid foreign key (organisationunitid) references organisationunit (organisationunitid),
    add constraint fk_singleevent_enrollmentid foreign key (enrollmentid) references enrollment (enrollmentid),
    add constraint fk_singleevent_programstageid foreign key (programstageid) references programstage (programstageid);

alter table if exists relationshipitem
    drop constraint fk_relationshipitem_trackereventid,
    drop constraint fk_relationshipitem_singleeventid;
alter table if exists relationshipitem
    add constraint fk_relationshipitem_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_relationshipitem_singleeventid foreign key (singleeventid) references singleevent (eventid);

alter table if exists programmessage
    drop constraint fk_programmessage_trackereventid,
    drop constraint fk_programmessage_singleeventid;
alter table if exists programmessage
    add constraint fk_programmessage_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_programmessage_singleeventid foreign key (singleeventid) references singleevent (eventid);

alter table if exists programnotificationinstance
    drop constraint fk_programnotificationinstance_trackereventid,
    drop constraint fk_programnotificationinstance_singleeventid;
alter table if exists programnotificationinstance
    add constraint fk_programnotificationinstance_trackereventid foreign key (trackereventid) references trackerevent (eventid),
    add constraint fk_programnotificationinstance_singleeventid foreign key (singleeventid) references singleevent (eventid);

alter table if exists trackereventchangelog
    drop constraint fk_trackereventchangelog_eventid;
alter table if exists trackereventchangelog
    add constraint fk_trackereventchangelog_eventid foreign key (eventid) references trackerevent (eventid);
alter table if exists singleeventchangelog
    drop constraint fk_singleeventchangelog_eventid;
alter table if exists singleeventchangelog
    add constraint fk_singleeventchangelog_eventid foreign key (eventid) references singleevent (eventid);

alter table if exists trackerevent_notes
    drop constraint fk_trackerevent_notes_eventid;
alter table if exists trackerevent_notes
    add constraint fk_trackerevent_notes_eventid foreign key (eventid) references trackerevent (eventid);
alter table if exists singleevent_notes
    drop constraint fk_singleevent_notes_eventid;
alter table if exists singleevent_notes
    add constraint fk_singleevent_notes_eventid foreign key (eventid) references singleevent (eventid);

alter table singleevent drop column code;
alter table trackerevent drop column code;

create sequence if not exists trackerevent_sequence;
create sequence if not exists singleevent_sequence;
select setval('trackerevent_sequence', max(eventid)) FROM trackerevent;
select setval('singleevent_sequence', max(eventid)) FROM singleevent;

drop table if exists event;
