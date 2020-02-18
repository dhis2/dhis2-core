-- This script is responsible for setting default values for the following columns.
-- See Feature DHIS2-7946

-- digitgroupseparator
-- displaydensity
-- fontsize


-- 1) Setting digitgroupseparator defaults
UPDATE visualization SET digitgroupseparator = 'SPACE' WHERE COALESCE(digitgroupseparator, '') = '' OR digitgroupseparator IS NULL;


-- 2) Setting displaydensity defaults
UPDATE visualization SET displaydensity = 'NORMAL' WHERE COALESCE(displaydensity, '') = '' OR displaydensity IS NULL;


-- 3) Setting fontsize defaults
UPDATE visualization SET fontsize = 'NORMAL' WHERE COALESCE(fontsize, '') = '' OR fontsize IS NULL;
