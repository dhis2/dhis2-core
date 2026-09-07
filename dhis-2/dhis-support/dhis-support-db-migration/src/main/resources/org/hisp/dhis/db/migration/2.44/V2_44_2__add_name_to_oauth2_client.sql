-- Add the missing `name` column to oauth2_client.
-- Backfill existing rows from client_id (truncated to the column length),
-- then enforce NOT NULL to match the Hibernate mapping and the standard
-- BaseIdentifiableObject contract.
alter table oauth2_client add column if not exists name varchar(230);

update oauth2_client set name = substring(client_id, 1, 230) where name is null;

alter table oauth2_client alter column name set not null;
