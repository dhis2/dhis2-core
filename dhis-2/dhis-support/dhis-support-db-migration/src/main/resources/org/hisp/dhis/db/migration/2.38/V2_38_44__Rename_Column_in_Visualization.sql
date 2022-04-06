-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11347

DO
$$
    DECLARE
        has_legendshowkey_column bool;

    BEGIN
        has_legendshowkey_column := (SELECT EXISTS (SELECT 1
                                        FROM information_schema.columns
                                        WHERE table_name='visualization' AND column_name='legendshowkey'));

        IF has_legendshowkey_column = FALSE THEN
            -- Rename column: "legendhidekey" to "legendshowkey".
            ALTER TABLE visualization RENAME COLUMN legendhidekey TO legendshowkey;
        END IF;
    END
$$ LANGUAGE plpgsql
