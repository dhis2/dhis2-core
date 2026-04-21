-- Add the missing `name` column to oauth2_client.
-- Dhis2OAuth2Client extends BaseIdentifiableObject, so the schema exposes a
-- `name` property. Without a mapped column, list queries that ORDER BY
-- `displayName` fail after the move to DB-side ordering (PR #22395).
-- Column is nullable; the controller defaults name = clientId on save. The
-- settings UI currently has no name field, so the schema validator at
-- /api/schemas/oAuth2Client would reject every UI POST if name were required.
-- Backfill existing rows from client_id (truncated to the column length) so
-- sorting has a sensible value. client_id is varchar(255) upstream, so the
-- substring guard prevents "value too long" failures on long client IDs.
alter table oauth2_client add column if not exists name varchar(230);

update oauth2_client set name = substring(client_id, 1, 230) where name is null;
