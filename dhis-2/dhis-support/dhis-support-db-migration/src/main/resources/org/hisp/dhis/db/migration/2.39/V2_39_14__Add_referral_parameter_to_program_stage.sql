-- https://jira.dhis2.org/browse/DHIS2-13301
-- Adding referral parameter to ProgramStage

ALTER TABLE programstage ADD COLUMN IF NOT EXISTS referral BOOLEAN DEFAULT false;
