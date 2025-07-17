alter table if exists relationshipitem add column if not exists trackereventid bigint;
alter table if exists relationshipitem add column if not exists singleeventid bigint;
alter table if exists relationshipitem drop constraint if exists fk_relationshipitem_trackereventid;
alter table if exists relationshipitem drop constraint if exists fk_relationshipitem_singleeventid;
alter table if exists relationshipitem
    add constraint fk_relationshipitem_trackereventid FOREIGN KEY (trackereventid) REFERENCES event(eventid);
alter table if exists relationshipitem
    add constraint fk_relationshipitem_singleeventid FOREIGN KEY (singleeventid) REFERENCES event(eventid);
alter table if exists relationshipitem drop constraint if exists fk_relationshipitem_programstageinstanceid;

UPDATE relationshipitem
SET singleeventid = eventid
WHERE eventid IN (
    SELECT ev.eventid
    FROM event ev
    JOIN programstage ps ON ev.programstageid = ps.programstageid
    JOIN program p ON ps.programid = p.programid
    WHERE p.type = 'WITHOUT_REGISTRATION'
);
update relationshipitem
set trackereventid = eventid
where singleeventid is null;

alter table if exists relationshipitem drop column if exists eventid;
