-- DHIS2-13334: Add twofactortype column to userinfo table
ALTER TABLE userinfo
    ADD COLUMN IF NOT EXISTS twofactortype character varying(50);

-- Set all existing users to have the default twofactortype TOTP, if the secret column is not empty.
UPDATE userinfo
SET twofactortype = 'TOTP_ENABLED'
WHERE secret IS NOT NULL;
