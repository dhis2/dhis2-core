-- Add the missing `name` column to oauth2_authorization.
-- Dhis2OAuth2Authorization extends BaseIdentifiableObject, so the schema
-- exposes a `name` property. Without a mapped column, list queries that
-- ORDER BY `displayName` fail after the move to DB-side ordering (PR #22395).
-- Backfill existing rows from `principal_name` (truncated to column length).
alter table oauth2_authorization add column if not exists name varchar(230);

update oauth2_authorization set name = substring(principal_name, 1, 230) where name is null;

alter table oauth2_authorization alter column name set not null;
