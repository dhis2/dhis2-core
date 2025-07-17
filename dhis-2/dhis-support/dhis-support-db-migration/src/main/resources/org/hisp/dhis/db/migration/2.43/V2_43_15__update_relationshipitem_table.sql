alter table if exists relationshipitem rename column eventid to trackereventid;
alter table if exists relationshipitem add column singleeventid bigint;
alter table if exists relationshipitem
    add constraint fk_relationshipitem_singleeventid FOREIGN KEY (singleeventid) REFERENCES event(eventid);
