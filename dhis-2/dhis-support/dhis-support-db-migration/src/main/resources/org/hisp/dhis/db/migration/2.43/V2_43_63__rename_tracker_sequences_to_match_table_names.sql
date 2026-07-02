-- DHIS2-21378: align tracker sequence names with the current table names and give
-- relationship/relationshipitem their own sequences instead of the shared hibernate_sequence.

-- Tables were renamed long ago (trackedentityinstance -> trackedentity,
-- programinstance -> enrollment) but the sequences kept the legacy names.
alter sequence if exists trackedentityinstance_sequence rename to trackedentity_sequence;
alter sequence if exists programinstance_sequence rename to enrollment_sequence;

-- After the event table split (V2_43_21) the SingleEvent Hibernate mapping kept drawing ids
-- from the legacy programstageinstance_sequence while the JDBC import path used the canonical
-- singleevent_sequence, so the two counters diverged. Resync the canonical sequence to the
-- table's max id and drop the legacy sequence.
select setval('singleevent_sequence', coalesce(max(eventid), 1)) from singleevent;
drop sequence if exists programstageinstance_sequence;

-- relationship and relationshipitem drew ids from the shared, global hibernate_sequence
-- (<generator class="native"/>). Give them dedicated sequences, seeded from the current max ids.
create sequence if not exists relationship_sequence;
select setval('relationship_sequence', coalesce(max(relationshipid), 1)) from relationship;

create sequence if not exists relationshipitem_sequence;
select setval('relationshipitem_sequence', coalesce(max(relationshipitemid), 1)) from relationshipitem;
