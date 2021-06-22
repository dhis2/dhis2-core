-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11347

-- Rename column: "legend" to "serieskey".
ALTER TABLE visualization RENAME COLUMN legend TO serieskey;

-- Add new boolean flag related to legend.
ALTER TABLE visualization ADD COLUMN IF NOT EXISTS legendhidekey BOOLEAN DEFAULT FALSE;
