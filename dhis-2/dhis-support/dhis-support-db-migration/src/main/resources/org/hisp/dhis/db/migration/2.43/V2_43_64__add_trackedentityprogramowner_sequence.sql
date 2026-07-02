-- DHIS2-21378: give trackedentityprogramowner its own sequence instead of the shared global
-- hibernate_sequence. The table drew ids from hibernate_sequence (<generator class="native"/>);
-- both the JDBC tracker-import write path (TrackedEntityProgramOwnerWriter) and the remaining
-- Hibernate paths (ownership create-or-update and transfer) now use this dedicated sequence.
-- Seed it from the current max id so the first nextval is above all existing rows.
create sequence if not exists trackedentityprogramowner_sequence;
select setval('trackedentityprogramowner_sequence', coalesce(max(trackedentityprogramownerid), 1)) from trackedentityprogramowner;
