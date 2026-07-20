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
