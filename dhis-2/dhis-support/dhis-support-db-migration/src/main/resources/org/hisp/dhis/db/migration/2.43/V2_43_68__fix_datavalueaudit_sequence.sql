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
