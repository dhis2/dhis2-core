DO $$
BEGIN

ALTER TABLE trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
EXCEPTION
     WHEN not_null_violation THEN
         RAISE EXCEPTION 'Database contains inconsistent data. For more information about this issue and steps to resolve it, please refer to the migration notes:
             https://github.com/dhis2/dhis2-releases/blob/master/releases/2.42/migration-notes.md#null-trackedentitytype. Detailed error message: %', SQLERRM;
END
$$;
