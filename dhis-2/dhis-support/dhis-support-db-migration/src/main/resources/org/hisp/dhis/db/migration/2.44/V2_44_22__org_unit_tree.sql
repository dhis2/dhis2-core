-- function to remove accents from text
CREATE EXTENSION IF NOT EXISTS unaccent;

-- required to allow usage in index expressions
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
    RETURNS text AS $$
SELECT unaccent($1);
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;

-- index the unaccented name of an OU
CREATE INDEX idx_ou_name_trgm ON organisationunit
    USING gin (immutable_unaccent(name) gin_trgm_ops);


CREATE TABLE organisationunit_translation (
    organisationunitid integer NOT NULL REFERENCES organisationunit(organisationunitid) ON DELETE CASCADE,
    locale text NOT NULL,
    property text NOT NULL,
    value_unaccent text NOT NULL,   -- pre‑unaccented value
    PRIMARY KEY (organisationunitid, locale, property)
);

INSERT INTO organisationunit_translation (organisationunitid, locale, property, value_unaccent)
SELECT
    ou.organisationunitid,
    trans->>'locale',
    trans->>'property',
    immutable_unaccent(trans->>'value')
FROM organisationunit ou
         CROSS JOIN LATERAL jsonb_array_elements(ou.translations) AS trans
WHERE trans->>'property' IN ('NAME', 'SHORT_NAME')   -- include both if needed
ON CONFLICT (organisationunitid, locale, property) DO UPDATE
    SET value_unaccent = EXCLUDED.value_unaccent;

-- Function to update translation table on insert/update/delete
CREATE OR REPLACE FUNCTION sync_ou_translations() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM organisationunit_translation WHERE organisationunitid = OLD.organisationunitid;
        RETURN OLD;
    ELSE
        -- For INSERT or UPDATE, replace all translations for this row
        DELETE FROM organisationunit_translation WHERE organisationunitid = NEW.organisationunitid;
        INSERT INTO organisationunit_translation (organisationunitid, locale, property, value_unaccent)
        SELECT NEW.organisationunitid, trans->>'locale', trans->>'property', immutable_unaccent(trans->>'value')
        FROM jsonb_array_elements(NEW.translations) AS trans
        WHERE trans->>'property' IN ('NAME', 'SHORT_NAME');
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_translations
    AFTER INSERT OR UPDATE OR DELETE ON organisationunit
    FOR EACH ROW EXECUTE FUNCTION sync_ou_translations();


-- For exact locale + property + trigram search on value
CREATE INDEX idx_trans_search ON organisationunit_translation
    USING gin (locale, property, value_unaccent gin_trgm_ops);

-- For fast existence check (locale + property)
CREATE UNIQUE INDEX idx_trans_lookup ON organisationunit_translation (organisationunitid, locale, property);