-- https://dhis2.atlassian.net/browse/DHIS2-18694
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
                 JOIN unnest(c.conkey) AS k(attnum) ON true
                 JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum
        WHERE c.contype = 'f'
          AND c.conrelid = 'trackedentityaudit'::regclass
          AND a.attname = 'trackedentity'
        LOOP
            EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',
                           'trackedentityaudit', r.conname);
        END LOOP;
END $$;
