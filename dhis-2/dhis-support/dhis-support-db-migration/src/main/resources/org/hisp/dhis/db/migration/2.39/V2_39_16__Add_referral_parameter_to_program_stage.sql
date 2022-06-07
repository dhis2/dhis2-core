-- https://jira.dhis2.org/browse/DHIS2-13301
-- Adding referral parameter to ProgramStage

ALTER TABLE programstage ADD COLUMN IF NOT EXISTS referral BOOLEAN;

UPDATE programstage SET referral = FALSE WHERE referral IS NULL;

ALTER TABLE programstage ALTER COLUMN referral SET DEFAULT false;

ALTER TABLE programstage ALTER COLUMN referral SET NOT NULL;