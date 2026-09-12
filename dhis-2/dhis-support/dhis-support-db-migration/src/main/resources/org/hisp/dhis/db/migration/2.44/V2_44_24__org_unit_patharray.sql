-- 1. add patharray column
ALTER TABLE organisationunit ADD COLUMN IF NOT EXISTS patharray varchar(11)[];

DO $$
    DECLARE
        r record;
    BEGIN
        FOR r IN
            SELECT organisationunitid, uid, parentid
            FROM organisationunit
            ORDER BY hierarchylevel NULLS FIRST, organisationunitid
        LOOP
            IF r.parentid IS NULL THEN
                UPDATE organisationunit
                SET patharray = ARRAY[r.uid]::varchar(11)[]
                WHERE organisationunitid = r.organisationunitid;
            ELSE
                UPDATE organisationunit u
                SET patharray = p.patharray || u.uid
                FROM organisationunit p
                WHERE u.organisationunitid = r.organisationunitid
                  AND p.organisationunitid = r.parentid
                  AND p.patharray IS NOT NULL;
            END IF;
        END LOOP;
END $$;

ALTER TABLE organisationunit ALTER COLUMN patharray SET NOT NULL;


-- 2. Change path and level columns into columns generated from patharray
CREATE OR REPLACE FUNCTION ou_path_join(varchar(11)[])
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    STRICT
AS $$
SELECT '/' || array_to_string($1, '/');
$$;

ALTER TABLE organisationunit DROP COLUMN path;
ALTER TABLE organisationunit ADD COLUMN path varchar(255)
    GENERATED ALWAYS AS (ou_path_join(patharray)) STORED;

ALTER TABLE organisationunit DROP COLUMN hierarchylevel;
ALTER TABLE organisationunit ADD COLUMN hierarchylevel integer
    GENERATED ALWAYS AS (array_length(patharray, 1)) STORED;

-- 3. (re) create indexes
CREATE INDEX organisationunit_patharray_gin ON organisationunit USING GIN (patharray);

DROP INDEX IF EXISTS in_organisationunit_hierarchylevel;
CREATE INDEX in_organisationunit_hierarchylevel ON organisationunit USING btree (hierarchylevel);

DROP INDEX IF EXISTS in_organisationunit_path;
CREATE INDEX in_organisationunit_path ON organisationunit USING btree (path varchar_pattern_ops);

-- 4. BEFORE trigger to update the patharray of a moved OU
CREATE OR REPLACE FUNCTION ou_moved_patharray()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    parent_path varchar(11)[];
BEGIN
    IF NEW.parentid IS NULL THEN
        NEW.patharray := ARRAY[NEW.uid]::varchar(11)[];
    ELSE
        SELECT p.patharray
        INTO parent_path
        FROM organisationunit p
        WHERE p.organisationunitid = NEW.parentid;

        IF NOT FOUND THEN
            RAISE EXCEPTION
                'Parent % not found for unit %',
                NEW.parentid, NEW.uid;
        END IF;

        IF NEW.uid = ANY(parent_path) THEN
            RAISE EXCEPTION
                'Cycle detected: OU % is already an ancestor of parent %',
                NEW.uid, parent_path;
        END IF;

        NEW.patharray := parent_path || NEW.uid;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER organisationunit_moved_patharray
    BEFORE INSERT OR UPDATE OF parentid
    ON organisationunit
    FOR EACH ROW
EXECUTE FUNCTION ou_moved_patharray();

-- 5. AFTER trigger to update patharray of all decedents of a moved OU

CREATE OR REPLACE FUNCTION ou_moved_subtree()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    old_len int := array_length(OLD.patharray, 1);
BEGIN
    IF NEW.parentid IS DISTINCT FROM OLD.parentid THEN
        UPDATE organisationunit
        SET patharray = NEW.patharray ||
                        COALESCE(
                                patharray[(old_len + 1):array_length(patharray, 1)],
                                ARRAY[]::varchar(11)[]
                        )
        WHERE patharray @> OLD.patharray
          AND organisationunitid <> NEW.organisationunitid;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER organisationunit_moved_subtree
    AFTER UPDATE OF parentid
    ON organisationunit
    FOR EACH ROW
EXECUTE FUNCTION ou_moved_subtree();