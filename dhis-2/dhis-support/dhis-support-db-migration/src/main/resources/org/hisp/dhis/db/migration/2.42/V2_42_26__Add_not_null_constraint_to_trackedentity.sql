DO $$
DECLARE
results RECORD;
inconsistent_records_count INTEGER;
BEGIN
SELECT COUNT(*) INTO inconsistent_records_count FROM trackedentity WHERE trackedentitytypeid IS NULL;

IF inconsistent_records_count > 0 THEN
    RAISE NOTICE 'Inconsistencies found: %', inconsistent_records_count;

    FOR results IN
    SELECT programid, trackedentityid FROM enrollment WHERE trackedentityid IN (
        SELECT trackedentityid
        FROM trackedentity
        WHERE trackedentitytypeid IS NULL
    )
    LOOP
    UPDATE trackedentity
    SET trackedentitytypeid = (
        SELECT trackedentitytypeid
        FROM program
        WHERE programid = results.programid
    )
    WHERE trackedentityid = results.trackedentityid;

    RAISE NOTICE 'Updated trackedentity: programid = %, trackedentityid = %', results.programid, results.trackedentityid;
    END LOOP;

        ALTER TABLE trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
        RAISE EXCEPTION 'The database contains inconsistent data that must be resolved before re-running this migration. Please refer to the migration notes for detailed instructions on how to address this issue:
             https://github.com/dhis2/dhis2-releases/blob/master/releases/2.42/migration-notes.md#null-tracked-entity-type. Detailed error message: %', SQLERRM;
    END IF;
ELSE
     RAISE NOTICE 'No inconsistencies found, trackedentitytypeid is already populated.';
     ALTER TABLE trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
END IF;
END $$;
