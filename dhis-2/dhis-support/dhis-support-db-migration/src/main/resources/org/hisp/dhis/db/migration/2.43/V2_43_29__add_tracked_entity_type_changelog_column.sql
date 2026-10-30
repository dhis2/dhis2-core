ALTER TABLE trackedentitytype
    ADD COLUMN IF NOT EXISTS enablechangelog BOOLEAN;

UPDATE trackedentitytype
SET enablechangelog = COALESCE(allowauditlog, FALSE)
WHERE enablechangelog IS NULL;

ALTER TABLE trackedentitytype
    ALTER COLUMN enablechangelog SET NOT NULL;