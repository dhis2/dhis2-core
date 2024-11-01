-- DHIS2-13334: Add twofactortype column to userinfo table
ALTER TABLE userinfo ADD COLUMN IF NOT EXISTS twofactortype character varying(50);