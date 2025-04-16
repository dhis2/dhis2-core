--Creates indexes on the lastupdated column of the event and enrollment tables
-- to improve query performance for data statistics operations.
DROP INDEX IF EXISTS "in_event_lastupdated";
DROP INDEX IF EXISTS "in_enrollment_lastupdated";

CREATE INDEX "in_event_lastupdated" ON "event" ("lastupdated");
CREATE INDEX "in_enrollment_lastupdated" ON "enrollment" ("lastupdated");