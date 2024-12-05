DO $$
BEGIN

ALTER TABLE IF EXISTS trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
EXCEPTION
     WHEN not_null_violation THEN
         RAISE EXCEPTION 'The database contains inconsistent data that must be resolved before re-running this migration. Please refer to the migration notes for detailed instructions on how to address this issue:
             https://github.com/dhis2/dhis2-releases/blob/master/releases/2.42/migration-notes.md#null-trackedentitytype. Detailed error message: %', SQLERRM;
END
$$;
