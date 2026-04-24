-- 3-column composite index on (organisationunitid, programstageid, occurreddate).
-- Strictly supersedes the 2-column (ou, date) variant: Capture app queries always
-- specify programstage, so the middle column eliminates rows before the date range
-- is evaluated. IF NOT EXISTS handles upgrades from 2.41 where a 2-column variant
-- was already created. 2.41 equivalent is V2_41_58; master equivalent is V2_43_50.
create index if not exists idx_event_ou_ps_occurreddate
    on event (organisationunitid, programstageid, occurreddate);

-- Drop the 2-column index superseded by the composite above (created in V2_41_58
-- on 2.41 systems that upgrade; a no-op on fresh 2.42 installs).
drop index if exists idx_event_ou_occurreddate;

-- Drop single-column OU index superseded by the composite above.
-- programstageinstance_organisationunitid is defined in the base schema; any query
-- using it is strictly covered by the 3-column composite.
drop index if exists programstageinstance_organisationunitid;

-- Raise enrollmentid statistics so the planner estimates skewed distributions
-- (e.g. a single enrollment owning millions of events) more accurately.
alter table event alter column enrollmentid set statistics 500;
analyze event (enrollmentid);

-- Fix autovacuum thresholds. The default scale_factor=0.2 requires 20% dead tuples
-- before autovacuum fires -- on a 10M+ row event table that threshold is never reached
-- in practice. Tighter thresholds keep statistics and visibility maps current.
alter table event set (
    autovacuum_vacuum_scale_factor   = 0.01,
    autovacuum_analyze_scale_factor  = 0.01,
    autovacuum_vacuum_threshold      = 1000,
    autovacuum_analyze_threshold     = 1000
);
