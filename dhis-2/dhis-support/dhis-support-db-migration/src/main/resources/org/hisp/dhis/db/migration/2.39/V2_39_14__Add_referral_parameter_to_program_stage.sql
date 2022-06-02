-- https://jira.dhis2.org/browse/DHIS2-13301
-- Adding referral parameter to ProgramStage

ALTER TABLE programstage
    ADD COLUMN if not exists referral boolean;

UPDATE programstage SET referral = false WHERE referral IS NULL;

ALTER TABLE programstage
    ALTER COLUMN referral SET NOT NULL;