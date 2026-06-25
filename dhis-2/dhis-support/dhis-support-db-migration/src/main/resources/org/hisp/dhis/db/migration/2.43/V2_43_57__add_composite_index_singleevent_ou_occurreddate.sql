-- Composite index on singleevent (organisationunitid, occurreddate) to improve
-- performance of orgUnitMode=SELECTED/CHILDREN/DESCENDANTS queries. Without this index,
-- the planner may fall back to a BitmapAnd across large tables even when a direct
-- organisationunitid predicate is present. This index allows an Index Scan Backward
-- covering both predicates.
-- IF NOT EXISTS handles upgrades from 2.42 where the equivalent index already exists
-- on the event table (now split into singleevent and trackerevent).
create index if not exists idx_event_ou_occurreddate
    on singleevent (organisationunitid, occurreddate);
