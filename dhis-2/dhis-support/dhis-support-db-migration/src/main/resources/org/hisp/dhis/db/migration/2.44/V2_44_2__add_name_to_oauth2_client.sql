-- Add the missing `name` column to oauth2_client.
-- Dhis2OAuth2Client extends BaseIdentifiableObject, so the schema exposes a
-- `name` property. Without a mapped column, list queries that ORDER BY
-- `displayName` fail after the move to DB-side ordering (PR #22395).
-- Backfill existing rows from `client_id` before enforcing NOT NULL.
alter table oauth2_client add column if not exists name varchar(230);

update oauth2_client set name = client_id where name is null;

alter table oauth2_client alter column name set not null;
