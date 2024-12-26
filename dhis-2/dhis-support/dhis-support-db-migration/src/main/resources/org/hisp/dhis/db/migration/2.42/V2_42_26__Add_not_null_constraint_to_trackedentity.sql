DO $$
DECLARE
inconsistent_records_count INTEGER;
BEGIN
SELECT COUNT(*) INTO inconsistent_records_count FROM trackedentity WHERE trackedentitytypeid IS NULL;
IF inconsistent_records_count > 0 THEN
        RAISE NOTICE 'Inconsistencies found: %', inconsistent_records_count;

    UPDATE trackedentity
    SET trackedentitytypeid = (
        SELECT program.trackedentitytypeid
        FROM program
                 JOIN enrollment ON program.programid = enrollment.programid
        WHERE enrollment.trackedentityid = trackedentity.trackedentityid
    )
    WHERE trackedentitytypeid IS NULL
      AND EXISTS (
            SELECT 1
            FROM enrollment
                     JOIN program ON enrollment.programid = program.programid
            WHERE enrollment.trackedentityid = trackedentity.trackedentityid
        );
END IF;
    ALTER TABLE IF EXISTS trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
EXCEPTION
    WHEN not_null_violation THEN
        RAISE EXCEPTION 'The database contains inconsistent data that must be resolved before re-running this migration. Please refer to the migration notes for detailed instructions: https://github.com/dhis2/dhis2-releases/blob/master/releases/2.42/migration-notes.md#null-tracked-entity-type. Detailed error message: %', SQLERRM;
END $$;