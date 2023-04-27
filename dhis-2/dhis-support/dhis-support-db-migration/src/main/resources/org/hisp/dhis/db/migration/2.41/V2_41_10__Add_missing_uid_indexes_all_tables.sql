-- Generates UID codes, same as CodeGenerator.generateUid()
CREATE OR REPLACE FUNCTION generate_code(code_size INTEGER) RETURNS TEXT AS
$$
DECLARE
    letters TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    allowed_chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    number_of_codepoints INTEGER := LENGTH(allowed_chars);
    random_chars TEXT := '';
    i INTEGER;
BEGIN

    -- First char should be a letter
    random_chars := SUBSTRING(letters FROM (RANDOM() * (LENGTH(letters) - 1) + 1)::INTEGER FOR 1);

    FOR i IN 2..code_size LOOP
            random_chars := random_chars || SUBSTRING(allowed_chars FROM (RANDOM() * (number_of_codepoints - 1) + 1)::INTEGER FOR 1);
        END LOOP;

    RETURN random_chars;
END;
$$ LANGUAGE plpgsql;

-- Updates uid rows if they are null, then alters the column to be not null
CREATE OR REPLACE FUNCTION alter_uid_not_null(p_table_name TEXT) RETURNS VOID AS $$
DECLARE
    v_sql TEXT;
BEGIN
    -- Update the table
    v_sql := format('UPDATE %I SET uid = generate_code(11) WHERE uid IS NULL;', p_table_name);
    EXECUTE v_sql;

    -- Alter the table
    v_sql := format('ALTER TABLE %I ALTER COLUMN uid SET NOT NULL;', p_table_name);
    EXECUTE v_sql;
END;
$$ LANGUAGE plpgsql;

-- Makes the uid column unique, only if there are no duplicates
CREATE OR REPLACE FUNCTION make_uid_unique(p_table_name TEXT) RETURNS BOOLEAN AS
$$
DECLARE
    duplicate_count INTEGER;
BEGIN
    EXECUTE format('SELECT COUNT(*) FROM (SELECT uid FROM %I GROUP BY uid HAVING COUNT(*) > 1) AS duplicates', p_table_name)
        INTO duplicate_count;

    IF duplicate_count = 0 THEN
        EXECUTE format('DROP INDEX IF EXISTS uk_%I_uid', p_table_name);
        EXECUTE format('CREATE UNIQUE INDEX uk_%I_uid ON %I (uid)', p_table_name, p_table_name);
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Returns a list of tables that have a uid column
CREATE OR REPLACE FUNCTION get_tables_with_uid_column()
    RETURNS TABLE
            (
                table_schema VARCHAR,
                table_name   VARCHAR
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT c.table_schema::VARCHAR,
               c.table_name::VARCHAR
        FROM information_schema.columns c
        WHERE c.column_name = 'uid'
        GROUP BY c.table_schema,
                 c.table_name;
END;
$$ LANGUAGE plpgsql;

-- Returns a list of tables that have a uid column but no index on it
CREATE OR REPLACE FUNCTION get_tables_missing_uid_index()
    RETURNS TABLE (table_schema VARCHAR, table_name VARCHAR) AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (
        SELECT * FROM get_tables_with_uid_column()
    ) LOOP
            IF NOT EXISTS (
                SELECT 1
                FROM pg_indexes
                WHERE
                        schemaname = rec.table_schema
                  AND tablename = rec.table_name
                  AND indexdef LIKE '%(uid)%'
            ) THEN
                table_schema := rec.table_schema;
                table_name := rec.table_name;
                RETURN NEXT;
            END IF;
        END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Loops through all tables that have a uid column but no index on it and alters the column to be not null
CREATE OR REPLACE FUNCTION alter_uid_not_null_all_tables_missing() RETURNS VOID AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (
        SELECT * FROM get_tables_missing_uid_index()
    ) LOOP
            PERFORM alter_uid_not_null(format('%I', rec.table_name));
        END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Loops through all tables that have a uid column but no index on it and makes the uid column unique
CREATE OR REPLACE FUNCTION create_missing_uid_indexes_all_tables() RETURNS VOID AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (
        SELECT * FROM get_tables_missing_uid_index()
    ) LOOP
            IF rec.table_name <> 'audit' THEN
                PERFORM make_uid_unique(format('%I', rec.table_name));
            END IF;
        END LOOP;
END;
$$ LANGUAGE plpgsql;


BEGIN;
    SELECT alter_uid_not_null_all_tables_missing();
    SELECT create_missing_uid_indexes_all_tables();
COMMIT;
