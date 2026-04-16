-- Composite index on programstageinstance (organisationunitid, executiondate) to improve
-- performance of orgUnitMode=SELECTED event queries. Without this index, the planner may
-- fall back to a BitmapAnd across large tables even when a direct organisationunitid
-- predicate is present. This index allows an Index Scan Backward covering both predicates.
-- Note: column is named executiondate in 2.40 (renamed to occurreddate in 2.41+).
create index if not exists idx_event_ou_occurreddate
    on programstageinstance (organisationunitid, executiondate);
