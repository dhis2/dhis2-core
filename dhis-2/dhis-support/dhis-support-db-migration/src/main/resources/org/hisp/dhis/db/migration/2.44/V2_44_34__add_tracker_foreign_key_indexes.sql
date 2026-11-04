-- Index foreign keys to tracker tables and program messages so deleting them doesn't scan these tables per row. DHIS2-16351
create index if not exists in_programmessage_trackedentityid
    on programmessage (trackedentityid) where trackedentityid is not null;
create index if not exists in_programmessage_enrollmentid
    on programmessage (enrollmentid) where enrollmentid is not null;
create index if not exists in_programmessage_trackereventid
    on programmessage (trackereventid) where trackereventid is not null;
create index if not exists in_programmessage_singleeventid
    on programmessage (singleeventid) where singleeventid is not null;

create index if not exists in_programmessage_deliverychannels_programmessageid
    on programmessage_deliverychannels (programmessagedeliverychannelsid);
create index if not exists in_programmessage_emailaddresses_programmessageid
    on programmessage_emailaddresses (programmessageemailaddressid);
create index if not exists in_programmessage_phonenumbers_programmessageid
    on programmessage_phonenumbers (programmessagephonenumberid);

create index if not exists in_programnotificationinstance_enrollmentid
    on programnotificationinstance (enrollmentid) where enrollmentid is not null;
create index if not exists in_programnotificationinstance_trackereventid
    on programnotificationinstance (trackereventid) where trackereventid is not null;
create index if not exists in_programnotificationinstance_singleeventid
    on programnotificationinstance (singleeventid) where singleeventid is not null;

create index if not exists in_relationshipitem_trackereventid
    on relationshipitem (trackereventid) where trackereventid is not null;
create index if not exists in_relationshipitem_singleeventid
    on relationshipitem (singleeventid) where singleeventid is not null;

create index if not exists in_programtempowner_trackedentityid
    on programtempowner (trackedentityid);
create index if not exists in_programtempownershipaudit_trackedentityid
    on programtempownershipaudit (trackedentityid);
create index if not exists in_programownershiphistory_trackedentityid
    on programownershiphistory (trackedentityid);
