-- This script is responsible for creating a new JSONB column in the Visualization table.
-- See Feature DHIS2-10079.

-- What it does?
-- 1) creates a new JSONB column("outlieranalysis") into Visualization table;


-- Step 1) creates the JSONB column("outlieranalysis") into Visualization table;
alter table if exists visualization add column if not exists outlieranalysis jsonb;
