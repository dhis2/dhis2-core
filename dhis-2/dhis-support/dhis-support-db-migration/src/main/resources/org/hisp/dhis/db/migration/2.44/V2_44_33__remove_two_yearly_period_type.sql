-- Remove the obsolete 'TwoYearly' period type, but only if nothing still uses it.
-- If it is still referenced, fail loudly so the operator can fix it manually.

DO $$
    DECLARE
        pt_id     BIGINT;
        ds_count  INTEGER;
        daw_count INTEGER;
    BEGIN
        SELECT periodtypeid
        INTO pt_id
        FROM periodtype
        WHERE name = 'TwoYearly';

        -- Nothing to do if the period type is already gone.
        IF NOT FOUND THEN
            RAISE NOTICE 'periodtype ''TwoYearly'' does not exist - nothing to delete.';
            RETURN;
        END IF;

        -- Count remaining references --------------------------------------------------
        SELECT COUNT(*) INTO ds_count  FROM dataset              WHERE periodtypeid = pt_id;
        SELECT COUNT(*) INTO daw_count FROM dataapprovalworkflow WHERE periodtypeid = pt_id;

        IF ds_count > 0 OR daw_count > 0 THEN
            RAISE EXCEPTION
                'Refusing to delete periodtype ''TwoYearly'' (id=%): still referenced by % dataset(s) and % dataapprovalworkflow(s).',
                pt_id, ds_count, daw_count
                USING HINT = 'Reassign dataset.periodtypeid / dataapprovalworkflow.periodtypeid to a valid periodtype, then re-run this migration. No rows were changed.';
        END IF;

        -- Safe to delete: first the dependent period rows, then the type itself ------
        DELETE FROM period     WHERE periodtypeid = pt_id;
        DELETE FROM periodtype WHERE periodtypeid = pt_id;

        RAISE NOTICE 'Deleted periodtype ''TwoYearly'' (id=%) and its period rows.', pt_id;
    END $$;