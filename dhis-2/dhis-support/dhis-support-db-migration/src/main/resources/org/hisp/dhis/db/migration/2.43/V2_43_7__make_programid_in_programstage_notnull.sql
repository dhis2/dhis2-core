DO $$
    BEGIN
        alter table if exists programstage alter column programid set not null;
    EXCEPTION
        WHEN not_null_violation THEN
            RAISE EXCEPTION 'There is inconsistent data in your DB. Please check https://github.com/dhis2/dhis2-releases/blob/master/releases/2.43/migration-notes.md#null-program to have more information on the issue and to find ways to fix it. Detailed error message: %', SQLERRM;

    END;
$$;
