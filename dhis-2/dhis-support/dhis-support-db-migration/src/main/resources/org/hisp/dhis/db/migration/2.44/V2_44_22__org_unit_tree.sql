-- function to remove accents from text
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- required to allow usage in index expressions
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
    RETURNS text AS $$
SELECT unaccent($1);
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;

-- index unaccented OU name for LIKE search
CREATE INDEX IF NOT EXISTS idx_ou_name_trgm ON organisationunit
    USING gin (immutable_unaccent(name) gin_trgm_ops);

-- index unaccented OU name for exact match search
CREATE INDEX IF NOT EXISTS idx_ou_name_unaccent ON organisationunit
    (immutable_unaccent(name));

-- Create the translation table + backfill; heal partial state on re-run
DO $$
    DECLARE
        table_existed  boolean := false;
        expected_count bigint;
        actual_count   bigint;
    BEGIN
        SELECT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name   = 'organisationunit_translation'
        ) INTO table_existed;

        IF NOT table_existed THEN
            CREATE TABLE organisationunit_translation (
              organisationunitid integer NOT NULL
                  REFERENCES organisationunit(organisationunitid) ON DELETE CASCADE,
              locale         text NOT NULL,
              property       text NOT NULL,
              value_unaccent text NOT NULL,
              PRIMARY KEY (organisationunitid, locale, property)
            );

            -- Fresh backfill; table is empty, plain INSERT is enough
            INSERT INTO organisationunit_translation
            (organisationunitid, locale, property, value_unaccent)
            SELECT ou.organisationunitid,
                   trans->>'locale',
                   trans->>'property',
                   immutable_unaccent(trans->>'value')
            FROM organisationunit ou
                     CROSS JOIN LATERAL jsonb_array_elements(ou.translations) AS trans
            WHERE trans->>'property' IN ('NAME', 'SHORT_NAME');
        ELSE
            -- Heal: recompute expected vs actual, repair only when they diverge
            SELECT count(*) INTO expected_count
            FROM organisationunit ou
                     CROSS JOIN LATERAL jsonb_array_elements(ou.translations) AS trans
            WHERE trans->>'property' IN ('NAME', 'SHORT_NAME');

            SELECT count(*) INTO actual_count FROM organisationunit_translation;

            IF actual_count <> expected_count THEN
                -- 1) Remove orphans (rows whose source translation no longer exists).
                --    Normally impossible thanks to the trigger; only reachable if a
                --    previous run failed before the trigger was installed.
                DELETE FROM organisationunit_translation t
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM organisationunit ou
                             CROSS JOIN LATERAL jsonb_array_elements(ou.translations) AS trans
                    WHERE ou.organisationunitid = t.organisationunitid
                      AND trans->>'locale'      = t.locale
                      AND trans->>'property'    = t.property
                      AND trans->>'property' IN ('NAME', 'SHORT_NAME')
                );

                -- 2) Upsert missing / stale rows
                INSERT INTO organisationunit_translation
                (organisationunitid, locale, property, value_unaccent)
                SELECT ou.organisationunitid,
                       trans->>'locale',
                       trans->>'property',
                       immutable_unaccent(trans->>'value')
                FROM organisationunit ou
                         CROSS JOIN LATERAL jsonb_array_elements(ou.translations) AS trans
                WHERE trans->>'property' IN ('NAME', 'SHORT_NAME')
                ON CONFLICT (organisationunitid, locale, property) DO UPDATE
                    SET value_unaccent = EXCLUDED.value_unaccent;
            END IF;
        END IF;
    END $$;

-- Function to keep translation table in sync with organisationunit
CREATE OR REPLACE FUNCTION sync_ou_translations() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM organisationunit_translation
        WHERE organisationunitid = OLD.organisationunitid;
        RETURN OLD;
    ELSE
        DELETE FROM organisationunit_translation
        WHERE organisationunitid = NEW.organisationunitid;
        INSERT INTO organisationunit_translation
        (organisationunitid, locale, property, value_unaccent)
        SELECT NEW.organisationunitid,
               trans->>'locale',
               trans->>'property',
               immutable_unaccent(trans->>'value')
        FROM jsonb_array_elements(NEW.translations) AS trans
        WHERE trans->>'property' IN ('NAME', 'SHORT_NAME');
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- CREATE TRIGGER has no IF NOT EXISTS
DROP TRIGGER IF EXISTS trg_sync_translations ON organisationunit;
CREATE TRIGGER trg_sync_translations
    AFTER INSERT OR UPDATE OR DELETE ON organisationunit
    FOR EACH ROW EXECUTE FUNCTION sync_ou_translations();

-- Trigram search on the accented-stripped value
CREATE INDEX IF NOT EXISTS idx_trans_value_trgm ON organisationunit_translation
    USING gin (value_unaccent gin_trgm_ops);

-- B-tree supporting the locale + property (+ value) equality filter alongside the trigram
CREATE INDEX IF NOT EXISTS idx_trans_value_unaccent
    ON organisationunit_translation (locale, property, value_unaccent);