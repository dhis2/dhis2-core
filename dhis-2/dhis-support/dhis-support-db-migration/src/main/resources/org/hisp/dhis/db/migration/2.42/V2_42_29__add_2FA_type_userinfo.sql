-- DHIS2-13334: Add twofactortype column to userinfo table
ALTER TABLE userinfo
    ADD COLUMN IF NOT EXISTS twofactortype character varying(50) DEFAULT 'NOT_ENABLED' NOT NULL;

-- Set all existing users to have the default twofactortype TOTP, if the secret column is not empty and does not start with 'APPROVAL_'.
UPDATE userinfo
SET twofactortype = 'TOTP_ENABLED'
WHERE secret IS NOT NULL
  AND secret NOT LIKE 'APPROVAL_%';