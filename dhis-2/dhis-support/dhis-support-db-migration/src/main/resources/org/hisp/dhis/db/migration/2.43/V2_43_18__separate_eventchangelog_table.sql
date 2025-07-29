alter table if exists eventchangelog
    drop constraint if exists fk_eventchangelog_eventid;

CREATE TABLE trackereventchangelog (LIKE eventchangelog INCLUDING ALL);
CREATE TABLE singleeventchangelog (LIKE eventchangelog INCLUDING ALL);

INSERT INTO trackereventchangelog
SELECT *
FROM eventchangelog
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITH_REGISTRATION'
);

INSERT INTO singleeventchangelog
SELECT *
FROM eventchangelog
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);

alter table if exists trackereventchangelog
    add constraint fk_trackereventchangelog_eventid FOREIGN KEY (eventid) REFERENCES event(eventid);
alter table if exists singleeventchangelog
    add constraint fk_singleeventchangelog_eventid FOREIGN KEY (eventid) REFERENCES event(eventid);

drop table eventchangelog;
