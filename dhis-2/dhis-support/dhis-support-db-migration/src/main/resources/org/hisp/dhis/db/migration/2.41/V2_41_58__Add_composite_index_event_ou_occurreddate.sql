-- Adds a composite index on (organisationunitid, occurreddate) to the event table.
-- This supports efficient orgUnitMode=SELECTED queries which filter by both OU and
-- date range, avoiding an unstable BitmapAnd plan for high-cardinality org units.
create index if not exists idx_event_ou_occurreddate
    on event (organisationunitid, occurreddate);
