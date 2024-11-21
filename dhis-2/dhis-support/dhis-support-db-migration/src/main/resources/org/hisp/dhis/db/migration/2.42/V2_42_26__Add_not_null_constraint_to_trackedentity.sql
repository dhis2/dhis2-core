DO $$
DECLARE
dummy_tet_id INT;
dummy_tet_uid VARCHAR(11);
BEGIN
    -- If there are NULL values in trackedentitytypeid, proceed with the update and NOT NULL constraint
    IF EXISTS (SELECT 1 FROM trackedentity WHERE trackedentitytypeid IS NULL) THEN
        -- Step 2: Generate a dummy ID for trackedentitytype
        SELECT COALESCE(MAX(trackedentitytypeid), 0) + 1 INTO dummy_tet_id FROM trackedentitytype;

        -- Generate a unique UID for the dummy entry
        LOOP
        SELECT generate_uid() INTO dummy_tet_uid;
        -- Ensure the UID is unique in trackedentitytype
        EXIT WHEN NOT EXISTS (SELECT 1 FROM trackedentitytype WHERE uid = dummy_tet_uid);
        END LOOP;

        -- Step 3: Insert dummy TrackedEntityType using generated ID and UID
        INSERT INTO trackedentitytype(
                trackedentitytypeid, uid, code, created, lastupdated, name, description, formname, style)
                VALUES (dummy_tet_id, dummy_tet_uid, 'dummy_tet', now(), now(), 'dummy_tet', 'Placeholder for NULL values', 'dummy_tet', '{}'::jsonb);

        -- Step 4: Update rows with NULL values in trackedentitytypeid
        UPDATE trackedentity
        SET trackedentitytypeid = dummy_tet_id
        WHERE trackedentitytypeid IS NULL;

        -- Step 5: Add NOT NULL constraint to trackedentitytypeid
        ALTER TABLE trackedentity ALTER COLUMN trackedentitytypeid SET NOT NULL;
    END IF;
END $$;
