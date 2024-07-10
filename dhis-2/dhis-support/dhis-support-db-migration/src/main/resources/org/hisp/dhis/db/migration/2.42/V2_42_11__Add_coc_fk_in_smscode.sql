-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-12279
-- Adds CategoryOptionCombo id as foreign key in sms codes table

ALTER table smscodes ALTER COLUMN optionid type bigint;

-- Set invalid COC to default
UPDATE smscodes SET optionid =
    (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE name = 'default')
WHERE optionid NOT IN
    (SELECT DISTINCT categoryoptioncomboid FROM categoryoptioncombo) OR optionid IS NULL;

ALTER table smscodes DROP CONSTRAINT IF EXISTS fk_categoryoptioncombo_categoryoptioncomboid;
ALTER table smscodes ADD CONSTRAINT fk_categoryoptioncombo_categoryoptioncomboid FOREIGN KEY (optionid)
    REFERENCES categoryoptioncombo(categoryoptioncomboid);