-- https://jira.dhis2.org/browse/DHIS2-13333
-- Add require 2fa option

ALTER TABLE userinfo ADD COLUMN IF NOT EXISTS requires2fa BOOLEAN default false;

