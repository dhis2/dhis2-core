-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11057

-- Add new column "fixColumnHeaders"
ALTER TABLE visualization ADD COLUMN IF NOT EXISTS fixColumnHeaders BOOLEAN;
UPDATE visualization SET fixColumnHeaders = FALSE;
ALTER TABLE visualization ALTER COLUMN fixColumnHeaders SET NOT NULL;

-- Add new column "fixRowHeaders"
ALTER TABLE visualization ADD COLUMN IF NOT EXISTS fixRowHeaders BOOLEAN;
UPDATE visualization SET fixRowHeaders = FALSE;
ALTER TABLE visualization ALTER COLUMN fixRowHeaders SET NOT NULL;
