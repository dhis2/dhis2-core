--Recreates the lastupdated index on trackerevent/singleevent, dropped (without being
-- recreated) when V2_43_21__split_event_table.sql split the event table. See
-- V2_43_3__create_lastupdated_index_event_and_enrollment.sql for the original index this
-- restores. DHIS2-21924.
DROP INDEX IF EXISTS "in_trackerevent_lastupdated";
DROP INDEX IF EXISTS "in_singleevent_lastupdated";

CREATE INDEX "in_trackerevent_lastupdated" ON "trackerevent" ("lastupdated");
CREATE INDEX "in_singleevent_lastupdated" ON "singleevent" ("lastupdated");

ANALYZE "trackerevent";
ANALYZE "singleevent";
