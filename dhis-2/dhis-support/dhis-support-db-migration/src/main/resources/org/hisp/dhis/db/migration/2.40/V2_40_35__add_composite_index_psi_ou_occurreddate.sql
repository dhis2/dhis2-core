-- 3-column composite index on (organisationunitid, programstageid, executiondate).
-- Strictly supersedes the 2-column (ou, date) variant: Capture app queries always
-- specify programstage, so the middle column eliminates rows before the date range
-- is evaluated. Master made the same index decision in V2_43_50. 2.41 equivalent
-- is V2_41_58. Note: column is named executiondate in 2.40 (renamed to occurreddate in 2.41+).
create index if not exists idx_event_ou_ps_occurreddate
    on programstageinstance (organisationunitid, programstageid, executiondate);

-- Drop single-column OU index superseded by the composite above.
-- The programstageinstance_organisationunitid index is defined in the base schema;
-- any query using it is strictly covered by the 3-column composite.
drop index if exists programstageinstance_organisationunitid;

-- Raise programinstanceid statistics so the planner estimates skewed distributions
-- (e.g. a single enrollment owning millions of events) more accurately.
-- 2.41 equivalent raises enrollmentid statistics on the renamed event table.
alter table programstageinstance alter column programinstanceid set statistics 500;
analyze programstageinstance (programinstanceid);

-- Fix autovacuum thresholds. The default scale_factor=0.2 requires 20% dead tuples
-- before autovacuum fires -- on a 10M+ row programstageinstance table that threshold
-- is never reached in practice. Tighter thresholds keep statistics and visibility
-- maps current. 2.41 equivalent applies identical settings to the renamed event table.
alter table programstageinstance set (
    autovacuum_vacuum_scale_factor   = 0.01,
    autovacuum_analyze_scale_factor  = 0.01,
    autovacuum_vacuum_threshold      = 1000,
    autovacuum_analyze_threshold     = 1000
);
