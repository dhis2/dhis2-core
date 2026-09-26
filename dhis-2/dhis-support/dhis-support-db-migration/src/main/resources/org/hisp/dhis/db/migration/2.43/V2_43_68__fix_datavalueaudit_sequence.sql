-- DHIS2-22034: the datavalue audit trigger introduced in V2_43_17 hardcodes
-- nextval('hibernate_sequence') for datavalueauditid, instead of the dedicated
-- datavalueaudit_sequence that DataValueChangelog.hbm.xml declares as the id generator.
-- As a result datavalueaudit_sequence has been dead since V2_43_17, while hibernate_sequence
-- takes on extra unrelated churn.

-- Reseed the dedicated sequence past any ids already inserted via hibernate_sequence since
-- V2_43_17, so switching the trigger back to it below cannot collide with an existing PK value.
select setval('datavalueaudit_sequence', coalesce((select max(datavalueauditid) from datavalueaudit), 1));

-- DHIS2-22059: log_datavalue_audit() was originally created by V2_43_17. CREATE OR REPLACE
-- FUNCTION on a pre-existing function requires the connecting role to already own it (or be
-- superuser) - on environments where migrations now run as a different role than the one
-- that ran V2_43_17 (e.g. a database refreshed from a dump/snapshot without normalizing
-- object ownership), replacing it in place fails with "must be owner of function
-- log_datavalue_audit" and blocks this migration (and, since migrations run as a single
-- grouped transaction, every migration after it) from ever applying.
--
-- Instead, define the corrected trigger logic under a new function name (which the current
-- role always owns, since it's the one creating it) and repoint the trigger to it.
-- Retargeting a trigger only requires ownership of the table it's defined on (datavalue),
-- not of the function it calls, so this works regardless of who owns the old
-- log_datavalue_audit() function.
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
