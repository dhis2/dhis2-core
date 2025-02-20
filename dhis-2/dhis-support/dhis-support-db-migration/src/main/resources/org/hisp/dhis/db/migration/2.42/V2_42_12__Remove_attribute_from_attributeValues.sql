-- a loop over all tables that have a attributevalues column
-- and for each of such tables and each attribute
-- we update each row and remove the "attribute" property
--
-- unfortunately when removing properties form a JSON(B) column value
-- the exact path needs to be specified which is why this has 2nd loop level
DO
$do$
    DECLARE
        t varchar;
        a varchar;
    BEGIN
        FOR t IN SELECT t.table_name FROM information_schema.tables t
                INNER JOIN information_schema.columns c ON c.table_name = t.table_name AND c.table_schema = t.table_schema
                WHERE c.column_name = 'attributevalues' AND c.data_type = 'jsonb' AND t.table_type = 'BASE TABLE' LOOP
            FOR a IN SELECT DISTINCT uid FROM attribute LOOP
                EXECUTE format('update %I set attributevalues = attributevalues #- ''{%s,attribute}'' ', t,a);
            END LOOP;
        END LOOP;
    END;
$do$