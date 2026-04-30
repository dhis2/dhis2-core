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
