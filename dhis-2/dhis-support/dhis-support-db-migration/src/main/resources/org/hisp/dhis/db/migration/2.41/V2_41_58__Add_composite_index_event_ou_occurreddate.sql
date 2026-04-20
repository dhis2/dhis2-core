-- 3-column composite index on (organisationunitid, programstageid, occurreddate).
-- Strictly supersedes the 2-column (ou, date) variant: Capture app queries always
-- specify programstage, so the middle column eliminates rows before the date range
-- is evaluated. Master made the same index decision in V2_43_50.
create index if not exists idx_event_ou_ps_occurreddate
    on event (organisationunitid, programstageid, occurreddate);

-- Drop duplicate lastupdated index left over from the programstageinstance→event rename.
-- idx_programstageinstance_lastupdated and in_event_lastupdated cover the same column;
-- the newer name is retained.
drop index if exists idx_programstageinstance_lastupdated;

-- Drop single-column OU index superseded by the composite above.
-- Master precedent: V2_43_50__replace_singleevent_indexes.sql dropped the equivalent
-- in_singleevent_organisationunitid index when composites were introduced.
drop index if exists programstageinstance_organisationunitid;

-- Raise enrollmentid statistics so the planner estimates skewed distributions
-- (e.g. a single enrollment owning millions of events) more accurately.
alter table event alter column enrollmentid set statistics 500;
analyze event (enrollmentid);

-- Fix autovacuum thresholds. The default scale_factor=0.2 requires 20% dead tuples
-- before autovacuum fires -- on a 10M+ row event table that threshold is never reached
-- in practice. Tighter thresholds keep table statistics and visibility maps current.
alter table event set (
    autovacuum_vacuum_scale_factor   = 0.01,
    autovacuum_analyze_scale_factor  = 0.01,
    autovacuum_vacuum_threshold      = 1000,
    autovacuum_analyze_threshold     = 1000
);
