ALTER TABLE program
    ADD COLUMN IF NOT EXISTS enablechangelog BOOLEAN;

UPDATE program
SET enablechangelog = TRUE
WHERE enablechangelog IS NULL;

ALTER TABLE program
    ALTER COLUMN enablechangelog SET NOT NULL;