-- Backfill any existing rows with missing audit timestamps
UPDATE map SET lastupdated = now() WHERE lastupdated IS NULL;
UPDATE map SET created = now() WHERE created IS NULL;

-- Enforce the invariant the application already assumes (BaseIdentifiableObject's
-- Hibernate mapping declares both not-null="true"), and default to now() for any
-- writer that omits these columns, mirroring setAutoFields() semantics.
ALTER TABLE map
    ALTER COLUMN created SET DEFAULT now(),
    ALTER COLUMN lastupdated SET DEFAULT now(),
    ALTER COLUMN created SET NOT NULL,
    ALTER COLUMN lastupdated SET NOT NULL;
