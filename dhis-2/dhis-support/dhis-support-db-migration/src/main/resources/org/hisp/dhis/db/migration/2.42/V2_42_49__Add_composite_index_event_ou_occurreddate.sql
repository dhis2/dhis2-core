-- Adds a composite index on (organisationunitid, occurreddate) to the event table.
-- This supports efficient orgUnitMode=SELECTED queries which filter by both OU and
-- date range, avoiding an unstable BitmapAnd plan for high-cardinality org units.
-- IF NOT EXISTS handles upgrades from 2.41 where this index was already created.
create index if not exists idx_event_ou_occurreddate
    on event (organisationunitid, occurreddate);
