-- This script relates to the issue https://jira.dhis2.org/browse/DHIS2-12279
-- Adds CategoryOptionCombo id as foreign key in smscodes table

ALTER table smscodes ALTER COLUMN optionid type bigint;
ALTER table smscodes DROP CONSTRAINT IF EXISTS fk_categoryoptioncombo_categoryoptioncomboid;
ALTER table smscodes ADD CONSTRAINT fk_categoryoptioncombo_categoryoptioncomboid FOREIGN KEY (optionid)
    REFERENCES categoryoptioncombo(categoryoptioncomboid);