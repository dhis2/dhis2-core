-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11347

DO
$$
    DECLARE
        has_serieskey_column bool;
        has_legendhidekey_column bool;

    BEGIN
        has_serieskey_column := (SELECT EXISTS (SELECT 1
                                    FROM information_schema.columns
                                    WHERE table_name='visualization' AND column_name='serieskey'));

        has_legendhidekey_column := (SELECT EXISTS (SELECT 1
                                        FROM information_schema.columns
                                        WHERE table_name='visualization' AND column_name='legendhidekey'));

        IF has_serieskey_column = FALSE AND has_legendhidekey_column = FALSE THEN
            -- Rename column: "legend" to "serieskey".
            ALTER TABLE visualization RENAME COLUMN legend TO serieskey;

            -- Add new boolean flag related to legend.
            ALTER TABLE visualization ADD COLUMN IF NOT EXISTS legendhidekey BOOLEAN;
            UPDATE visualization SET legendhidekey = FALSE;
        END IF;
    END
$$ LANGUAGE plpgsql
