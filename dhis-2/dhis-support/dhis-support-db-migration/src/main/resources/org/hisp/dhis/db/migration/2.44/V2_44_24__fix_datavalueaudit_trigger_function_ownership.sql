-- DHIS2-22034 follow-up: V2_44_23 redefined log_datavalue_audit() via
-- CREATE OR REPLACE FUNCTION. PostgreSQL only allows that when the connecting role
-- already owns the function (or is superuser). log_datavalue_audit() was originally
-- created by V2_43_17, so on any environment where the role running migrations now
-- differs from the role that ran V2_43_17 (e.g. a database refreshed from a dump/
-- snapshot without normalizing object ownership), V2_44_23 fails with
-- "must be owner of function log_datavalue_audit".
--
-- Fix: define the trigger logic under a new function name instead of replacing the
-- existing one, then repoint the trigger to it. Retargeting a trigger only requires
-- ownership of the table it is defined on (datavalue), not of the function it calls,
-- so this works regardless of who owns the old log_datavalue_audit() function.
CREATE OR REPLACE FUNCTION log_datavalue_audit_v2()
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

DROP TRIGGER IF EXISTS trg_datavalue_audit ON datavalue;

CREATE TRIGGER trg_datavalue_audit
    AFTER INSERT OR UPDATE ON datavalue
    FOR EACH ROW
    EXECUTE FUNCTION log_datavalue_audit_v2();

-- Best-effort cleanup of the now-unused function. Only succeeds when we happen to own
-- it; when we don't (the exact scenario this migration exists to route around), skip
-- rather than fail the migration.
DO $$
BEGIN
    DROP FUNCTION IF EXISTS log_datavalue_audit();
EXCEPTION
    WHEN insufficient_privilege THEN
        NULL;
END;
$$;
