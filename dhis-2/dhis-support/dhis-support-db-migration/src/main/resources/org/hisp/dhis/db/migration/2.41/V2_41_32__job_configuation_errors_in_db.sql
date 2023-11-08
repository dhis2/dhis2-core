-- adds error tracking to the job configuration table
-- this comes in two parts, the errors themself are added to the existing progress JSON object
-- the "index" of the errors, the list of error codes, from the full error description
-- is extracted and kept as a space seperated list in a new column "error_codes"
-- the column is null for row that existed before the feature and did not run since
-- the column is an empty string if there were no errors
-- the column is a list of error codes, like "E1100 E1105" if there where these error
-- list of error codes are always sorted by their number to allow combination searches using patterns

alter table jobconfiguration
    add column if not exists errorcodes text;