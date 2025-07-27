-- Performance:
-- Category model mapping table indexes
-- required to make "reverse" lookup not do a full table scan
-- when doing a JOIN to get all COCs for a categorycomboid
CREATE INDEX IF NOT EXISTS idx_categorycombos_optioncombos_categorycomboid ON categorycombos_optioncombos(categorycomboid);


-- Remove data import settings no longer used
-- (not strictly needed but good to keep it clean in DB)
DELETE FROM systemsetting WHERE name
    IN (
        'keyDataImportStrictDataElements',
        'keyDataImportStrictCategoryOptionCombos',
        'keyDataImportRequireCategoryOptionCombo',
        'keyDataImportStrictDataSetApproval',
        'keyDataImportStrictDataSetLocking',
        'keyDataImportStrictDataSetInputPeriods'
       );