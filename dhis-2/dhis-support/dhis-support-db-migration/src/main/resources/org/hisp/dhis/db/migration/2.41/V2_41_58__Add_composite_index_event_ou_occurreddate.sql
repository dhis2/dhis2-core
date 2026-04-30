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
