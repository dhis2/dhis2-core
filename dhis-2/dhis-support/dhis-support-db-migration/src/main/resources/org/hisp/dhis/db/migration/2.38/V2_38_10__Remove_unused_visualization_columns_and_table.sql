-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-11951
-- It drops the table "datasetcompleteness" and remove unused columns in "interpretation"

-- Drop legacy unused table "datasetcompleteness"
drop table if exists datasetcompleteness;

-- Remove unused visualization columns from "interpretation"
alter table interpretation drop column if exists "reporttableid";
alter table interpretation drop column if exists "chartid";
