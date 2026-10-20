-- Removes leading and trailing whitespace from the name column on insert, so that names
-- differing only by whitespace (e.g. 'Name' vs 'Name ') do not appear as near-duplicate metadata.
-- Whitespace is space, tab, line feed, carriage return, form feed and vertical tab.
CREATE OR REPLACE FUNCTION trim_metadata_name() RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.name IS NOT NULL THEN
        NEW.name := btrim(NEW.name, E' \t\n\r\f\v');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attaches the trim_metadata_name trigger to every metadata table, being a table with both a uid and a
-- text name column. Existing rows are not modified.
DO
$$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (
        SELECT c.table_name
        FROM information_schema.columns c
                 INNER JOIN information_schema.tables t
                            ON c.table_schema = t.table_schema
                                AND c.table_name = t.table_name
        WHERE c.table_schema = current_schema()
          AND t.table_type = 'BASE TABLE'
          AND c.column_name = 'name'
          AND c.data_type IN ('character varying', 'text')
          AND EXISTS (SELECT 1
                      FROM information_schema.columns u
                      WHERE u.table_schema = c.table_schema
                        AND u.table_name = c.table_name
                        AND u.column_name = 'uid')
    ) LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_trim_name ON %I', rec.table_name);
        EXECUTE format(
            'CREATE TRIGGER trg_trim_name BEFORE INSERT ON %I FOR EACH ROW EXECUTE FUNCTION trim_metadata_name()',
            rec.table_name);
    END LOOP;
END;
$$;
