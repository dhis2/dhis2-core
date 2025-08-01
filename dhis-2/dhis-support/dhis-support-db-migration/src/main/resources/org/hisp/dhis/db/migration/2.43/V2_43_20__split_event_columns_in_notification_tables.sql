alter table if exists programmessage
    add column if not exists trackereventid bigint,
    add column if not exists singleeventid bigint;
alter table if exists programmessage
    drop constraint if exists fk_programmessage_programstageinstanceid,
    drop constraint if exists fk_programmessage_trackereventid,
    drop constraint if exists fk_programmessage_singleeventid;
alter table if exists programmessage
    add constraint fk_programmessage_trackereventid FOREIGN KEY (trackereventid) REFERENCES event(eventid),
    add constraint fk_programmessage_singleeventid FOREIGN KEY (singleeventid) REFERENCES event(eventid);

UPDATE programmessage
SET singleeventid = eventid
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);
update programmessage
set trackereventid = eventid
where singleeventid is null;

alter table if exists programmessage drop column if exists eventid;
 ------

alter table if exists programnotificationinstance
    add column if not exists trackereventid bigint,
    add column if not exists singleeventid bigint;
alter table if exists programnotificationinstance
    drop constraint if exists fk_programstagenotification_psi,
    drop constraint if exists fk_programnotificationinstance_trackereventid,
    drop constraint if exists fk_programnotificationinstance_singleeventid;
alter table if exists programnotificationinstance
    add constraint fk_programnotificationinstance_trackereventid FOREIGN KEY (trackereventid) REFERENCES event(eventid),
    add constraint fk_programnotificationinstance_singleeventid FOREIGN KEY (singleeventid) REFERENCES event(eventid);

UPDATE programnotificationinstance
SET singleeventid = eventid
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
             JOIN programstage ps ON ev.programstageid = ps.programstageid
             JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);
update programnotificationinstance
set trackereventid = eventid
where singleeventid is null;

alter table if exists programnotificationinstance drop column if exists eventid;
