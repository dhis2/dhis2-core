-- DHIS2-22034: the datavalue audit trigger introduced in V2_43_17 hardcodes
-- nextval('hibernate_sequence') for datavalueauditid, instead of the dedicated
-- datavalueaudit_sequence that DataValueChangelog.hbm.xml declares as the id generator.
-- As a result datavalueaudit_sequence has been dead since 2.43, while hibernate_sequence
-- takes on extra unrelated churn.

-- Reseed the dedicated sequence past any ids already inserted via hibernate_sequence since
-- V2_43_17, so switching the trigger back to it below cannot collide with an existing PK value.
select setval('datavalueaudit_sequence', coalesce((select max(datavalueauditid) from datavalueaudit), 1));

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
           nextval('datavalueaudit_sequence'),
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
