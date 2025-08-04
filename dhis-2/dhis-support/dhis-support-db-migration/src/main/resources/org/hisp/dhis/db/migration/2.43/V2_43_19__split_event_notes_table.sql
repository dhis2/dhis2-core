alter table if exists event_notes
    drop constraint if exists fk_programstageinstancecomments_programstageinstanceid;

CREATE TABLE trackerevent_notes (LIKE event_notes INCLUDING ALL);
CREATE TABLE singleevent_notes (LIKE event_notes INCLUDING ALL);

INSERT INTO trackerevent_notes
SELECT *
FROM event_notes
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITH_REGISTRATION'
);

INSERT INTO singleevent_notes
SELECT *
FROM event_notes
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);

alter table if exists trackerevent_notes
    add constraint fk_trackerevent_notes_eventid FOREIGN KEY (eventid) REFERENCES event(eventid);
alter table if exists singleevent_notes
    add constraint fk_singleevent_notes_eventid FOREIGN KEY (eventid) REFERENCES event(eventid);

drop table event_notes;
