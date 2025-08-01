
alter table if exists event
    drop constraint if exists fk_programstageinstance_assigneduserid,
    drop constraint if exists fk_programstageinstance_attributeoptioncomboid,
    drop constraint if exists fk_programstageinstance_organisationunitid,
    drop constraint if exists fk_programstageinstance_programinstanceid,
    drop constraint if exists fk_programstageinstance_programstageid;

CREATE TABLE trackerevent (LIKE event INCLUDING ALL);
CREATE TABLE singleevent (LIKE event INCLUDING ALL);

INSERT INTO trackerevent
SELECT *
FROM event
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITH_REGISTRATION'
);

INSERT INTO singleevent
SELECT *
FROM event
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);

alter table if exists trackerevent
    add constraint fk_trackerevent_assigneduserid FOREIGN KEY (assigneduserid) REFERENCES userinfo(userinfoid),
    add constraint fk_trackerevent_attributeoptioncomboid FOREIGN KEY (attributeoptioncomboid) REFERENCES categoryoptioncombo(categoryoptioncomboid),
    add constraint fk_trackerevent_organisationunitid FOREIGN KEY (organisationunitid) REFERENCES organisationunit(organisationunitid),
    add constraint fk_trackerevent_enrollmentid FOREIGN KEY (enrollmentid) REFERENCES enrollment(enrollmentid),
    add constraint fk_trackerevent_programstageid FOREIGN KEY (programstageid) REFERENCES programstage(programstageid);

alter table if exists singleevent
    add constraint fk_singleevent_assigneduserid FOREIGN KEY (assigneduserid) REFERENCES userinfo(userinfoid),
    add constraint fk_singleevent_attributeoptioncomboid FOREIGN KEY (attributeoptioncomboid) REFERENCES categoryoptioncombo(categoryoptioncomboid),
    add constraint fk_singleevent_organisationunitid FOREIGN KEY (organisationunitid) REFERENCES organisationunit(organisationunitid),
    add constraint fk_singleevent_enrollmentid FOREIGN KEY (enrollmentid) REFERENCES enrollment(enrollmentid),
    add constraint fk_singleevent_programstageid FOREIGN KEY (programstageid) REFERENCES programstage(programstageid);

alter table if exists relationshipitem
    drop constraint fk_relationshipitem_trackereventid,
    drop constraint fk_relationshipitem_singleeventid;
alter table if exists relationshipitem
    add constraint fk_relationshipitem_trackereventid FOREIGN KEY (trackereventid) REFERENCES trackerevent(eventid),
    add constraint fk_relationshipitem_singleeventid FOREIGN KEY (singleeventid) REFERENCES singleevent(eventid);

alter table if exists programmessage
    drop constraint fk_programmessage_trackereventid,
    drop constraint fk_programmessage_singleeventid;
alter table if exists programmessage
    add constraint fk_programmessage_trackereventid FOREIGN KEY (trackereventid) REFERENCES trackerevent(eventid),
    add constraint fk_programmessage_singleeventid FOREIGN KEY (singleeventid) REFERENCES singleevent(eventid);

alter table if exists programnotificationinstance
    drop constraint fk_programnotificationinstance_trackereventid,
    drop constraint fk_programnotificationinstance_singleeventid;
alter table if exists programnotificationinstance
    add constraint fk_programnotificationinstance_trackereventid FOREIGN KEY (trackereventid) REFERENCES trackerevent(eventid),
    add constraint fk_programnotificationinstance_singleeventid FOREIGN KEY (singleeventid) REFERENCES singleevent(eventid);

alter table if exists trackereventchangelog
    drop constraint fk_trackereventchangelog_eventid;
alter table if exists trackereventchangelog
    add constraint fk_trackereventchangelog_eventid FOREIGN KEY (eventid) REFERENCES trackerevent(eventid);
alter table if exists singleeventchangelog
    drop constraint fk_singleeventchangelog_eventid;
alter table if exists singleeventchangelog
    add constraint fk_singleeventchangelog_eventid FOREIGN KEY (eventid) REFERENCES singleevent(eventid);

alter table if exists trackerevent_notes
    drop constraint fk_trackerevent_notes_eventid;
alter table if exists trackerevent_notes
    add constraint fk_trackerevent_notes_eventid FOREIGN KEY (eventid) REFERENCES trackerevent(eventid);
alter table if exists singleevent_notes
    drop constraint fk_singleevent_notes_eventid;
alter table if exists singleevent_notes
    add constraint fk_singleevent_notes_eventid FOREIGN KEY (eventid) REFERENCES singleevent(eventid);


drop table if exists event;
