-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11347

-- Rename column: "legendhidekey" to "legendshowkey".
ALTER TABLE visualization RENAME COLUMN legendhidekey TO legendshowkey;
