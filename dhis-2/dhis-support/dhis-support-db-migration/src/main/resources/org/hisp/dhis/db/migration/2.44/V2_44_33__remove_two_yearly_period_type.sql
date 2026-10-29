-- Remove the obsolete 'TwoYearly' period type, but only if nothing still
-- references it via any of the known FK columns.  Otherwise fail loudly and
-- leave the database untouched.

DO $$
DECLARE
    pt_id     BIGINT;
    cnt       INTEGER;
    total     INTEGER := 0;
    offending TEXT    := '';
    -- [table, column] pairs that reference periodtype
    checks    TEXT[][] := ARRAY[
        ['dataset',                            'periodtypeid'],
        ['dataapprovalworkflow',               'periodtypeid'],
        ['configuration',                      'infrastructuralperiodtypeid'],
        ['configuration_dataoutputperiodtype', 'periodtypeid'],
        ['validationrule',                     'periodtypeid'],
        ['predictor',                          'periodtypeid'],
        ['program',                            'expiryperiodtypeid'],
        ['programstage',                       'periodtypeid'],
        ['periodboundary',                     'offsetperiodtypeid']
        ];
BEGIN
    SELECT periodtypeid
    INTO pt_id
    FROM periodtype
    WHERE name = 'TwoYearly';

    IF NOT FOUND THEN
        RAISE NOTICE 'periodtype ''TwoYearly'' does not exist - nothing to delete.';
        RETURN;
    END IF;

    -- Count references in every known FK column --------------------------------
    FOR i IN 1 .. array_length(checks, 1) LOOP
            EXECUTE format('SELECT COUNT(*) FROM %I WHERE %I = $1',
                           checks[i][1], checks[i][2])
                INTO cnt
                USING pt_id;

            IF cnt > 0 THEN
                offending := offending || format(E'\n  - %I.%I: %s row(s)',
                                                 checks[i][1], checks[i][2], cnt);
                total     := total + cnt;
            END IF;
        END LOOP;

    -- Any reference => fail; nothing is deleted -------------------------------
    IF total > 0 THEN
        RAISE EXCEPTION
            'Refusing to delete periodtype ''TwoYearly'' (id=%): still referenced by % row(s) in:%s',
            pt_id, total, offending
            USING HINT = 'Reassign or delete the referencing rows, then re-run this migration. No rows were changed.';
    END IF;

    -- Safe to delete: dependent period rows first, then the periodtype row ----
    DELETE FROM period     WHERE periodtypeid = pt_id;
    DELETE FROM periodtype WHERE periodtypeid = pt_id;

    RAISE NOTICE 'Deleted periodtype ''TwoYearly'' (id=%) and its period rows.', pt_id;
END $$;