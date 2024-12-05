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

SELECT COUNT(*) INTO inconsistent_records_count FROM trackedentity WHERE trackedentitytypeid IS NULL;

IF inconsistent_records_count = 0 THEN
    ALTER TABLE IF EXISTS trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
    RAISE NOTICE 'trackedentitytypeid is now set to NOT NULL.';
ELSE
    RAISE NOTICE 'There are still NULL values in trackedentitytypeid. Please review the data.';
END IF;

ELSE
     RAISE NOTICE 'No inconsistencies found, trackedentitytypeid is already populated.';
END IF;
END $$;
