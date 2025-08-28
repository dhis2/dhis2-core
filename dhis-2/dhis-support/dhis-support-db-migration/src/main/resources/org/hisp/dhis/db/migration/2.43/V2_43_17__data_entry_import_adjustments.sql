-- Make iso column non-null after 2_43_10 has filled in the value
ALTER TABLE period
    ALTER COLUMN iso SET NOT NULL;

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


-- Do audit via DB trigger function
CREATE OR REPLACE FUNCTION log_datavalue_audit()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR
       (TG_OP = 'UPDATE' AND (
           OLD.value IS DISTINCT FROM NEW.value OR OLD.deleted IS DISTINCT FROM NEW.deleted
       ))
    THEN
        INSERT INTO datavalueaudit (
            datavalueauditid,
            created,
            modifiedby,
            dataelementid,
            periodid,
            organisationunitid,
            categoryoptioncomboid,
            attributeoptioncomboid,
            value,
            audittype
        )
        VALUES (
           nextval('hibernate_sequence'),
           now(),
           left(NEW.storedby, 100),
           NEW.dataelementid,
           NEW.periodid,
           NEW.sourceid,
           NEW.categoryoptioncomboid,
           NEW.attributeoptioncomboid,
           NEW.value,
           CASE
               WHEN TG_OP = 'INSERT' THEN 'CREATE'
               WHEN NEW.deleted AND (OLD.deleted IS NULL OR NOT OLD.deleted) THEN 'DELETE'
               ELSE 'UPDATE'
           END
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_datavalue_audit
    AFTER INSERT OR UPDATE ON datavalue
    FOR EACH ROW
    EXECUTE FUNCTION log_datavalue_audit();